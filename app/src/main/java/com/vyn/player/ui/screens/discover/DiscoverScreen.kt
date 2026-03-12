package com.vyn.player.ui.screens.discover

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.toArgb
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vyn.player.data.model.Song
import com.vyn.player.data.model.Album
import com.vyn.player.data.model.Artist
import com.vyn.player.data.model.Folder
import com.vyn.player.ui.screens.library.LibraryViewModel
import com.vyn.player.ui.components.bounceClick
import kotlin.math.absoluteValue

@Composable
fun DiscoverScreen(
    viewModel: LibraryViewModel = hiltViewModel(),
    appViewModel: com.vyn.player.ui.MainViewModel = hiltViewModel(),
    onNavigateToArtist: (String) -> Unit = {},
    onNavigateToAlbumDetail: (Long, String) -> Unit = { _, _ -> },
    onNavigateToFolderDetail: (String) -> Unit = {},
    onNavigateToAllRecentlyAdded: () -> Unit = {},
    onNavigateToAllArtists: () -> Unit = {},
    onNavigateToAllAlbums: () -> Unit = {},
    onNavigateToAllFolders: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val playbackState by viewModel.playbackState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    // ─── TAP TO TOP LISTENER ───
    LaunchedEffect(Unit) {
        appViewModel.scrollToTopRequest.collect { route ->
            if (route == com.vyn.player.ui.navigation.Screen.Discover.route) {
                listState.animateScrollToItem(0)
            }
        }
    }

    // ─── SCROLL PERFORMANCE PROTECTION ───
    com.vyn.player.ui.components.ObserveScrollState(listState)

    val songs = uiState.songs
    val artists = uiState.artists
    val albums = uiState.albums
    val folders = uiState.folders

    val recentlyAdded = remember(songs) {
        songs.sortedByDescending { it.dateAdded }.take(15)
    }

    // ─── Mood-based quick play groups ───
    val moodGroups = remember(songs) { buildMoodGroups(songs) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding(),
        state = listState,
        contentPadding = PaddingValues(bottom = 144.dp)
    ) {
        // ─── Title ───
        item {
            Text(
                text = "Discover",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = 16.dp, top = 28.dp, bottom = 20.dp)
            )
        }

        // ─── Smart Mix Section ───
        if (uiState.smartMixSongs.isNotEmpty()) {
            item {
                AnimatedItem {
                    SmartMixCard(
                        onPlayClick = { viewModel.playSmartMix() },
                        onRefreshClick = { viewModel.refreshSmartMix(true) }
                    )
                }
            }
        }

        if (songs.isNotEmpty()) {
            // ─── 1. Recently Added ───
            if (recentlyAdded.isNotEmpty()) {
                item {
                    DiscoverSongSection(
                        title = "Recently Added",
                        songs = recentlyAdded.take(15),
                        isPlayingSong = { id -> playbackState.currentSong?.id == id },
                        onSongClick = { song -> viewModel.playSong(song, recentlyAdded) },
                        isScrolling = listState.isScrollInProgress,
                        onSeeAll = onNavigateToAllRecentlyAdded
                    )
                }
            }

            // ─── 2. Artists ───
            if (artists.isNotEmpty()) {
                item {
                    DiscoverArtistSection(
                        title = "Artists",
                        artists = artists.take(10),
                        songs = songs,
                        onArtistClick = { artist -> onNavigateToArtist(artist.name) },
                        onSeeAll = onNavigateToAllArtists
                    )
                }
            }

            // ─── 3. Albums ───
            if (albums.isNotEmpty()) {
                item {
                    DiscoverAlbumSection(
                        title = "Albums",
                        albums = albums.take(10),
                        onAlbumClick = { album ->
                            onNavigateToAlbumDetail(album.id, album.name)
                        },
                        isScrolling = listState.isScrollInProgress,
                        onSeeAll = onNavigateToAllAlbums
                    )
                }
            }

            // ─── 4. Moods / Quick Play ───
            if (moodGroups.isNotEmpty()) {
                item {
                    DiscoverMoodSection(
                        title = "Moods & Quick Play",
                        moods = moodGroups,
                        onMoodClick = { moodSongs ->
                            if (moodSongs.isNotEmpty()) {
                                viewModel.playSong(moodSongs.first(), moodSongs.shuffled())
                            }
                        }
                    )
                }
            }

            // ─── 5. Folder Explorer ───
            if (folders.isNotEmpty()) {
                item {
                    DiscoverFolderSection(
                        title = "Folders",
                        folders = folders.take(5),
                        onFolderClick = { folder ->
                            onNavigateToFolderDetail(folder.path)
                        },
                        onSeeAll = onNavigateToAllFolders
                    )
                }
            }
        } else {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No music found to discover.\nAdd some local files!",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        textAlign = TextAlign.Center,
                        lineHeight = 22.sp
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// Mood Group Builder
// ═══════════════════════════════════════════════════════════════

data class MoodGroup(
    val name: String,
    val icon: ImageVector,
    val color1: Color,
    val color2: Color,
    val songs: List<Song>
)

private fun buildMoodGroups(songs: List<Song>): List<MoodGroup> {
    if (songs.isEmpty()) return emptyList()

    val romantic = mutableListOf<Song>()
    val sad = mutableListOf<Song>()
    val workout = mutableListOf<Song>()
    val oldClassics = mutableListOf<Song>()

    val romanticWords = listOf("love", "heart", "kiss", "baby", "darling", "romantic", "ishq", "pyar", "mohabbat", "dil", "tere", "tera")
    val sadWords = listOf("sad", "cry", "tear", "alone", "broken", "pain", "miss", "lost", "dard", "tanha", "judai", "alvida")
    val workoutWords = listOf("fire", "run", "power", "energy", "fight", "strong", "beat", "pump", "rock", "rage", "hustle")

    for (song in songs) {
        val lower = (song.title + " " + song.album).lowercase()
        when {
            romanticWords.any { lower.contains(it) } -> romantic.add(song)
            sadWords.any { lower.contains(it) } -> sad.add(song)
            workoutWords.any { lower.contains(it) } -> workout.add(song)
        }
        if (song.year in 1960..2005) oldClassics.add(song)
    }

    val groups = mutableListOf<MoodGroup>()

    if (romantic.size >= 3) groups.add(
        MoodGroup("Romantic", Icons.Rounded.Favorite,
            Color(0xFFE91E63), Color(0xFFAD1457), romantic.take(30))
    )
    if (sad.size >= 3) groups.add(
        MoodGroup("Sad", Icons.Rounded.WaterDrop,
            Color(0xFF5C6BC0), Color(0xFF283593), sad.take(30))
    )
    if (workout.size >= 3) groups.add(
        MoodGroup("Workout", Icons.Rounded.FitnessCenter,
            Color(0xFFFF7043), Color(0xFFBF360C), workout.take(30))
    )
    if (oldClassics.size >= 3) groups.add(
        MoodGroup("Old Classics", Icons.Rounded.MusicNote,
            Color(0xFF8D6E63), Color(0xFF4E342E), oldClassics.shuffled().take(30))
    )

    // Add a shuffle-all mood always
    groups.add(
        MoodGroup("Shuffle All", Icons.Rounded.Shuffle,
            Color(0xFF26A69A), Color(0xFF00695C), songs.shuffled().take(50))
    )

    return groups
}

// ═══════════════════════════════════════════════════════════════
// Section: Songs (Recently Added)
// ═══════════════════════════════════════════════════════════════

@Composable
private fun DiscoverSongSection(
    title: String,
    songs: List<Song>,
    isPlayingSong: (Long) -> Boolean,
    onSongClick: (Song) -> Unit,
    isScrolling: Boolean = false,
    onSeeAll: () -> Unit = {}
) {
    Column(modifier = Modifier.padding(bottom = 24.dp)) {
        SectionHeader(title, onSeeAll = onSeeAll)
        
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(songs, key = { it.id }) { song ->
                AnimatedItem {
                    SongCard(
                        song = song,
                        isPlaying = isPlayingSong(song.id),
                        cardWidth = 120.dp,
                        isScrolling = isScrolling,
                        onClick = { onSongClick(song) }
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// Section: Artists
// ═══════════════════════════════════════════════════════════════

@Composable
private fun DiscoverArtistSection(
    title: String,
    artists: List<Artist>,
    songs: List<Song>,
    onArtistClick: (Artist) -> Unit,
    onSeeAll: () -> Unit = {}
) {
    Column(modifier = Modifier.padding(bottom = 24.dp)) {
        SectionHeader(title, onSeeAll = onSeeAll)

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(artists, key = { it.id }) { artist ->
                AnimatedItem {
                    ArtistCard(
                        artist = artist,
                        songs = songs,
                        onClick = { onArtistClick(artist) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ArtistCard(
    artist: Artist,
    songs: List<Song>,
    onClick: () -> Unit
) {
    val artistSong = remember(artist.name) {
        songs.firstOrNull { it.artist.equals(artist.name, ignoreCase = true) }
    }

    Column(
        modifier = Modifier
            .width(90.dp)
            .bounceClick(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Circular avatar with first letter
        Box(
            modifier = Modifier.size(80.dp),
            contentAlignment = Alignment.Center
        ) {
            if (artistSong != null) {
                com.vyn.player.ui.components.MuzicImage(
                    model = artistSong.albumArtUri,
                    contentDescription = artist.name,
                    modifier = Modifier.fillMaxSize(),
                    cornerRadius = 40.dp,
                    iconSize = 24.dp,
                    fallbackText = artist.name,
                    albumName = artistSong.album,
                    artistName = artist.name
                )
            } else {
                val hash = artist.name.hashCode()
                val hue = (hash % 360).toFloat().absoluteValue
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    shape = CircleShape,
                    color = Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, 0.5f, 0.5f)))
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = artist.name.take(1).uppercase(),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = artist.name,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = "${artist.songCount} songs",
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
            fontSize = 10.sp,
            maxLines = 1,
            textAlign = TextAlign.Center
        )
    }
}

// ═══════════════════════════════════════════════════════════════
// Section: Albums
// ═══════════════════════════════════════════════════════════════

@Composable
private fun DiscoverAlbumSection(
    title: String,
    albums: List<Album>,
    onAlbumClick: (Album) -> Unit,
    isScrolling: Boolean = false,
    onSeeAll: () -> Unit = {}
) {
    Column(modifier = Modifier.padding(bottom = 24.dp)) {
        SectionHeader(title, onSeeAll = onSeeAll)

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(albums, key = { it.id }) { album ->
                AnimatedItem {
                    AlbumCard(
                        album = album,
                        cardWidth = 120.dp,
                        isScrolling = isScrolling,
                        onClick = { onAlbumClick(album) }
                    )
                }
            }
        }
    }
}

@Composable
private fun AlbumCard(
    album: Album,
    cardWidth: Dp,
    onClick: () -> Unit,
    isScrolling: Boolean = false
) {
    Column(
        modifier = Modifier
            .width(cardWidth)
            .bounceClick(onClick = onClick)
    ) {
        com.vyn.player.ui.components.MuzicImage(
            model = album.albumArtUri,
            contentDescription = album.name,
            modifier = Modifier.size(cardWidth),
            cornerRadius = 14.dp,
            iconSize = 28.dp,
            fallbackText = album.name,
            albumName = album.name,
            artistName = album.artist,
            isScrolling = isScrolling,
            thumbnailMode = true
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = album.name,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 15.sp,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = album.artist,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            fontSize = 10.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 13.sp,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

// ═══════════════════════════════════════════════════════════════
// Section: Moods & Quick Play
// ═══════════════════════════════════════════════════════════════

@Composable
private fun DiscoverMoodSection(
    title: String,
    moods: List<MoodGroup>,
    onMoodClick: (List<Song>) -> Unit
) {
    Column(modifier = Modifier.padding(bottom = 24.dp)) {
        SectionHeader(title)

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(moods, key = { it.name }) { mood ->
                AnimatedItem {
                    MoodCard(
                        mood = mood,
                        onClick = { onMoodClick(mood.songs) }
                    )
                }
            }
        }
    }
}

@Composable
private fun MoodCard(
    mood: MoodGroup,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .width(140.dp)
            .height(80.dp)
            .bounceClick(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(listOf(mood.color1, mood.color2)),
                    RoundedCornerShape(16.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(horizontal = 12.dp)
            ) {
                Icon(
                    imageVector = mood.icon,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = mood.name,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    maxLines = 1
                )
            }
            // Song count badge
            Text(
                text = "${mood.songs.size} songs",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 9.sp,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// Section: Folder Explorer
// ═══════════════════════════════════════════════════════════════

@Composable
private fun DiscoverFolderSection(
    title: String,
    folders: List<Folder>,
    onFolderClick: (Folder) -> Unit,
    onSeeAll: () -> Unit = {}
) {
    Column(modifier = Modifier.padding(bottom = 24.dp)) {
        SectionHeader(title, onSeeAll = onSeeAll)

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(folders, key = { it.path }) { folder ->
                AnimatedItem {
                    FolderCard(
                        folder = folder,
                        onClick = { onFolderClick(folder) }
                    )
                }
            }
        }
    }
}

@Composable
private fun FolderCard(
    folder: Folder,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .width(140.dp)
            .bounceClick(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                imageVector = Icons.Rounded.Folder,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = folder.name,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${folder.songCount} songs",
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                fontSize = 10.sp,
                maxLines = 1
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// Shared Components
// ═══════════════════════════════════════════════════════════════

@Composable
private fun SongCard(
    song: Song,
    isPlaying: Boolean,
    cardWidth: Dp,
    onClick: () -> Unit,
    isScrolling: Boolean = false
) {
    Column(
        modifier = Modifier
            .width(cardWidth)
            .bounceClick(onClick = onClick)
    ) {
        Box(
            modifier = Modifier.size(cardWidth),
            contentAlignment = Alignment.Center
        ) {
            com.vyn.player.ui.components.MuzicImage(
                model = song.albumArtUri,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                cornerRadius = 14.dp,
                iconSize = 28.dp,
                fallbackText = song.title,
                albumName = song.album,
                artistName = song.artist,
                isScrolling = isScrolling,
                thumbnailMode = true
            )

            if (isPlaying) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.background.copy(alpha = 0.6f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Equalizer,
                        contentDescription = "Playing",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = song.title,
            color = if (isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 15.sp,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = song.artist,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            fontSize = 10.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 13.sp,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun SectionHeader(title: String, onSeeAll: (() -> Unit)? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            letterSpacing = (-0.3).sp
        )
        if (onSeeAll != null) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .clickable(onClick = onSeeAll)
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "See All",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.width(2.dp))
                Icon(
                    imageVector = Icons.Rounded.ArrowForward,
                    contentDescription = "See All",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun AnimatedItem(content: @Composable () -> Unit) {
    var appeared by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { appeared = true }
    val alpha by animateFloatAsState(
        targetValue = if (appeared) 1f else 0f,
        animationSpec = tween(300),
        label = "itemAlpha"
    )
    val scale by animateFloatAsState(
        targetValue = if (appeared) 1f else 0.92f,
        animationSpec = tween(300),
        label = "itemScale"
    )

    Box(
        modifier = Modifier.graphicsLayer {
            this.alpha = alpha
            scaleX = scale
            scaleY = scale
        }
    ) {
        content()
    }
}

@Composable
private fun SmartMixCard(
    onPlayClick: () -> Unit,
    onRefreshClick: () -> Unit
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .height(110.dp)
            .bounceClick { onPlayClick() },
        shape = RoundedCornerShape(24.dp),
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            primaryColor,
                            primaryColor.copy(alpha = 0.7f),
                            primaryColor.copy(alpha = 0.4f)
                        )
                    )
                )
        ) {
            // Background Decoration
            Icon(
                imageVector = Icons.Rounded.AutoAwesome,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.15f),
                modifier = Modifier
                    .size(120.dp)
                    .align(Alignment.CenterEnd)
                    .offset(x = 30.dp, y = 20.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AutoAwesome,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Smart Mix",
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "AI-powered personalized queue",
                        color = Color.White.copy(alpha = 0.8f),
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                IconButton(
                    onClick = {
                        onRefreshClick()
                    },
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f))
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = "Refresh",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
