package com.muzic.player.data.repository

import com.muzic.player.data.local.dao.FavoriteDao
import com.muzic.player.data.local.entity.FavoriteEntity
import com.muzic.player.data.model.Album
import com.muzic.player.data.model.Artist
import com.muzic.player.data.model.Folder
import com.muzic.player.data.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import android.net.Uri
import android.content.ContentUris
import com.muzic.player.data.local.dao.SongDao
import com.muzic.player.data.local.entity.SongEntity
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject
import javax.inject.Singleton

import com.muzic.player.data.local.dao.PlaybackHistoryDao
import com.muzic.player.data.local.entity.PlaybackHistoryEntity

@Singleton
class MusicRepository @Inject constructor(
    private val mediaStoreScanner: MediaStoreScanner,
    private val favoriteDao: FavoriteDao,
    private val playbackHistoryDao: PlaybackHistoryDao,
    private val songDao: SongDao
) {
    // Cached data
    private var cachedSongs: List<Song>? = null
    private var cachedAlbums: List<Album>? = null
    private var cachedArtists: List<Artist>? = null
    private var cachedFolders: List<Folder>? = null

    fun getAllSongs(): Flow<List<Song>> = flow {
        // 1. Memory Cache
        cachedSongs?.let {
            val favoriteIds = favoriteDao.getAllFavoriteIds().first().toSet()
            emit(it.map { s -> s.copy(isFavorite = s.id in favoriteIds) })
            return@flow
        }

        // 2. DB Cache
        val dbSongs = songDao.getAllSongs().first()
        if (dbSongs.isNotEmpty()) {
            val songs = dbSongs.map { it.toModel() }
            cachedSongs = songs
            val favoriteIds = favoriteDao.getAllFavoriteIds().first().toSet()
            emit(songs.map { s -> s.copy(isFavorite = s.id in favoriteIds) })
            return@flow
        }

        // 3. Scan
        val scannedSongs = mediaStoreScanner.scanAllSongs()
        songDao.insertSongs(scannedSongs.map { it.toEntity() })
        cachedSongs = scannedSongs
        val favoriteIds = favoriteDao.getAllFavoriteIds().first().toSet()
        emit(scannedSongs.map { s -> s.copy(isFavorite = s.id in favoriteIds) })
    }.flowOn(Dispatchers.IO)

    fun getAllAlbums(): Flow<List<Album>> = flow {
        val albums = cachedAlbums ?: mediaStoreScanner.scanAlbums().also { cachedAlbums = it }
        emit(albums)
    }.flowOn(Dispatchers.IO)

    fun getAllArtists(): Flow<List<Artist>> = flow {
        val artists = cachedArtists ?: mediaStoreScanner.scanArtists().also { cachedArtists = it }
        emit(artists)
    }.flowOn(Dispatchers.IO)

    fun getAllFolders(): Flow<List<Folder>> = flow {
        val songs = cachedSongs ?: mediaStoreScanner.scanAllSongs().also { cachedSongs = it }
        val folders = cachedFolders ?: mediaStoreScanner.scanFolders(songs).also { cachedFolders = it }
        emit(folders)
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
        cachedSongs = null
        cachedAlbums = null
        cachedArtists = null
        cachedFolders = null
        songDao.clearCache()
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
        folderPath = folderPath
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
        folderPath = folderPath
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
    suspend fun recordSongPlayed(songId: Long) {
        playbackHistoryDao.insert(PlaybackHistoryEntity(songId = songId))
    }

    fun getTopSongs(limit: Int): Flow<List<com.muzic.player.data.local.dao.SongPlayCount>> = playbackHistoryDao.getTopSongs(limit)

    fun getRecentSongs(limit: Int): Flow<List<Long>> = playbackHistoryDao.getRecentSongs(limit)
}
