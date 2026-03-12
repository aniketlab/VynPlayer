package com.vyn.player.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import com.vyn.player.util.MetadataSanitizer
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.withLock
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ArtworkRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userPrefs: com.vyn.player.data.preferences.UserPreferencesManager,
    private val songDao: com.vyn.player.data.local.dao.SongDao,
    private val albumArtworkDao: com.vyn.player.data.local.dao.AlbumArtworkDao
) {
    companion object {
        private const val TAG = "MuzicArtwork"
        private const val ITUNES_BASE = "https://itunes.apple.com/search"
        private const val MB_BASE = "https://musicbrainz.org/ws/2/release/"
        private const val LASTFM_BASE = "https://ws.audioscrobbler.com/2.0/"
        private const val LASTFM_API_KEY = "b25b959554ed76058ac220b7b2e0a026"
        private const val USER_AGENT = "MuzicPlayer/2.4.0 ( sharm@example.com )"
        private const val MAX_IMAGE_SIZE = 512

        private const val BATCH_SIZE = 3
        private const val MAX_CONCURRENT = 1 // Strictly 1 request at a time
        private const val TASK_DELAY_MS = 300L // 300ms gap between tasks for gradual processing
        private const val MAX_SESSION_DOWNLOADS = 200
        private const val RETRY_COOL_DOWN_MS = 24 * 60 * 60 * 1000L // 24 hours
        private const val MAX_RETRIES = 1 // Only 1 attempt per session, no retries

        fun getAlbumCacheKey(album: String, artist: String): String {
            val input = "iTunes_${album.lowercase().trim()}_${artist.lowercase().trim()}"
            return MessageDigest.getInstance("MD5")
                .digest(input.toByteArray())
                .joinToString("") { "%02x".format(it) }
        }

        private fun getSimilarity(s1: String, s2: String): Float {
            val c1 = MetadataSanitizer.sanitize(s1).lowercase().replace(Regex("[^a-z0-9]"), "")
            val c2 = MetadataSanitizer.sanitize(s2).lowercase().replace(Regex("[^a-z0-9]"), "")
            if (c1.isEmpty() || c2.isEmpty()) return 0f
            if (c1 == c2) return 1f
            if (c1.contains(c2) || c2.contains(c1)) {
                val diff = kotlin.math.abs(c1.length - c2.length)
                val maxL = kotlin.math.max(c1.length, c2.length)
                return 1.0f - (diff.toFloat() / maxL)
            }
            return 0f
        }
    }

    // Single shared OkHttpClient — no custom DNS, no interceptors
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    private val cacheDir: File by lazy {
        File(context.cacheDir, "artwork_cache").also {
            it.mkdirs()
            Log.d(TAG, "Cache dir: ${it.absolutePath}")
        }
    }

    // --- State ---
    private val _cacheVersion = MutableStateFlow(0L)
    val cacheVersion: StateFlow<Long> = _cacheVersion.asStateFlow()
    
    // For narrow UI updates without full list recomposition
    private val _artworkRefreshFlow = MutableSharedFlow<String>(extraBufferCapacity = 64)
    val artworkRefreshFlow = _artworkRefreshFlow.asSharedFlow()
    
    private var lastCacheUpdate = 0L
    private var cacheDebounceJob: kotlinx.coroutines.Job? = null
    
    private fun notifyCacheUpdated(albumKey: String? = null) {
        val now = System.currentTimeMillis()
        if (albumKey != null) {
            prefetchScope.launch { _artworkRefreshFlow.emit(albumKey) }
        }
        
        if (now - lastCacheUpdate > 2000L) {
            _cacheVersion.value++
            lastCacheUpdate = now
        } else {
            cacheDebounceJob?.cancel()
            cacheDebounceJob = prefetchScope.launch {
                delay(2000L)
                _cacheVersion.value++
                lastCacheUpdate = System.currentTimeMillis()
            }
        }
    }
    private val _isScrolling = MutableStateFlow(false)
    
    fun setScrolling(scrolling: Boolean) {
        _isScrolling.value = scrolling
    }
    
    fun isScrolling(): Boolean = _isScrolling.value

    private val prefetchMutex = kotlinx.coroutines.sync.Mutex()
    private val prefetchQueue = ConcurrentLinkedQueue<PrefetchItem>()
    private val isPrefetching = AtomicBoolean(false)
    private val prefetchScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val failedAttempts = java.util.concurrent.ConcurrentHashMap<String, Pair<Int, Long>>()
    private val queuedKeys = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()
    // Tracks ALL album keys that have been attempted this session (success or fail)
    // Prevents infinite re-queuing during scroll
    private val processedAlbumKeys = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()

    data class PrefetchItem(val songId: Long, val title: String, val artist: String, val album: String, val preResolvedUrl: String? = null)

    // Custom LRU cache holding max 1000 disk files natively
    private val diskLruCache = object : java.util.LinkedHashMap<String, File>(1000, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, File>?): Boolean {
            if (size > 1000 && eldest != null) {
                try {
                    if (eldest.value.exists()) eldest.value.delete()
                    Log.d(TAG, "Evicted old artwork from cache: ${eldest.value.name}")
                } catch (e: Exception) {}
                return true
            }
            return false
        }
    }

    init {
        prefetchScope.launch {
            try {
                // Populate index from existing cache files, sorted by mod time
                val files = cacheDir.listFiles()?.sortedBy { it.lastModified() } ?: emptyList()
                Log.d(TAG, "Initializing Smart Cache Index with ${files.size} items")
                for (file in files) {
                    diskLruCache[file.nameWithoutExtension] = file
                }
                Log.d(TAG, "Smart Cache Index initialized, pruned to ${diskLruCache.size} items")
            } catch (e: Exception) {}
        }
    }

    // ═══════════════════════════════════
    // PUBLIC API
    // ═══════════════════════════════════

    suspend fun getCachedAlbumArtwork(albumName: String, artistName: String, thumbnailMode: Boolean = false): File? = withContext(Dispatchers.IO) {
        if (albumName.isBlank() || albumName.equals("unknown", ignoreCase = true)) return@withContext null
        val key = getAlbumCacheKey(albumName, artistName)
        
        if (thumbnailMode) {
            val thumbFile = getCachedFile("${key}_thumb")
            if (thumbFile != null) return@withContext thumbFile
        }

        val file = getCachedFile(key)
        if (file != null) return@withContext file
        
        // Secondary check: look in shared DB table
        try {
            val sharedKey = "${artistName}_${albumName}".lowercase()
            val sharedArt = albumArtworkDao.getArtwork(sharedKey)
            if (sharedArt != null) {
                val f = File(sharedArt.artworkPath)
                if (f.exists()) return@withContext f
            }
        } catch (e: Exception) {}
        
        null
    }

    fun startPrefetch(songs: List<com.vyn.player.data.model.Song>) {
        Log.d(TAG, "════════════════════════════════════════")
        Log.d(TAG, "startPrefetch() called with ${songs.size} songs")

        prefetchScope.launch {
            prefetchMutex.withLock {
                try {
                    // Check user setting ONLY
                    val mode = runCatching { userPrefs.artworkDownloadMode.first() }.getOrDefault(2)
                    Log.d(TAG, "Artwork mode: $mode (0=Off 1=WiFi 2=All)")
                    if (mode == 0) {
                        Log.d(TAG, "Artwork downloads disabled by user")
                        return@withLock
                    }

                    val itemsToQueue = mutableListOf<PrefetchItem>()
                    var skippedQueued = 0
                    var skippedCached = 0
                    var skippedFailed = 0
                    var skippedUnknown = 0
                    val albumsMap = songs
                        .filter {
                            if (it.album.isBlank() || it.album.equals("unknown", ignoreCase = true)) {
                                skippedUnknown++
                                false
                            } else true
                        }
                        .groupBy { getAlbumCacheKey(it.album, it.artist) }

                    for ((key, albumSongs) in albumsMap) {
                        if (key in queuedKeys) { skippedQueued++; continue }
                        if (key in processedAlbumKeys) { skippedQueued++; continue } // Already attempted this session
                        if (getCachedFile(key) != null) { skippedCached++; continue }
                        val f = failedAttempts[key]
                        if (f != null && (f.first >= MAX_RETRIES || System.currentTimeMillis() < f.second)) { skippedFailed++; continue }

                        queuedKeys.add(key)
                        val sampleSong = albumSongs.first()
                        itemsToQueue.add(PrefetchItem(sampleSong.id, sampleSong.title, sampleSong.artist, sampleSong.album, sampleSong.artworkUrl))
                        Log.d(TAG, "Grouped album for download: ${sampleSong.album} (from ${albumSongs.size} songs)")
                    }

                    Log.d(TAG, "Queue: ${itemsToQueue.size} new unique albums (after grouping)")
                    Log.d(TAG, "  Skip: unknown=$skippedUnknown queued=$skippedQueued cached=$skippedCached failed=$skippedFailed")

                    if (itemsToQueue.isNotEmpty()) {
                        itemsToQueue.take(3).forEachIndexed { i, it ->
                            Log.d(TAG, "  [$i] '${it.album}' by '${it.artist}'")
                        }
                        prefetchQueue.addAll(itemsToQueue)
                    }

                } catch (e: Exception) {
                    Log.e(TAG, "startPrefetch EXCEPTION: ${e.javaClass.simpleName}: ${e.message}", e)
                }
            } // end mutex

            // Always attempt to start the worker if queue isn't empty, even if we didn't add anything new this time
            if (prefetchQueue.isNotEmpty() && !isPrefetching.get()) {
                startPrefetchWorker()
            }
        }
    }

    fun forceArtworkScan(songs: List<com.vyn.player.data.model.Song>) {
        Log.d(TAG, "╔═══ FORCE ARTWORK SCAN ═══╗")
        queuedKeys.clear()
        failedAttempts.clear()
        prefetchQueue.clear()
        isPrefetching.set(false)
        startPrefetch(songs)
    }

    fun runConnectivityTest() {
        prefetchScope.launch {
            Log.d(TAG, "─── CONNECTIVITY TEST ───")
            try {
                val request = Request.Builder()
                    .url("https://itunes.apple.com/search?term=test&limit=1")
                    .header("User-Agent", USER_AGENT)
                    .build()
                Log.d(TAG, "Sending OkHttp request...")
                val response = client.newCall(request).execute()
                val body = response.body?.string() ?: ""
                Log.d(TAG, "HTTP ${response.code}, body=${body.length} chars")
                Log.d(TAG, "Body preview: ${body.take(200)}")
            } catch (e: Exception) {
                Log.e(TAG, "TEST FAILED: ${e.javaClass.simpleName}: ${e.message}", e)
            }
        }
    }

    // ═══════════════════════════════════
    // WORKER
    // ═══════════════════════════════════

    private fun startPrefetchWorker() {
        if (isPrefetching.getAndSet(true)) {
            Log.d(TAG, "Worker already running")
            return
        }

        prefetchScope.launch {
            Log.d(TAG, "╔═══ ENRICHMENT WORKER STARTED ═══")
            Log.d(TAG, "Artwork queue size: ${prefetchQueue.size}")

            try {
                while (prefetchQueue.isNotEmpty()) {
                    val item = prefetchQueue.poll() ?: break

                    Log.d(TAG, "║ Processing: ${item.album} by ${item.artist}")
                    processItem(item)

                    // 300ms delay between tasks — gradual, never bursts
                    delay(TASK_DELAY_MS)
                }
            } catch (e: Exception) {
                Log.e(TAG, "WORKER EXCEPTION: ${e.javaClass.simpleName}: ${e.message}", e)
            }

            isPrefetching.set(false)
            Log.d(TAG, "╚═══ ENRICHMENT WORKER IDLE ═══ remaining=${prefetchQueue.size}")

            if (prefetchQueue.isNotEmpty() && !isPrefetching.get()) {
                startPrefetchWorker()
            }
        }
    }

    private suspend fun processItem(item: PrefetchItem) {
        val key = getAlbumCacheKey(item.album, item.artist)
        val f = failedAttempts[key]
        if (f != null && (f.first >= MAX_RETRIES || System.currentTimeMillis() < f.second)) return
        if (getCachedFile(key) != null) return
        
        // ─── Shared Table Lookup ───
        // Before network, check if another song from this album already fixed it
        val sharedKey = "${item.artist}_${item.album}".lowercase()
        val sharedArt = albumArtworkDao.getArtwork(sharedKey)
        if (sharedArt != null && File(sharedArt.artworkPath).exists()) {
            Log.d(TAG, "Reusing artwork from shared table for: ${item.album}")
            notifyCacheUpdated(key)
            processedAlbumKeys.add(key) // Mark done
            return
        }

        Log.d(TAG, "Searching artwork for: ${item.title} - ${item.artist}")

        try {
            val url = item.preResolvedUrl ?: fetchArtworkUrl(item)
            if (url != null) {
                Log.d(TAG, "Artwork download started for: ${item.title}")
                val bytes = downloadImage(url)
                if (bytes != null && bytes.isNotEmpty()) {
                    val file = saveToDisk(key, bytes)
                    notifyCacheUpdated(key)
                    failedAttempts.remove(key)
                    processedAlbumKeys.add(key) // Mark done — won't retry
                    Log.d(TAG, "Image downloaded and saved (${bytes.size / 1024}KB)")
                    
                    if (file != null) {
                        try {
                            val sharedKey = "${item.artist}_${item.album}".lowercase()
                            albumArtworkDao.insertArtwork(com.vyn.player.data.local.entity.AlbumArtworkEntity(
                                albumKey = sharedKey,
                                albumName = item.album,
                                artistName = item.artist,
                                artworkPath = file.absolutePath
                            ))
                        } catch (e: Exception) {
                            Log.e(TAG, "DB Path update error", e)
                        }
                    }
                    return
                } else {
                    Log.w(TAG, "Download empty for: ${item.title}")
                    processedAlbumKeys.add(key) // Mark empty downloads as processed too
                }
            } else {
                Log.d(TAG, "No artwork found for: ${item.title} - ${item.artist}")
                processedAlbumKeys.add(key) // No result from any API \u2014 don't retry
            }
        } catch (e: Exception) {
            Log.e(TAG, "processItem error: ${e.javaClass.simpleName}: ${e.message}")
        }

        val retries = (f?.first ?: 0) + 1
        failedAttempts[key] = retries to (System.currentTimeMillis() + RETRY_COOL_DOWN_MS)
        processedAlbumKeys.add(key) // Mark as attempted — won't be re-queued
        Log.d(TAG, "Failed and marked processed: $retries/$MAX_RETRIES")
    }

    // ═══════════════════════════════════
    // iTUNES — direct OkHttp, no pre-checks
    // ═══════════════════════════════════

    private suspend fun fetchArtworkUrl(item: PrefetchItem): String? = withContext(Dispatchers.IO) {
        val sTitle = MetadataSanitizer.sanitize(item.title)
        val sArtist = MetadataSanitizer.sanitize(item.artist)
        val sAlbum = MetadataSanitizer.sanitize(item.album)

        val isArtistUnknown = sArtist.isBlank() || sArtist.equals("unknown", true)

        val searchQuery = buildString {
            append(sTitle)
            if (!isArtistUnknown) append(" $sArtist")
            if (sAlbum.isNotBlank() && !sAlbum.equals("unknown", true)) append(" $sAlbum")
        }.trim()

        var url: String? = null

        // Priority 1: iTunes API
        if (searchQuery.isNotBlank()) {
            url = queryiTunes(searchQuery, sAlbum)
        }

        // Priority 2: Last.fm API
        if (url == null && !isArtistUnknown && sAlbum.isNotBlank()) {
            url = queryLastFm(sArtist, sAlbum)
        }

        // Priority 3: MusicBrainz CoverArtArchive
        if (url == null && sAlbum.isNotBlank()) {
            url = queryMusicBrainz(sArtist, sAlbum)
        }

        url
    }

    private fun queryiTunes(term: String, expectedAlbum: String): String? {
        if (term.isBlank() || term.length < 2) return null
        val encoded = java.net.URLEncoder.encode(term.trim(), "UTF-8")
        val url = "$ITUNES_BASE?term=$encoded&entity=song&limit=5"

        return try {
            Log.d(TAG, "iTunes request for term: '$term'")
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", USER_AGENT)
                .build()
            val response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                Log.w(TAG, "iTunes HTTP ${response.code}")
                return null
            }

            val body = response.body?.string() ?: return null
            val json = JSONObject(body)
            val results = json.optJSONArray("results")
            Log.d(TAG, "iTunes response: ${json.optInt("resultCount")} results for term: '$term'")

            if (results != null && results.length() > 0) {
                // Try strict match with expected album
                for (i in 0 until results.length()) {
                    val result = results.getJSONObject(i)
                    val collectionName = result.optString("collectionName", "")
                    if (getSimilarity(collectionName, expectedAlbum) > 0.65f) {
                        val artUrl = result.optString("artworkUrl100", "")
                        if (artUrl.isNotBlank()) {
                            Log.d(TAG, "Selected API result (Strict Match): $collectionName")
                            return artUrl.replace("100x100bb", "600x600bb")
                        }
                    }
                }
                
                // Fallback to the first result that has artwork
                for (i in 0 until results.length()) {
                    val result = results.getJSONObject(i)
                    val artUrl = result.optString("artworkUrl100", "")
                    if (artUrl.isNotBlank()) {
                        val collectionName = result.optString("collectionName", "")
                        Log.d(TAG, "Selected API result (Fallback Match): $collectionName")
                        return artUrl.replace("100x100bb", "600x600bb")
                    }
                }
            }
            null
        } catch (e: Exception) {
            Log.e(TAG, "iTunes error: ${e.javaClass.simpleName}: ${e.message}")
            null
        }
    }

    private fun queryLastFm(artist: String, album: String): String? {
        try {
            val aEnc = java.net.URLEncoder.encode(artist, "UTF-8")
            val alEnc = java.net.URLEncoder.encode(album, "UTF-8")
            val url = "$LASTFM_BASE?method=album.getinfo&api_key=$LASTFM_API_KEY&artist=$aEnc&album=$alEnc&format=json"

            Log.d(TAG, "Last.fm fallback request for: '$artist - $album'")
            val request = Request.Builder().url(url).header("User-Agent", USER_AGENT).build()
            val response = client.newCall(request).execute()

            if (!response.isSuccessful) return null
            val body = response.body?.string() ?: return null
            val json = JSONObject(body)
            val albumObj = json.optJSONObject("album") ?: return null

            val images = albumObj.optJSONArray("image")
            if (images != null && images.length() > 0) {
                // Get the largest image provided
                val art = images.getJSONObject(images.length() - 1).optString("#text", "")
                if (art.isNotBlank() && art.startsWith("http")) return art
            }
        } catch (e: Exception) { Log.e(TAG, "LastFm EXCEPTION: ${e.message}") }
        return null
    }

    private fun queryMusicBrainz(artist: String, album: String): String? {
        try {
            val query = if (artist.isNotBlank() && !artist.lowercase().contains("unknown")) {
                "release:\"$album\" AND artist:\"$artist\""
            } else {
                "release:\"$album\""
            }
            val encoded = java.net.URLEncoder.encode(query, "UTF-8")
            val url = "$MB_BASE?query=$encoded&fmt=json"

            Log.d(TAG, "MusicBrainz fallback request for: '$query'")
            val request = Request.Builder().url(url).header("User-Agent", USER_AGENT).build()
            val response = client.newCall(request).execute()

            if (!response.isSuccessful) return null
            val body = response.body?.string() ?: return null
            val json = JSONObject(body)
            val releases = json.optJSONArray("releases") ?: return null

            for (i in 0 until releases.length()) {
                val rel = releases.getJSONObject(i)
                val mbid = rel.optString("id", "")
                val title = rel.optString("title", "")
                
                if (mbid.isNotBlank() && getSimilarity(title, album) > 0.65f) {
                    Log.d(TAG, "MusicBrainz CoverArtArchive match for MBID: $mbid")
                    return "https://coverartarchive.org/release/$mbid/front"
                }
            }
        } catch (e: Exception) { Log.e(TAG, "MusicBrainz EXCEPTION: ${e.message}") }
        return null
    }

    private fun downloadImage(url: String): ByteArray? {
        return try {
            val request = Request.Builder().url(url).header("User-Agent", USER_AGENT).build()
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val bytes = response.body?.bytes()
                Log.d(TAG, "Downloaded ${bytes?.size ?: 0} bytes")
                bytes
            } else {
                Log.w(TAG, "Image HTTP ${response.code}")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Image error: ${e.message}")
            null
        }
    }

    // ═══════════════════════════════════
    // DISK CACHE
    // ═══════════════════════════════════

    private fun saveToDisk(key: String, bytes: ByteArray): File? {
        val file = File(cacheDir, "$key.jpg")
        val thumbFile = File(cacheDir, "${key}_thumb.jpg")
        try {
            val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
            val scale = calculateInSampleSize(opts.outWidth, opts.outHeight, MAX_IMAGE_SIZE)
            
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size,
                BitmapFactory.Options().apply { inSampleSize = scale })
                
            if (bitmap != null) {
                // Save Main (512 max)
                FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.JPEG, 85, it) }
                
                // Save Thumb (256 max)
                try {
                    val thumbW = (bitmap.width * 0.5f).toInt().coerceAtLeast(1)
                    val thumbH = (bitmap.height * 0.5f).toInt().coerceAtLeast(1)
                    val thumbBmp = Bitmap.createScaledBitmap(bitmap, thumbW, thumbH, true)
                    FileOutputStream(thumbFile).use { thumbBmp.compress(Bitmap.CompressFormat.JPEG, 70, it) }
                    thumbBmp.recycle()
                    diskLruCache["${key}_thumb"] = thumbFile
                } catch (e: Exception) { Log.e(TAG, "Thumb generation error", e) }
                
                bitmap.recycle()
            } else {
                FileOutputStream(file).use { it.write(bytes) }
            }
            Log.d(TAG, "Saved: ${file.name} (${file.length() / 1024}KB)")
            diskLruCache[key] = file
            return file
        } catch (e: Exception) {
            try { 
                FileOutputStream(file).use { it.write(bytes) } 
                diskLruCache[key] = file
                return file
            } catch (_: Exception) {}
        }
        return null
    }

    private fun calculateInSampleSize(w: Int, h: Int, max: Int): Int {
        var s = 1
        if (h > max || w > max) { while (h / (s * 2) >= max && w / (s * 2) >= max) s *= 2 }
        return s
    }



    private fun getCachedFile(key: String): File? {
        val cachedFile = diskLruCache[key] ?: File(cacheDir, "$key.jpg")
        if (cachedFile.exists()) {
            diskLruCache[key] = cachedFile
            return cachedFile
        }
        return null
    }

    suspend fun clearCache() = withContext(Dispatchers.IO) {
        cacheDir.listFiles()?.forEach { it.delete() }
        failedAttempts.clear()
        queuedKeys.clear()
        isPrefetching.set(false)
        _cacheVersion.value++
        Log.d(TAG, "Cache cleared")
    }
}
