package com.vyn.player.data.repository

import com.vyn.player.data.local.dao.PlaylistDao
import com.vyn.player.data.local.entity.PlaylistEntity
import com.vyn.player.data.local.entity.PlaylistSongCrossRef
import com.vyn.player.data.model.Playlist
import com.vyn.player.data.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaylistRepository @Inject constructor(
    private val playlistDao: PlaylistDao,
    private val musicRepository: MusicRepository
) {
    fun getAllPlaylists(): Flow<List<Playlist>> {
        return playlistDao.getAllPlaylists().map { entities ->
            entities.map { entity ->
                val songCount = playlistDao.getSongCountForPlaylist(entity.id).first()
                Playlist(
                    id = entity.id,
                    name = entity.name,
                    songCount = songCount,
                    createdAt = entity.createdAt
                )
            }
        }
    }

    suspend fun createPlaylist(name: String): Long {
        return playlistDao.insertPlaylist(
            PlaylistEntity(name = name)
        )
    }

    suspend fun deletePlaylist(playlistId: Long) {
        val playlist = playlistDao.getPlaylistById(playlistId) ?: return
        playlistDao.deletePlaylist(playlist)
    }

    suspend fun renamePlaylist(playlistId: Long, newName: String) {
        val playlist = playlistDao.getPlaylistById(playlistId) ?: return
        playlistDao.updatePlaylist(
            playlist.copy(name = newName, updatedAt = System.currentTimeMillis())
        )
    }

    suspend fun addSongToPlaylist(playlistId: Long, songId: Long) {
        val songCount = playlistDao.getSongCountForPlaylist(playlistId).first()
        playlistDao.addSongToPlaylist(
            PlaylistSongCrossRef(
                playlistId = playlistId,
                songId = songId,
                sortOrder = songCount
            )
        )
        // Update the playlist's updatedAt timestamp
        val playlist = playlistDao.getPlaylistById(playlistId) ?: return
        playlistDao.updatePlaylist(playlist.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun removeSongFromPlaylist(playlistId: Long, songId: Long) {
        playlistDao.removeSongFromPlaylist(
            PlaylistSongCrossRef(playlistId = playlistId, songId = songId)
        )
    }

    suspend fun getPlaylistSongs(playlistId: Long): List<Song> = withContext(Dispatchers.IO) {
        val songIds = playlistDao.getSongIdsForPlaylist(playlistId)
        songIds.mapNotNull { songId ->
            musicRepository.getSongById(songId)
        }
    }

    suspend fun getPlaylistWithSongs(playlistId: Long): Playlist? = withContext(Dispatchers.IO) {
        val entity = playlistDao.getPlaylistById(playlistId) ?: return@withContext null
        val songs = getPlaylistSongs(playlistId)
        Playlist(
            id = entity.id,
            name = entity.name,
            songCount = songs.size,
            createdAt = entity.createdAt,
            songs = songs
        )
    }
}
