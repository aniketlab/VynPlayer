package com.vyn.player.data.repository

import android.util.Log
import com.vyn.player.data.model.Song
import com.vyn.player.util.MetadataSanitizer
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlin.math.max

data class MetadataResult(
    val artist: String,
    val album: String,
    val artworkUrl: String? = null,
    val artistImageUrl: String? = null
)

@Singleton
class MetadataCorrectionRepository @Inject constructor(
    private val musicRepository: MusicRepository,
    private val artworkRepository: ArtworkRepository
) {
    companion object {
        private const val TAG = "MuzicMetadata"
        private const val ITUNES_BASE = "https://itunes.apple.com/search"
        private const val MB_BASE = "https://musicbrainz.org/ws/2/recording/"
        private const val LASTFM_BASE = "https://ws.audioscrobbler.com/2.0/"
        
        // Generic fallback api key for audioscrobbler
        private const val LASTFM_API_KEY = "b25b959554ed76058ac220b7b2e0a026"
        private const val USER_AGENT = "MuzicPlayer/2.4.0 ( sharm@example.com )"
        
        private const val BATCH_SIZE = 5
        private const val MAX_CONCURRENT = 1  // Only 1 API call at a time to prevent network spikes
        private const val DELAY_MS = 3000L    // 3 second delay between batches
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    private val correctionMutex = Mutex()
    private val queue = ConcurrentLinkedQueue<Song>()
    private val isWorking = AtomicBoolean(false)
    private val workerScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val attemptedIds = java.util.concurrent.ConcurrentHashMap.newKeySet<Long>()

    fun startCorrection(songs: List<Song>) {
        workerScope.launch {
            correctionMutex.withLock {
                val toFix = songs.filter {
                    val aLower = it.artist.trim().lowercase()
                    val isUnknownArtist = aLower.isEmpty() || 
                                          aLower.contains("unknown") || 
                                          aLower.contains("<unknown>") || 
                                          aLower.contains("unknown artist") || 
                                          aLower.length < 2
                                          
                    val isUnknownAlbum = it.album.isBlank() || it.album.equals("unknown", true) || it.album.equals("<unknown>", true)
                    
                    if (it.id !in attemptedIds && (isUnknownArtist || isUnknownAlbum)) {
                        attemptedIds.add(it.id)
                        true
                    } else {
                        false
                    }
                }

                if (toFix.isNotEmpty()) {
                    Log.d(TAG, "Found ${toFix.size} songs needing metadata repair")
                    queue.addAll(toFix)
                }
            }

            if (queue.isNotEmpty()) {
                startWorker()
            }
        }
    }

    private fun startWorker() {
        if (isWorking.getAndSet(true)) return

        workerScope.launch {
            Log.d(TAG, "Starting metadata enrichment worker. Queue size: ${queue.size}")
            
            // Suppress Room Flow re-emissions during batch metadata updates
            // This prevents full list recomposition for EVERY single song fix
            musicRepository.setSuppressFlowUpdates(true)
            
            try {
                while (queue.isNotEmpty()) {
                    val song = queue.poll() ?: break
                    processSong(song)
                    delay(DELAY_MS) // 3s delay between API calls — gradual processing
                }
            } catch (e: Exception) {
                Log.e(TAG, "Worker exception: ${e.message}", e)
            } finally {
                // Re-enable flow updates so the UI gets ONE final refresh with all changes
                musicRepository.setSuppressFlowUpdates(false)
                isWorking.set(false)
                Log.d(TAG, "Worker idle. Remaining: ${queue.size}")
            }
        }
    }

    private suspend fun processSong(song: Song) {
        val sTitle = MetadataSanitizer.sanitize(song.title).trim()
        if (sTitle.isBlank()) return

        Log.d(TAG, "Searching multi-API metadata for: '${song.title}'")

        val result = fetchMetadataAll(sTitle, song.artist)
        if (result != null) {
            Log.d(TAG, "Metadata match accepted: [${song.title}] -> Artist: ${result.artist}, Album: ${result.album}")
            
            // Delete memory cache
            com.vyn.player.ui.components.ArtworkModelCache.cache.remove(song.path)

            // Update metadata in DB
            musicRepository.updateSongMetadata(
                song.id, 
                result.artist, 
                result.album, 
                result.artworkUrl,
                result.artistImageUrl
            )
            
            // Trigger Artwork prefetch
            val fixedSong = song.copy(artist = result.artist, album = result.album, artworkUrl = result.artworkUrl, artistImageUrl = result.artistImageUrl)
            artworkRepository.startPrefetch(listOf(fixedSong))
        } else {
            Log.d(TAG, "Metadata match rejected/not found for: '${song.title}'")
        }
    }

    private fun fetchMetadataAll(title: String, originalArtist: String?): MetadataResult? {
        val expectedClean = title.lowercase().replace(Regex("[^a-z0-9]"), "")
        if (expectedClean.isEmpty()) return null

        val artistTerm = if (!originalArtist.isNullOrBlank() && originalArtist.length > 2 && !originalArtist.lowercase().contains("unknown")) {
            originalArtist.trim()
        } else {
            ""
        }

        // 1. MusicBrainz API
        var result = tryMusicBrainz(title, artistTerm, expectedClean)
        if (result != null) Log.d(TAG, "Primary API success (MusicBrainz)")
        
        // 2. iTunes API
        if (result == null) {
            result = tryItunes(title, artistTerm, expectedClean)
            if (result != null) Log.d(TAG, "Fallback API success (iTunes)")
        }

        // 3. Last.fm API
        if (result == null) {
            result = tryLastFm(title, expectedClean)
            if (result != null) Log.d(TAG, "Fallback API success (Last.fm)")
        }

        // 4. Enrich with Last.fm Artist Image if needed
        if (result != null && result.artistImageUrl == null) {
            val image = fetchLastFmArtistImage(result.artist)
            if (image != null) {
                result = result.copy(artistImageUrl = image)
            }
        }

        return result
    }

    private fun isSimilar(matchedTitle: String, expectedClean: String): Boolean {
        val trackClean = matchedTitle.lowercase().replace(Regex("[^a-z0-9]"), "")
        if (trackClean.isEmpty() || expectedClean.isEmpty()) return false
        
        if (trackClean == expectedClean) return true
        
        if (trackClean.contains(expectedClean) || expectedClean.contains(trackClean)) {
            val diff = abs(trackClean.length - expectedClean.length)
            val maxL = max(trackClean.length, expectedClean.length)
            val similarity = 1.0f - (diff.toFloat() / maxL)
            return similarity >= 0.75f
        }
        return false
    }

    private fun tryMusicBrainz(title: String, artist: String, expectedClean: String): MetadataResult? {
        try {
            val query = if (artist.isNotBlank()) {
                "recording:\"$title\" AND artist:\"$artist\""
            } else {
                "recording:\"$title\""
            }
            
            val encodedId = java.net.URLEncoder.encode(query, "UTF-8")
            val url = "$MB_BASE?query=$encodedId&fmt=json"
            val req = Request.Builder().url(url).header("User-Agent", USER_AGENT).build()
            val res = client.newCall(req).execute()
            
            if (!res.isSuccessful) return null
            val json = JSONObject(res.body?.string() ?: "")
            val recs = json.optJSONArray("recordings") ?: return null
            
            for (i in 0 until recs.length()) {
                val rec = recs.getJSONObject(i)
                val trackName = rec.optString("title", "")
                
                var artistName = ""
                val artistCredit = rec.optJSONArray("artist-credit")
                if (artistCredit != null && artistCredit.length() > 0) {
                    val artistObj = artistCredit.getJSONObject(0).optJSONObject("artist")
                    if (artistObj != null) artistName = artistObj.optString("name", "")
                }
                
                var collectionName = ""
                var mbidRelease = ""
                val releases = rec.optJSONArray("releases")
                if (releases != null && releases.length() > 0) {
                    val rel = releases.getJSONObject(0)
                    collectionName = rel.optString("title", "")
                    mbidRelease = rel.optString("id", "")
                }
                
                if (artistName.isNotBlank() && collectionName.isNotBlank() && isSimilar(trackName, expectedClean)) {
                    var cover: String? = null
                    // If we have an MBID release, we can use CoverArtArchive
                    if (mbidRelease.isNotBlank()) {
                        cover = "https://coverartarchive.org/release/$mbidRelease/front"
                    }
                    return MetadataResult(artistName, collectionName, cover)
                }
            }
        } catch (e: Exception) { Log.e(TAG, "MB error: ${e.message}") }
        return null
    }

    private fun tryItunes(title: String, artist: String, expectedClean: String): MetadataResult? {
        try {
            val term = if (artist.isNotBlank()) "$title $artist" else title
            val encoded = java.net.URLEncoder.encode(term, "UTF-8")
            val url = "$ITUNES_BASE?term=$encoded&entity=song&limit=5"
            val req = Request.Builder().url(url).header("User-Agent", USER_AGENT).build()
            val res = client.newCall(req).execute()
            
            if (!res.isSuccessful) return null
            val json = JSONObject(res.body?.string() ?: "")
            val results = json.optJSONArray("results") ?: return null
            
            for (i in 0 until results.length()) {
                val r = results.getJSONObject(i)
                val trackName = r.optString("trackName", "")
                val artistName = r.optString("artistName", "")
                val collectionName = r.optString("collectionName", "")
                val art = r.optString("artworkUrl100", "").replace("100x100bb", "600x600bb")
                
                if (artistName.isNotBlank() && collectionName.isNotBlank() && isSimilar(trackName, expectedClean)) {
                    return MetadataResult(artistName, collectionName, if (art.isNotBlank()) art else null)
                }
            }
        } catch (e: Exception) { Log.e(TAG, "iTunes error: ${e.message}") }
        return null
    }

    private fun tryLastFm(title: String, expectedClean: String): MetadataResult? {
        try {
            val encoded = java.net.URLEncoder.encode(title, "UTF-8")
            val url = "${LASTFM_BASE}?method=track.search&track=$encoded&api_key=$LASTFM_API_KEY&format=json"
            val req = Request.Builder().url(url).build()
            val res = client.newCall(req).execute()
            if (!res.isSuccessful) return null
            
            val json = JSONObject(res.body?.string() ?: "")
            val matches = json.optJSONObject("results")?.optJSONObject("trackmatches")?.optJSONArray("track") ?: return null
            
            for (i in 0 until matches.length()) {
                val track = matches.getJSONObject(i)
                val name = track.optString("name", "")
                val artist = track.optString("artist", "")
                
                if (artist.isNotBlank() && isSimilar(name, expectedClean)) {
                    val infoUrl = "${LASTFM_BASE}?method=track.getInfo&api_key=$LASTFM_API_KEY&artist=${java.net.URLEncoder.encode(artist, "UTF-8")}&track=${java.net.URLEncoder.encode(name, "UTF-8")}&format=json"
                    val infoRes = client.newCall(Request.Builder().url(infoUrl).build()).execute()
                    if (infoRes.isSuccessful) {
                        val infoJson = JSONObject(infoRes.body?.string() ?: "").optJSONObject("track")
                        val albumObj = infoJson?.optJSONObject("album")
                        val albumTitle = albumObj?.optString("title", "") ?: ""
                        
                        var coverArt: String? = null
                        val images = albumObj?.optJSONArray("image")
                        if (images != null && images.length() > 0) {
                            coverArt = images.getJSONObject(images.length() - 1).optString("#text", "")
                        }
                        
                        if (albumTitle.isNotBlank()) {
                            return MetadataResult(artist, albumTitle, if (coverArt.isNullOrBlank()) null else coverArt)
                        }
                    }
                }
            }
        } catch (e: Exception) { Log.e(TAG, "LastFm error: ${e.message}") }
        return null
    }

    private fun fetchLastFmArtistImage(artistName: String): String? {
        try {
            val encoded = java.net.URLEncoder.encode(artistName, "UTF-8")
            val url = "${LASTFM_BASE}?method=artist.getinfo&artist=$encoded&api_key=$LASTFM_API_KEY&format=json"
            val req = Request.Builder().url(url).build()
            val res = client.newCall(req).execute()
            if (!res.isSuccessful) return null
            
            val infoJson = JSONObject(res.body?.string() ?: "").optJSONObject("artist")
            val images = infoJson?.optJSONArray("image")
            if (images != null && images.length() > 0) {
                // Return the 'extralarge' or largest present
                val art = images.getJSONObject(images.length() - 1).optString("#text", "")
                if (art.isNotBlank()) return art
            }
        } catch(e: Exception) { Log.e(TAG, "LastFm artist image error: ${e.message}") }
        return null
    }
}
