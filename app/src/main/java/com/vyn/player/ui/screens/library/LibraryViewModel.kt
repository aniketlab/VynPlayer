package com.vyn.player.ui.screens.library

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

enum class SortType {
    TITLE,
    RECENT,
    ARTIST,
    DURATION
}

private fun getSectionTitle(title: String): String {
    val firstChar = title.trim().firstOrNull()?.uppercaseChar() ?: '#'
    return if (firstChar in 'A'..'Z') firstChar.toString() else "#"
}

private fun List<Song>.sortedFor(sortType: SortType): List<Song> {
    return when (sortType) {
        SortType.TITLE -> sortedWith(
            compareBy<Song> {
                val section = getSectionTitle(it.title)
                if (section == "#") "ZZZ" else section
            }.thenBy {
                it.title.lowercase()
            }.thenBy {
                it.id
            }
        )
        SortType.RECENT -> sortedByDescending { it.dateAdded }
        SortType.ARTIST -> sortedWith(compareBy<Song> { it.artist.lowercase() }.thenBy { it.title.lowercase() }.thenBy { it.id })
        SortType.DURATION -> sortedWith(compareByDescending<Song> { it.duration }.thenBy { it.title.lowercase() }.thenBy { it.id })
    }
}

@Immutable
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
    val sortType: SortType = SortType.TITLE
)

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val musicRepository: MusicRepository,
    private val playlistRepository: PlaylistRepository,
    private val userPreferencesManager: UserPreferencesManager,
    private val smartMixRepository: SmartMixRepository,
    private val mediaStoreScanner: com.vyn.player.data.repository.MediaStoreScanner,
    val playbackManager: PlaybackManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()
    private var currentSortType by mutableStateOf(SortType.TITLE)
        private set
    var isRefreshing by mutableStateOf(false)
        private set
    var songs by mutableStateOf<List<Song>>(emptyList())
        private set
    private var refreshJob: Job? = null

    val playbackState: StateFlow<PlaybackState> = playbackManager.playbackState

    init {
        observeLibraryState()
        refreshSmartMix()
    }

    private fun observeLibraryState() {
        val libraryDataFlow = combine(
            musicRepository.getAllSongs(),
            musicRepository.getAllAlbums(),
            musicRepository.getAllArtists(),
            musicRepository.getAllFolders(),
            playlistRepository.getAllPlaylists()
        ) { songs, albums, artists, folders, playlists ->
            LibraryData(
                songs = songs,
                albums = albums,
                artists = artists,
                folders = folders,
                playlists = playlists
            )
        }

        val playbackDataFlow = combine(
            musicRepository.getRecentSongs(50),
            musicRepository.getTopSongs(50)
        ) { recentIds, topSongs ->
            PlaybackLibraryData(recentIds = recentIds, topSongs = topSongs)
        }

        val userProfileFlow = combine(
            userPreferencesManager.userDisplayName,
            userPreferencesManager.userSubtitle,
            userPreferencesManager.userAvatarUrl
        ) { name, subtitle, avatar ->
            UserProfileData(name = name, subtitle = subtitle, avatar = avatar)
        }

        combine(libraryDataFlow, playbackDataFlow, userProfileFlow) { libraryData, playbackData, userProfile ->
            val selectedSort = currentSortType
            val songsById = libraryData.songs.associateBy(Song::id)
            val sortedSongs = libraryData.songs.sortedFor(selectedSort)
            songs = libraryData.songs

            LibraryUiState(
                songs = libraryData.songs,
                displayedSongs = sortedSongs,
                albums = libraryData.albums,
                artists = libraryData.artists,
                playlists = libraryData.playlists,
                folders = libraryData.folders,
                isLoading = false,
                selectedTab = _uiState.value.selectedTab,
                searchQuery = _uiState.value.searchQuery,
                searchResults = _uiState.value.searchResults,
                userDisplayName = userProfile.name,
                userSubtitle = userProfile.subtitle,
                userAvatarUrl = userProfile.avatar,
                recentSongs = playbackData.recentIds.mapNotNull(songsById::get).distinctBy(Song::id).take(50),
                topSongs = playbackData.topSongs.filter { songsById.containsKey(it.songId) },
                smartMixSongs = _uiState.value.smartMixSongs,
                sortType = selectedSort,
                error = _uiState.value.error,
            )
        }
            .distinctUntilChanged()
            .onEach { newState -> _uiState.value = newState }
            .launchIn(viewModelScope)
    }

    private data class LibraryData(
        val songs: List<Song>,
        val albums: List<Album>,
        val artists: List<Artist>,
        val folders: List<Folder>,
        val playlists: List<Playlist>
    )

    private data class PlaybackLibraryData(
        val recentIds: List<Long>,
        val topSongs: List<SongPlayCount>
    )

    private data class UserProfileData(
        val name: String,
        val subtitle: String,
        val avatar: String?
    )

    fun refreshSmartMix(force: Boolean = false) {
        viewModelScope.launch(Dispatchers.IO) {
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

    fun setSortType(sortType: SortType) {
        currentSortType = sortType
        _uiState.update {
            it.copy(
                sortType = sortType,
                displayedSongs = it.songs.sortedFor(sortType)
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
                        displayedSongs = songs.sortedFor(currentSortType)
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
        if (isRefreshing) return

        refreshJob?.cancel()

        refreshJob = viewModelScope.launch {
            isRefreshing = true

            val timeoutJob = launch {
                delay(5000)
                if (isRefreshing) {
                    isRefreshing = false
                }
            }

            try {
                val result = withContext(Dispatchers.IO) {
                    mediaStoreScanner.scanLibrary()
                }

                songs = result
            } catch (_: Exception) {
            } finally {
                timeoutJob.cancel()
                isRefreshing = false
            }
        }
    }

    fun search(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        if (query.isBlank()) {
            _uiState.update { it.copy(searchResults = emptyList()) }
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
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
