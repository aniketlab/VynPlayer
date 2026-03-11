package com.muzic.player.data.repository

import com.muzic.player.data.local.dao.FavoriteDao
import com.muzic.player.data.local.entity.FavoriteEntity
import com.muzic.player.data.model.Album
import com.muzic.player.data.model.Artist
import com.muzic.player.data.model.Folder
import com.muzic.player.data.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import android.net.Uri
import android.content.ContentUris
import com.muzic.player.data.local.dao.SongDao
import com.muzic.player.data.local.entity.SongEntity
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject
import javax.inject.Singleton

import com.muzic.player.data.local.dao.PlaybackHistoryDao
import com.muzic.player.data.local.dao.SongStatsDao
import com.muzic.player.data.local.entity.SongStatsEntity
import com.muzic.player.data.local.entity.PlaybackHistoryEntity
import android.util.Log
import java.util.concurrent.atomic.AtomicBoolean

@Singleton
class MusicRepository @Inject constructor(
    private val mediaStoreScanner: MediaStoreScanner,
    private val favoriteDao: FavoriteDao,
    private val playbackHistoryDao: PlaybackHistoryDao,
    private val songDao: SongDao,
    private val songStatsDao: SongStatsDao,
    private val albumArtworkDao: com.muzic.player.data.local.dao.AlbumArtworkDao,
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

    private val _allSongsFlow = songDao.getAllSongs()
        .combine(favoriteDao.getAllFavoriteIds()) { dbSongs, favoriteIds ->
            val favSet = favoriteIds.toSet()
            val models = dbSongs.map { it.toModel().copy(isFavorite = it.id in favSet) }
            cachedSongs = models
            
            // Trigger background sync ONLY ONCE at startup when data becomes available
            if (!metadataFixStarted) {
                metadataFixStarted = true
                Log.d("LibraryScan", "Startup: Loading background scan & correction")
                
                // Trigger incremental sync in background (doesn't block UI)
                MainScope().launch(Dispatchers.IO) {
                    val lastModified = songDao.getMaxDateModified() ?: 0L
                    refreshLibraryIncremental(models, lastModified)
                    
                    // After incremental scan, if we have metadata fix workers, start them
                    if (models.isNotEmpty()) {
                        metadataCorrectionRepository.get().startCorrection(models)
                        artworkRepository.startPrefetch(models)
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
        val artists = songs.groupBy { it.artist }.map { (name, artistSongs) ->
            Artist(
                id = name.hashCode().toLong(),
                name = name,
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
        allSongs.filter { it.artist.equals(artistName, ignoreCase = true) }
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
            
            // Clear caches
            cachedSongs = null
            cachedAlbums = null
            cachedArtists = null
            cachedFolders = null
            songDao.clearCache()
            
            // Fast scan and batch insert
            val scannedSongs = mediaStoreScanner.scanAllSongs()
            if (scannedSongs.isNotEmpty()) {
                songDao.insertSongs(scannedSongs.map { it.toEntity() })
            }
            
            val elapsed = System.currentTimeMillis() - startTime
            Log.d("LibraryScan", "Full refresh: ${scannedSongs.size} songs in ${elapsed}ms")
            
            // Background workers
            if (scannedSongs.isNotEmpty()) {
                metadataCorrectionRepository.get().startCorrection(scannedSongs)
                artworkRepository.startPrefetch(scannedSongs)
                
                // Deferred metadata fix
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
            albumArtworkDao.insertArtwork(com.muzic.player.data.local.entity.AlbumArtworkEntity(
                albumKey = key,
                albumName = album,
                artistName = artist,
                artworkPath = artworkUrl
            ))
        }

        // Update live memory cache and clear derived caches so they rebuild on next request
        val fixedSongs = cachedSongs?.map {
            if (it.id == songId) it.copy(artist = artist, album = album, artworkUrl = artworkUrl ?: it.artworkUrl, artistImageUrl = artistImageUrl ?: it.artistImageUrl) else it
        } ?: return@withContext

        cachedSongs = fixedSongs
        cachedAlbums = null
        cachedArtists = null
        
        libraryUpdates.tryEmit(Unit)
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

    fun getTopSongs(limit: Int): Flow<List<com.muzic.player.data.local.dao.SongPlayCount>> = playbackHistoryDao.getTopSongs(limit)

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
