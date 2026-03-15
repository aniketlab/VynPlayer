package com.vyn.player.ui.screens.discover

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vyn.player.data.model.Album
import com.vyn.player.data.model.Artist
import com.vyn.player.data.model.Folder
import com.vyn.player.data.model.Song
import com.vyn.player.ui.actions.LocalSongActionDispatcher
import com.vyn.player.ui.actions.SongAction
import com.vyn.player.ui.components.SongItem
import com.vyn.player.ui.components.bounceClick
import com.vyn.player.ui.screens.library.LibraryViewModel
import kotlin.math.absoluteValue
import kotlin.text.Regex

// ═══════════════════════════════════════════════════════════════
// Top bar used by all "See All" screens
// ═══════════════════════════════════════════════════════════════

@Composable
private fun SeeAllTopBar(
    title: String,
    subtitle: String? = null,
    onBack: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 8.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(
                Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = "Back",
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
        Column(modifier = Modifier.weight(1f).padding(start = 4.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        }
    }
}

@Composable
private fun SectionSearchBar(
    query: String,
    placeholder: String,
    modifier: Modifier = Modifier,
    onQueryChange: (String) -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Rounded.Search,
            contentDescription = "Search",
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.width(12.dp))
        TextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text(
                    text = placeholder,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 15.sp
                )
            },
            singleLine = true,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
                cursorColor = MaterialTheme.colorScheme.primary
            )
        )
    }
}

// ═══════════════════════════════════════════════════════════════
// 1. All Recently Added
// ═══════════════════════════════════════════════════════════════

@Composable
fun AllRecentlyAddedScreen(
    onNavigateBack: () -> Unit,
    viewModel: LibraryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val playbackState by viewModel.playbackState.collectAsStateWithLifecycle()
    val songActionDispatcher = LocalSongActionDispatcher.current

    val recentlyAdded = remember(uiState.songs) {
        uiState.songs.sortedByDescending { it.dateAdded }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        SeeAllTopBar(
            title = "Recently Added",
            subtitle = "${recentlyAdded.size} songs",
            onBack = onNavigateBack
        )

        if (recentlyAdded.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "No recently added songs.",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        } else {
            LazyColumn(contentPadding = PaddingValues(bottom = 144.dp)) {
                items(recentlyAdded, key = { it.id }) { song ->
                    SongItem(
                        song = song,
                        isPlaying = playbackState.currentSong?.id == song.id,
                        isPlaybackActive = playbackState.isPlaying,
                        onSongClick = { songActionDispatcher(SongAction.Play(song, recentlyAdded)) },
                        onAction = songActionDispatcher,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// 2. All Artists
// ═══════════════════════════════════════════════════════════════

@Composable
fun AllArtistsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToArtist: (String) -> Unit,
    viewModel: LibraryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var searchQuery by rememberSaveable { mutableStateOf("") }
    val artists = remember(uiState.artists, searchQuery) {
        uiState.artists
            .toStructuredArtists()
            .filter { it.name.contains(searchQuery, ignoreCase = true) }
    }
    val groupedArtists = remember(artists) {
        artists.groupBy { artist ->
            artist.name.firstOrNull()?.uppercaseChar()?.takeIf { it.isLetter() } ?: '#'
        }.toSortedMap()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        SeeAllTopBar(
            title = "All Artists",
            subtitle = "${artists.size} artists",
            onBack = onNavigateBack
        )

        SectionSearchBar(
            query = searchQuery,
            placeholder = "Search artists",
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            onQueryChange = { searchQuery = it }
        )

        if (artists.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    if (searchQuery.isBlank()) "No artists found." else "No matching artists found.",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        } else {
            LazyColumn(contentPadding = PaddingValues(bottom = 144.dp)) {
                groupedArtists.forEach { (letter, artistsInSection) ->
                    item(key = "header_$letter") {
                        ArtistSectionHeader(letter = letter.toString())
                    }
                    items(artistsInSection, key = { it.id }) { artist ->
                        ArtistListItem(
                            artist = artist,
                            onClick = { onNavigateToArtist(artist.name) }
                        )
                    }
                }
            }
        }
    }
}

private fun List<Artist>.toStructuredArtists(): List<Artist> {
    return groupBy { artist -> artist.name.normalizedArtistKey() }
        .map { (normalizedKey, groupedArtists) ->
            val firstArtist = groupedArtists.first()
            val displayName = if (normalizedKey == UNKNOWN_ARTIST_KEY) "Unknown Artist" else firstArtist.name.trim()
            Artist(
                id = groupedArtists.minOf { it.id },
                name = displayName,
                songCount = groupedArtists.sumOf { it.songCount },
                albumCount = groupedArtists.sumOf { it.albumCount }
            )
        }
        .filter { artist -> artist.shouldDisplayArtist() }
        .sortedBy { it.name.lowercase() }
}

private fun String.normalizedArtistKey(): String {
    val normalized = trim().lowercase()
    return if (normalized.isBlank() || normalized == "unknown artist" || normalized == "<unknown>" || normalized == "unknown") {
        UNKNOWN_ARTIST_KEY
    } else {
        normalized
    }
}

private const val UNKNOWN_ARTIST_KEY = "__unknown_artist__"
private val INVALID_NUMERIC_ARTIST_REGEX = Regex("^\\d+$")
private val INVALID_FILENAME_ARTIST_REGEX = Regex("^\\d+(?:[\\s_].*|[a-zA-Z].*)?$", RegexOption.IGNORE_CASE)

private fun Artist.shouldDisplayArtist(): Boolean {
    if (name == "Unknown Artist") return true

    val trimmedName = name.trim()
    if (trimmedName.isBlank()) return false

    if (INVALID_NUMERIC_ARTIST_REGEX.matches(trimmedName)) return false
    if (INVALID_FILENAME_ARTIST_REGEX.matches(trimmedName)) return false

    if (trimmedName.length < 2) {
        val onlyLetters = trimmedName.all { it.isLetter() }
        if (!onlyLetters) return false
    }

    return true
}

@Composable
private fun ArtistSectionHeader(letter: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = letter,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(12.dp))
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
        )
    }
}

@Composable
private fun ArtistListItem(
    artist: Artist,
    onClick: () -> Unit
) {
    val hash = artist.name.hashCode()
    val hue = (hash % 360).toFloat().absoluteValue

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f))
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f),
                shape = RoundedCornerShape(16.dp)
            )
            .bounceClick(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Circle avatar
        Surface(
            modifier = Modifier.size(52.dp),
            shape = CircleShape,
            color = Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, 0.5f, 0.5f)))
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = artist.name.take(1).uppercase(),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 14.dp)
        ) {
            Text(
                text = artist.name,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${artist.songCount} songs · ${artist.albumCount} albums",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }
        Icon(
            Icons.Rounded.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
            modifier = Modifier.size(20.dp)
        )
    }
}

// ═══════════════════════════════════════════════════════════════
// 3. All Albums (Grid)
// ═══════════════════════════════════════════════════════════════

@Composable
fun AllAlbumsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToAlbumDetail: (Long, String) -> Unit = { _, _ -> },
    viewModel: LibraryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var searchQuery by rememberSaveable { mutableStateOf("") }
    val albums = remember(uiState.albums, searchQuery) {
        uiState.albums.filter { it.name.contains(searchQuery, ignoreCase = true) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        SeeAllTopBar(
            title = "All Albums",
            subtitle = "${albums.size} albums",
            onBack = onNavigateBack
        )

        SectionSearchBar(
            query = searchQuery,
            placeholder = "Search albums",
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            onQueryChange = { searchQuery = it }
        )

        if (albums.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    if (searchQuery.isBlank()) "No albums found." else "No matching albums found.",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 110.dp),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 8.dp,
                    bottom = 144.dp
                ),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(albums, key = { it.id }) { album ->
                    AlbumGridCard(
                        album = album,
                        onClick = {
                            onNavigateToAlbumDetail(album.id, album.name)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun AlbumGridCard(
    album: Album,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .bounceClick(onClick = onClick)
    ) {
        com.vyn.player.ui.components.MuzicImage(
            model = album.albumArtUri,
            contentDescription = album.name,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f),
            cornerRadius = 14.dp,
            iconSize = 28.dp,
            fallbackText = album.name,
            albumName = album.name,
            artistName = album.artist
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = album.name,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = album.artist,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            fontSize = 10.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

// ═══════════════════════════════════════════════════════════════
// 4. All Folders
// ═══════════════════════════════════════════════════════════════

@Composable
fun AllFoldersScreen(
    onNavigateBack: () -> Unit,
    onNavigateToFolderDetail: (String) -> Unit = {},
    viewModel: LibraryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var searchQuery by rememberSaveable { mutableStateOf("") }
    val folders = remember(uiState.folders, searchQuery) {
        uiState.folders.filter { it.name.contains(searchQuery, ignoreCase = true) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        SeeAllTopBar(
            title = "All Folders",
            subtitle = "${folders.size} folders",
            onBack = onNavigateBack
        )

        SectionSearchBar(
            query = searchQuery,
            placeholder = "Search folders",
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            onQueryChange = { searchQuery = it }
        )

        if (folders.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    if (searchQuery.isBlank()) "No folders found." else "No matching folders found.",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        } else {
            LazyColumn(contentPadding = PaddingValues(bottom = 144.dp)) {
                items(folders, key = { it.path }) { folder ->
                    FolderListItem(
                        folder = folder,
                        onClick = {
                            onNavigateToFolderDetail(folder.path)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun FolderListItem(
    folder: Folder,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .bounceClick(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(48.dp),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Rounded.Folder,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 14.dp)
        ) {
            Text(
                text = folder.name,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${folder.songCount} songs",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }
        Icon(
            Icons.Rounded.PlayArrow,
            contentDescription = "Play",
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
            modifier = Modifier.size(22.dp)
        )
    }
}
