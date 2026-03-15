package com.vyn.player.data.repository

import com.vyn.player.data.local.dao.FavoriteDao
import com.vyn.player.data.local.entity.FavoriteEntity
import com.vyn.player.data.model.Album
import com.vyn.player.data.model.Artist
import com.vyn.player.data.model.Folder
import com.vyn.player.data.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import android.net.Uri
import android.content.ContentUris
import com.vyn.player.data.local.dao.SongDao
import com.vyn.player.data.local.entity.SongEntity
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject
import javax.inject.Singleton

import com.vyn.player.data.local.dao.PlaybackHistoryDao
import com.vyn.player.data.local.dao.SongStatsDao
import com.vyn.player.data.local.entity.SongStatsEntity
import com.vyn.player.data.local.entity.PlaybackHistoryEntity
import android.util.Log
import java.util.concurrent.atomic.AtomicBoolean

private const val UNKNOWN_ARTIST_DISPLAY_NAME = "Unknown Artist"
private val INVALID_NUMERIC_ARTIST_REGEX = Regex("^\\d+$")
private val INVALID_FILENAME_ARTIST_REGEX = Regex("^\\d+(?:[\\s_].*|[a-zA-Z].*)?$", RegexOption.IGNORE_CASE)
private val FEATURE_SEPARATOR_REGEX = Regex("""\s+(?:ft\.?|feat\.?|featuring)\s+""", RegexOption.IGNORE_CASE)

@OptIn(kotlinx.coroutines.FlowPreview::class)
@Singleton
class MusicRepository @Inject constructor(
    private val mediaStoreScanner: MediaStoreScanner,
    private val favoriteDao: FavoriteDao,
    private val playbackHistoryDao: PlaybackHistoryDao,
    private val songDao: SongDao,
    private val songStatsDao: SongStatsDao,
    private val albumArtworkDao: com.vyn.player.data.local.dao.AlbumArtworkDao,
    private val musicHistoryRepository: MusicHistoryRepository,
    private val artworkRepository: ArtworkRepository,
    private val metadataCorrectionRepository: dagger.Lazy<MetadataCorrectionRepository>
) {
    // Cached data
    private var cachedSongs: List<Song>? = null
    private var cachedAlbums: List<Album>? = null
    private var cachedArtists: List<Artist>? = null
    private var cachedFolders: List<Folder>? = null
    private var metadataFixStarted = false
    private val isRefreshing = AtomicBoolean(false)

    val libraryUpdates = kotlinx.coroutines.flow.MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    // Flag to suppress rapid Room Flow re-emissions during background metadata updates
    @Volatile
    private var suppressFlowUpdates = false
    
    fun setSuppressFlowUpdates(suppress: Boolean) {
        suppressFlowUpdates = suppress
    }

    private val _allSongsFlow = songDao.getAllSongs()
        .debounce(300L) // Throttle: wait 300ms after last DB change before re-emitting
        .combine(favoriteDao.getAllFavoriteIds()) { dbSongs, favoriteIds ->
            // Skip processing if background work is actively writing to DB
            if (suppressFlowUpdates && cachedSongs != null) {
                return@combine cachedSongs!!
            }
            
            val favSet = favoriteIds.toSet()
            val models = dbSongs.map { it.toModel().copy(isFavorite = it.id in favSet) }
                .distinctBy { it.path }
            cachedSongs = models
            
            // Trigger auto-scan ONCE on startup
            if (!metadataFixStarted) {
                metadataFixStarted = true
                Log.d("LibraryScan", "Startup: Auto-scan triggered (${models.size} cached songs)")
                
                MainScope().launch(Dispatchers.IO) {
                    if (models.isEmpty()) {
                        // First launch or empty DB — full scan needed
                        Log.d("LibraryScan", "DB empty, performing full initial scan...")
                        val scannedSongs = mediaStoreScanner.scanAllSongs()
                        if (scannedSongs.isNotEmpty()) {
                            songDao.insertSongs(scannedSongs.map { it.toEntity() })
                            Log.d("LibraryScan", "Initial scan complete: ${scannedSongs.size} songs inserted")
                        }
                    } else {
                        // DB has songs — incremental scan for new/modified
                        val lastModified = songDao.getMaxDateModified() ?: 0L
                        refreshLibraryIncremental(models, lastModified)
                    }
                }
            }
            models
        }
        .flowOn(Dispatchers.IO)
        .shareIn(
            scope = MainScope(),
            started = SharingStarted.WhileSubscribed(5000),
            replay = 1
        )

    fun getAllSongs(): Flow<List<Song>> = _allSongsFlow

    private suspend fun refreshLibraryIncremental(existingSongs: List<Song>, lastModified: Long) {
        try {
            Log.d("LibraryScan", "Starting incremental scan (after $lastModified)...")
            val startTime = System.currentTimeMillis()
            val scannedSongs = mediaStoreScanner.scanAllSongs(lastModifiedAfter = lastModified)
            
            if (scannedSongs.isEmpty()) {
                Log.d("LibraryScan", "No new or modified songs found.")
                return
            }
            
            Log.d("LibraryScan", "Found ${scannedSongs.size} potential updates, batch inserting...")
            songDao.insertSongs(scannedSongs.map { it.toEntity() })
            
            val elapsed = System.currentTimeMillis() - startTime
            Log.d("LibraryScan", "Incremental sync complete in ${elapsed}ms")

            // Background metadata fix for only the new/modified songs
            kotlinx.coroutines.MainScope().launch(Dispatchers.IO) {
                try {
                    val fixedSongs = mediaStoreScanner.fixSuspiciousMetadata(scannedSongs)
                    if (fixedSongs.isNotEmpty()) {
                        songDao.insertSongs(fixedSongs.map { it.toEntity() })
                    }
                } catch (e: Exception) {
                    Log.e("LibraryScan", "Background metadata fix failed", e)
                }
            }
        } catch (e: Exception) {
            Log.e("LibraryScan", "Incremental scan failed", e)
        }
    }

    fun getAllAlbums(): Flow<List<Album>> = getAllSongs().map { songs ->
        val albums = songs.groupBy { it.albumId }.map { (id, albumSongs) ->
            val first = albumSongs.first()
            Album(
                id = id,
                name = first.album,
                artist = first.artist,
                songCount = albumSongs.size,
                year = first.year
            )
        }.sortedBy { it.name }
        cachedAlbums = albums
        albums
    }.flowOn(Dispatchers.IO)

    fun getAllArtists(): Flow<List<Artist>> = getAllSongs().map { songs ->
        val artists = songs.groupBy { it.artist.normalizedArtistGroupingKey() }.map { (normalizedKey, artistSongs) ->
            val displayName = artistSongs
                .map { it.artist.cleanArtistDisplayName() }
                .firstOrNull { it != UNKNOWN_ARTIST_DISPLAY_NAME }
                ?: UNKNOWN_ARTIST_DISPLAY_NAME

            Artist(
                id = normalizedKey.hashCode().toLong(),
                name = displayName,
                songCount = artistSongs.size,
                albumCount = artistSongs.map { it.albumId }.distinct().size
            )
        }.sortedBy { it.name }
        cachedArtists = artists
        artists
    }.flowOn(Dispatchers.IO)

    fun getAllFolders(): Flow<List<Folder>> = getAllSongs().map { songs ->
        val folders = mediaStoreScanner.scanFolders(songs)
        cachedFolders = folders
        folders
    }.flowOn(Dispatchers.IO)

    suspend fun getSongsForAlbum(albumId: Long): List<Song> = withContext(Dispatchers.IO) {
        val allSongs = cachedSongs ?: mediaStoreScanner.scanAllSongs().also { cachedSongs = it }
        allSongs.filter { it.albumId == albumId }.sortedBy { it.trackNumber }
    }

    suspend fun getSongsForArtist(artistName: String): List<Song> = withContext(Dispatchers.IO) {
        val allSongs = cachedSongs ?: mediaStoreScanner.scanAllSongs().also { cachedSongs = it }
        val targetKey = artistName.normalizedArtistGroupingKey()
        allSongs.filter { it.artist.normalizedArtistGroupingKey() == targetKey }
    }

    suspend fun getSongsInFolder(folderPath: String): List<Song> = withContext(Dispatchers.IO) {
        val allSongs = cachedSongs ?: mediaStoreScanner.scanAllSongs().also { cachedSongs = it }
        allSongs.filter { it.folderPath == folderPath }
    }

    suspend fun getFavoriteSongs(): List<Song> = withContext(Dispatchers.IO) {
        val allSongs = cachedSongs ?: mediaStoreScanner.scanAllSongs().also { cachedSongs = it }
        val favoriteIds = favoriteDao.getAllFavoriteIds().first().toSet()
        allSongs.filter { it.id in favoriteIds }.map { it.copy(isFavorite = true) }
    }

    suspend fun toggleFavorite(songId: Long) {
        favoriteDao.toggleFavorite(songId)
    }

    fun isFavorite(songId: Long): Flow<Boolean> = favoriteDao.isFavorite(songId)

    suspend fun getSongById(songId: Long): Song? {
        val allSongs = cachedSongs ?: mediaStoreScanner.scanAllSongs().also { cachedSongs = it }
        return allSongs.find { it.id == songId }
    }

    suspend fun refreshLibrary() = withContext(Dispatchers.IO) {
        if (!isRefreshing.compareAndSet(false, true)) {
            Log.d("LibraryScan", "Refresh already in progress, skipping")
            return@withContext
        }
        try {
            Log.d("LibraryScan", "Starting full library refresh...")
            val startTime = System.currentTimeMillis()
            
            // Scan FIRST, keep old list visible in the UI
            val scannedSongs = mediaStoreScanner.scanAllSongs()
            
            if (scannedSongs.isNotEmpty()) {
                // Suppress flow updates during the atomic swap
                suppressFlowUpdates = true
                
                // Clear and replace in one batch — old list stays visible via cachedSongs
                songDao.clearCache()
                songDao.insertSongs(scannedSongs.map { it.toEntity() })
                
                // Now allow the flow to emit the fresh list
                suppressFlowUpdates = false
            }
            
            // Clear derived caches so they rebuild from fresh data
            cachedAlbums = null
            cachedArtists = null
            cachedFolders = null
            
            val elapsed = System.currentTimeMillis() - startTime
            Log.d("LibraryScan", "Full refresh: ${scannedSongs.size} songs in ${elapsed}ms")
            
            // No external API enrichment — pure offline local player
            // Deferred local metadata fix (file tag based, no internet)
            if (scannedSongs.isNotEmpty()) {
                kotlinx.coroutines.MainScope().launch(Dispatchers.IO) {
                    try {
                        val fixedSongs = mediaStoreScanner.fixSuspiciousMetadata(scannedSongs)
                        if (fixedSongs.isNotEmpty()) {
                            songDao.insertSongs(fixedSongs.map { it.toEntity() })
                        }
                    } catch (e: Exception) {
                        Log.e("LibraryScan", "Background metadata fix failed", e)
                    }
                }
            }
        } finally {
            isRefreshing.set(false)
        }
    }

    suspend fun updateSongMetadata(songId: Long, artist: String, album: String, artworkUrl: String? = null, artistImageUrl: String? = null) = withContext(Dispatchers.IO) {
        songDao.updateSongMetadata(songId, artist, album, artworkUrl, artistImageUrl)
        
        // Save artwork to shared table if available
        if (artworkUrl != null) {
            val key = "${artist}_${album}".lowercase()
            albumArtworkDao.insertArtwork(com.vyn.player.data.local.entity.AlbumArtworkEntity(
                albumKey = key,
                albumName = album,
                artistName = artist,
                artworkPath = artworkUrl
            ))
        }

        // Update in-memory cache silently — no flow emission, no list jump
        cachedSongs = cachedSongs?.map {
            if (it.id == songId) it.copy(
                artist = artist, 
                album = album, 
                artworkUrl = artworkUrl ?: it.artworkUrl, 
                artistImageUrl = artistImageUrl ?: it.artistImageUrl
            ) else it
        }
        cachedAlbums = null
        cachedArtists = null
        // NOTE: We intentionally do NOT call libraryUpdates.tryEmit(Unit) here.
        // Background metadata updates must not trigger UI list refresh / scroll jumps.
    }

    private fun Song.toEntity() = SongEntity(
        id = id,
        title = title,
        artist = artist,
        album = album,
        albumId = albumId,
        duration = duration,
        path = path,
        uriString = uri.toString(),
        trackNumber = trackNumber,
        year = year,
        size = size,
        dateAdded = dateAdded,
        dateModified = dateModified,
        mimeType = mimeType,
        folderName = folderName,
        folderPath = folderPath,
        artworkUrl = artworkUrl,
        artistImageUrl = artistImageUrl
    )

    private fun SongEntity.toModel() = Song(
        id = id,
        title = title,
        artist = artist,
        album = album,
        albumId = albumId,
        duration = duration,
        path = path,
        uri = Uri.parse(uriString),
        trackNumber = trackNumber,
        year = year,
        size = size,
        dateAdded = dateAdded,
        dateModified = dateModified,
        mimeType = mimeType,
        folderName = folderName,
        folderPath = folderPath,
        artworkUrl = artworkUrl,
        artistImageUrl = artistImageUrl
    )

    suspend fun searchSongs(query: String): List<Song> = withContext(Dispatchers.IO) {
        val allSongs = cachedSongs ?: mediaStoreScanner.scanAllSongs().also { cachedSongs = it }
        val lowerQuery = query.lowercase()
        allSongs.filter {
            it.title.lowercase().contains(lowerQuery) ||
                    it.artist.lowercase().contains(lowerQuery) ||
                    it.album.lowercase().contains(lowerQuery)
        }
    }

    // Playback History
    suspend fun recordSongPlayed(songId: Long, duration: Long = 0L) {
        musicHistoryRepository.onSongStarted(songId)
    }

    fun getTopSongs(limit: Int): Flow<List<com.vyn.player.data.local.dao.SongPlayCount>> = playbackHistoryDao.getTopSongs(limit)

    fun getRecentSongs(limit: Int): Flow<List<Long>> = playbackHistoryDao.getRecentSongs(limit)

    // ─── SMART STATS TRACKING ───
    suspend fun updateSongStats(
        songId: Long,
        isCompleted: Boolean, 
        wasSkipped: Boolean,
        completionPercentage: Float
    ) {
        musicHistoryRepository.onSongFinished(songId, 0, 0, wasSkipped, completionPercentage)
    }

    suspend fun getAllSongStats() = songStatsDao.getAllStats()
}

private fun String.cleanArtistDisplayName(): String {
    val trimmed = trim()
    if (trimmed.isBlank()) return UNKNOWN_ARTIST_DISPLAY_NAME
    if (INVALID_NUMERIC_ARTIST_REGEX.matches(trimmed)) return UNKNOWN_ARTIST_DISPLAY_NAME
    if (INVALID_FILENAME_ARTIST_REGEX.matches(trimmed)) return UNKNOWN_ARTIST_DISPLAY_NAME

    val withoutFeature = FEATURE_SEPARATOR_REGEX.split(trimmed, limit = 2).firstOrNull().orEmpty().trim()
    if (withoutFeature.isBlank()) return UNKNOWN_ARTIST_DISPLAY_NAME
    if (INVALID_NUMERIC_ARTIST_REGEX.matches(withoutFeature)) return UNKNOWN_ARTIST_DISPLAY_NAME
    if (INVALID_FILENAME_ARTIST_REGEX.matches(withoutFeature)) return UNKNOWN_ARTIST_DISPLAY_NAME

    return withoutFeature
}

private fun String.normalizedArtistGroupingKey(): String {
    val cleaned = cleanArtistDisplayName()
    return if (cleaned == UNKNOWN_ARTIST_DISPLAY_NAME) {
        UNKNOWN_ARTIST_DISPLAY_NAME.lowercase()
    } else {
        cleaned.lowercase()
    }
}
