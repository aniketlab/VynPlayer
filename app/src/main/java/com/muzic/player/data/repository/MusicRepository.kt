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
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MusicRepository @Inject constructor(
    private val mediaStoreScanner: MediaStoreScanner,
    private val favoriteDao: FavoriteDao
) {
    // Cached data
    private var cachedSongs: List<Song>? = null
    private var cachedAlbums: List<Album>? = null
    private var cachedArtists: List<Artist>? = null
    private var cachedFolders: List<Folder>? = null

    fun getAllSongs(): Flow<List<Song>> = flow {
        val songs = cachedSongs ?: mediaStoreScanner.scanAllSongs().also { cachedSongs = it }
        val favoriteIds = favoriteDao.getAllFavoriteIds().first().toSet()
        emit(songs.map { it.copy(isFavorite = it.id in favoriteIds) })
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

    fun refreshLibrary() {
        cachedSongs = null
        cachedAlbums = null
        cachedArtists = null
        cachedFolders = null
    }

    suspend fun searchSongs(query: String): List<Song> = withContext(Dispatchers.IO) {
        val allSongs = cachedSongs ?: mediaStoreScanner.scanAllSongs().also { cachedSongs = it }
        val lowerQuery = query.lowercase()
        allSongs.filter {
            it.title.lowercase().contains(lowerQuery) ||
                    it.artist.lowercase().contains(lowerQuery) ||
                    it.album.lowercase().contains(lowerQuery)
        }
    }
}
