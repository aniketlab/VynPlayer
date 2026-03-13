package com.vyn.player.ui.screens.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vyn.player.data.model.Album
import com.vyn.player.data.model.Artist
import com.vyn.player.data.model.Folder
import com.vyn.player.data.model.Playlist
import com.vyn.player.data.model.Song
import com.vyn.player.data.repository.MusicRepository
import com.vyn.player.data.repository.PlaylistRepository
import com.vyn.player.data.preferences.UserPreferencesManager
import com.vyn.player.player.PlaybackManager
import com.vyn.player.player.PlaybackState
import com.vyn.player.data.repository.SmartMixRepository
import com.vyn.player.data.local.dao.SongPlayCount
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class LibrarySortOption {
    AZ,
    RECENTLY_ADDED,
    ARTIST,
    DURATION
}

private fun Song.normalizedTitleForSort(): String {
    val title = title.lowercase()
    return when {
        title.startsWith("the ") -> title.substring(4)
        title.startsWith("a ") -> title.substring(2)
        title.startsWith("an ") -> title.substring(3)
        else -> title
    }.trim()
}

private fun Song.normalizedArtistForSort(): String {
    return artist.trim().lowercase().ifBlank { "unknown artist" }
}

private fun List<Song>.sortedFor(option: LibrarySortOption): List<Song> {
    return when (option) {
        LibrarySortOption.AZ -> sortedWith(compareBy<Song> { it.normalizedTitleForSort() }.thenBy { it.id })
        LibrarySortOption.RECENTLY_ADDED -> sortedWith(compareByDescending<Song> { it.dateAdded }.thenBy { it.normalizedTitleForSort() })
        LibrarySortOption.ARTIST -> sortedWith(compareBy<Song> { it.normalizedArtistForSort() }.thenBy { it.normalizedTitleForSort() }.thenBy { it.id })
        LibrarySortOption.DURATION -> sortedWith(compareByDescending<Song> { it.duration }.thenBy { it.normalizedTitleForSort() })
    }
}

data class LibraryUiState(
    val songs: List<Song> = emptyList(),
    val displayedSongs: List<Song> = emptyList(),
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
    val topSongs: List<SongPlayCount> = emptyList(),
    val smartMixSongs: List<Song> = emptyList(),
    val selectedSortOption: LibrarySortOption = LibrarySortOption.AZ
)

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val musicRepository: MusicRepository,
    private val playlistRepository: PlaylistRepository,
    private val userPreferencesManager: UserPreferencesManager,
    private val smartMixRepository: SmartMixRepository,
    val playbackManager: PlaybackManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    val playbackState: StateFlow<PlaybackState> = playbackManager.playbackState

    init {
        // 1. Observe User Preferences
        viewModelScope.launch {
            combine(
                userPreferencesManager.userDisplayName,
                userPreferencesManager.userSubtitle,
                userPreferencesManager.userAvatarUrl
            ) { name: String, sub: String, avatar: String? ->
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

        // 2. Observe Songs (Reactive)
        musicRepository.getAllSongs()
            .onEach { songs ->
                _uiState.update {
                    it.copy(
                        songs = songs,
                        displayedSongs = songs.sortedFor(it.selectedSortOption),
                        isLoading = false
                    )
                }
            }
            .launchIn(viewModelScope)

        // 3. Observe Albums (Reactive)
        musicRepository.getAllAlbums()
            .onEach { albums -> _uiState.update { it.copy(albums = albums) } }
            .launchIn(viewModelScope)

        // 4. Observe Artists (Reactive)
        musicRepository.getAllArtists()
            .onEach { artists -> _uiState.update { it.copy(artists = artists) } }
            .launchIn(viewModelScope)

        // 5. Observe Folders (Reactive)
        musicRepository.getAllFolders()
            .onEach { folders -> _uiState.update { it.copy(folders = folders) } }
            .launchIn(viewModelScope)

        // 6. Observe Playlists (Reactive)
        playlistRepository.getAllPlaylists()
            .onEach { playlists -> _uiState.update { it.copy(playlists = playlists) } }
            .launchIn(viewModelScope)
        
        // 7. Recent Songs
        viewModelScope.launch {
            kotlinx.coroutines.flow.combine(
                musicRepository.getAllSongs(),
                musicRepository.getRecentSongs(50)
            ) { allSongs, recentIds ->
                recentIds.mapNotNull { id -> allSongs.find { it.id == id } }
            }.collect { recentSongs ->
                _uiState.update { it.copy(recentSongs = recentSongs.distinctBy { it.id }.take(50)) }
            }
        }

        // 8. Top Songs
        viewModelScope.launch {
            kotlinx.coroutines.flow.combine(
                musicRepository.getAllSongs(),
                musicRepository.getTopSongs(50)
            ) { allSongs, topCounters ->
                topCounters.filter { counter -> allSongs.any { it.id == counter.songId } }
            }.collect { validTopSongs ->
                _uiState.update { it.copy(topSongs = validTopSongs) }
            }
        }

        refreshSmartMix()
    }

    fun refreshSmartMix(force: Boolean = false) {
        viewModelScope.launch {
            val mix = smartMixRepository.getLatestMixSongs(force)
            _uiState.update { it.copy(smartMixSongs = mix) }
        }
    }

    fun playSmartMix() {
        val currentMix = _uiState.value.smartMixSongs
        if (currentMix.isNotEmpty()) {
            playbackManager.playQueue(currentMix, 0)
        } else {
            viewModelScope.launch {
                val mix = smartMixRepository.getLatestMixSongs()
                if (mix.isNotEmpty()) {
                    _uiState.update { it.copy(smartMixSongs = mix) }
                    playbackManager.playQueue(mix, 0)
                }
            }
        }
    }

    fun loadLibrary() {
        // Redundant with reactive streams, but kept for manual refresh if needed
        viewModelScope.launch {
             _uiState.update { it.copy(isLoading = true) }
             // Initial scan trigger is already in MusicRepository.getAllSongs()
        }
    }

    fun setSelectedTab(index: Int) {
        _uiState.update { it.copy(selectedTab = index) }
    }

    fun playSong(song: Song, queue: List<Song>? = null) {
        val songList = queue ?: _uiState.value.displayedSongs
        val index = songList.indexOf(song).coerceAtLeast(0)
        playbackManager.playQueue(songList, index)
    }

    fun setSortOption(option: LibrarySortOption) {
        _uiState.update {
            it.copy(
                selectedSortOption = option,
                displayedSongs = it.songs.sortedFor(option)
            )
        }
    }

    fun playAllDisplayedSongs() {
        val songs = _uiState.value.displayedSongs
        if (songs.isNotEmpty()) {
            playbackManager.playQueue(songs, 0)
        }
    }

    fun shuffleAllDisplayedSongs() {
        val songs = _uiState.value.displayedSongs
        if (songs.isNotEmpty()) {
            val shuffledSongs = songs.shuffled()
            playbackManager.playQueue(shuffledSongs, 0)
        }
    }

    fun toggleFavorite(songId: Long) {
        viewModelScope.launch {
            musicRepository.toggleFavorite(songId)
            // Refresh the song list to update favorite status
            musicRepository.getAllSongs().collect { songs ->
                _uiState.update {
                    it.copy(
                        songs = songs,
                        displayedSongs = songs.sortedFor(it.selectedSortOption)
                    )
                }
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
    
    suspend fun getPlaylistWithSongs(playlistId: Long): com.vyn.player.data.model.Playlist? {
        return playlistRepository.getPlaylistWithSongs(playlistId)
    }

    suspend fun addSongToPlaylist(playlistId: Long, songId: Long) {
        playlistRepository.addSongToPlaylist(playlistId, songId)
    }

    suspend fun removeSongFromPlaylist(playlistId: Long, songId: Long) {
        playlistRepository.removeSongFromPlaylist(playlistId, songId)
    }
}
