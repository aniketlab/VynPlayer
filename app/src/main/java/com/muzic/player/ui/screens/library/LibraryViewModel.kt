package com.muzic.player.ui.screens.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.muzic.player.data.model.Album
import com.muzic.player.data.model.Artist
import com.muzic.player.data.model.Folder
import com.muzic.player.data.model.Playlist
import com.muzic.player.data.model.Song
import com.muzic.player.data.repository.MusicRepository
import com.muzic.player.data.repository.PlaylistRepository
import com.muzic.player.data.preferences.UserPreferencesManager
import com.muzic.player.player.PlaybackManager
import com.muzic.player.player.PlaybackState
import com.muzic.player.data.local.dao.SongPlayCount
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LibraryUiState(
    val songs: List<Song> = emptyList(),
    val albums: List<Album> = emptyList(),
    val artists: List<Artist> = emptyList(),
    val playlists: List<Playlist> = emptyList(),
    val folders: List<Folder> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val selectedTab: Int = 0,
    val searchQuery: String = "",
    val searchResults: List<Song> = emptyList(),
    val userDisplayName: String = "Music Lover",
    val userSubtitle: String = "Music Enthusiast",
    val userAvatarUrl: String? = null,
    val recentSongs: List<Song> = emptyList(),
    val topSongs: List<SongPlayCount> = emptyList()
)

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val musicRepository: MusicRepository,
    private val playlistRepository: PlaylistRepository,
    private val userPreferencesManager: UserPreferencesManager,
    val playbackManager: PlaybackManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    val playbackState: StateFlow<PlaybackState> = playbackManager.playbackState

    init {
        loadLibrary()
        
        viewModelScope.launch {
            combine(
                userPreferencesManager.userDisplayName,
                userPreferencesManager.userSubtitle,
                userPreferencesManager.userAvatarUrl
            ) { name, sub, avatar ->
                Triple(name, sub, avatar)
            }.collect { (name, sub, avatar) ->
                _uiState.update { 
                    it.copy(
                        userDisplayName = name,
                        userSubtitle = sub,
                        userAvatarUrl = avatar
                    )
                }
            }
        }

        viewModelScope.launch {
            playbackState
                .map { it.currentSong?.id }
                .distinctUntilChanged()
                .collect { songId ->
                    if (songId != null) {
                        musicRepository.recordSongPlayed(songId)
                    }
                }
        }
        
        viewModelScope.launch {
            musicRepository.getRecentSongs(50).collect { recentIds ->
                val allSongs = _uiState.value.songs
                if (allSongs.isNotEmpty()) {
                    val recentSongs = recentIds.mapNotNull { id -> allSongs.find { it.id == id } }
                    _uiState.update { it.copy(recentSongs = recentSongs) }
                }
            }
        }

        viewModelScope.launch {
            musicRepository.getTopSongs(50).collect { topSongs ->
                _uiState.update { it.copy(topSongs = topSongs) }
            }
        }
    }

    fun loadLibrary() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                // Load songs
                musicRepository.getAllSongs().collect { songs ->
                    _uiState.update { it.copy(songs = songs, isLoading = false) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message, isLoading = false) }
            }
        }

        viewModelScope.launch {
            musicRepository.getAllAlbums().collect { albums ->
                _uiState.update { it.copy(albums = albums) }
            }
        }

        viewModelScope.launch {
            musicRepository.getAllArtists().collect { artists ->
                _uiState.update { it.copy(artists = artists) }
            }
        }

        viewModelScope.launch {
            playlistRepository.getAllPlaylists().collect { playlists ->
                _uiState.update { it.copy(playlists = playlists) }
            }
        }

        viewModelScope.launch {
            musicRepository.getAllFolders().collect { folders ->
                _uiState.update { it.copy(folders = folders) }
            }
        }
    }

    fun setSelectedTab(index: Int) {
        _uiState.update { it.copy(selectedTab = index) }
    }

    fun playSong(song: Song, queue: List<Song>? = null) {
        val songList = queue ?: _uiState.value.songs
        val index = songList.indexOf(song).coerceAtLeast(0)
        playbackManager.playQueue(songList, index)
    }

    fun toggleFavorite(songId: Long) {
        viewModelScope.launch {
            musicRepository.toggleFavorite(songId)
            // Refresh the song list to update favorite status
            musicRepository.getAllSongs().collect { songs ->
                _uiState.update { it.copy(songs = songs) }
            }
        }
    }

    fun createPlaylist(name: String) {
        viewModelScope.launch {
            playlistRepository.createPlaylist(name)
        }
    }

    fun deletePlaylist(playlistId: Long) {
        viewModelScope.launch {
            playlistRepository.deletePlaylist(playlistId)
        }
    }

    fun refreshLibrary() {
        viewModelScope.launch {
            musicRepository.refreshLibrary()
            loadLibrary()
        }
    }

    fun search(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        if (query.isBlank()) {
            _uiState.update { it.copy(searchResults = emptyList()) }
            return
        }
        viewModelScope.launch {
            val results = musicRepository.searchSongs(query)
            _uiState.update { it.copy(searchResults = results) }
        }
    }

    // Playback controls (delegated to manager)
    fun togglePlayPause() = playbackManager.togglePlayPause()
    fun skipToNext() = playbackManager.skipToNext()
    fun skipToPrevious() = playbackManager.skipToPrevious()
    
    fun updateUserProfile(displayName: String, subtitle: String, avatarUrl: String? = null) {
        viewModelScope.launch {
            userPreferencesManager.saveUserProfile(avatarUrl, displayName, subtitle)
        }
    }
    
    suspend fun getPlaylistWithSongs(playlistId: Long): com.muzic.player.data.model.Playlist? {
        return playlistRepository.getPlaylistWithSongs(playlistId)
    }

    suspend fun addSongToPlaylist(playlistId: Long, songId: Long) {
        playlistRepository.addSongToPlaylist(playlistId, songId)
    }

    suspend fun removeSongFromPlaylist(playlistId: Long, songId: Long) {
        playlistRepository.removeSongFromPlaylist(playlistId, songId)
    }
}
