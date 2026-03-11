package com.muzic.player.ui.screens.library

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.muzic.player.ui.screens.library.tabs.*
import com.muzic.player.ui.theme.*
import com.muzic.player.util.PermissionHelper
import kotlinx.coroutines.launch
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun LibraryScreen(
    onNavigateToArtist: (String) -> Unit = {},
    viewModel: LibraryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val playbackState by viewModel.playbackState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var hasPermission by remember {
        mutableStateOf(PermissionHelper.hasMediaPermission(context))
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasPermission = permissions.values.all { it }
        if (hasPermission) {
            viewModel.refreshLibrary()
        }
    }

    LaunchedEffect(Unit) {
        if (!hasPermission) {
            permissionLauncher.launch(PermissionHelper.getRequiredPermissions())
        }
    }

    val tabs = listOf("Songs", "Albums", "Artists", "Playlists", "Folders")
    val pagerState = rememberPagerState(pageCount = { tabs.size })
    val coroutineScope = rememberCoroutineScope()
    
    // States for each tab to enable scroll-to-top
    val songsListState = rememberLazyListState()
    val albumsGridState = rememberLazyGridState()
    val artistsGridState = rememberLazyGridState()
    val playlistsListState = rememberLazyListState()
    val foldersListState = rememberLazyListState()

    val appViewModel: com.muzic.player.ui.MainViewModel = hiltViewModel()

    LaunchedEffect(Unit) {
        appViewModel.scrollToTopRequest.collect { route ->
            if (route == com.muzic.player.ui.navigation.Screen.Library.route) {
                when (pagerState.currentPage) {
                    0 -> songsListState.animateScrollToItem(0)
                    1 -> albumsGridState.animateScrollToItem(0)
                    2 -> artistsGridState.animateScrollToItem(0)
                    3 -> playlistsListState.animateScrollToItem(0)
                    4 -> foldersListState.animateScrollToItem(0)
                }
            }
        }
    }

    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }

    val adaptivePadding = getAdaptivePadding()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            if (hasPermission) {
                TopAppBar(
                    title = {
                        Text(
                            text = "Muzic",
                            fontWeight = FontWeight.Bold,
                            fontSize = 28.sp,
                            color = MaterialTheme.colorScheme.onBackground,
                            letterSpacing = 1.sp
                        )
                    },
                    actions = {
                        IconButton(onClick = { viewModel.refreshLibrary() }) {
                            Icon(
                                imageVector = Icons.Rounded.Refresh,
                                contentDescription = "Refresh",
                                tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        scrolledContainerColor = MaterialTheme.colorScheme.background
                    )
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (!hasPermission) {
                // Permission screen - Responsive
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = adaptivePadding * 2)
                    ) {
                        Box(
                            modifier = Modifier
                                .size((200.dp.value * 0.4f).coerceIn(64f, 100f).toFloat().dp)
                                .clip(RoundedCornerShape(24.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primaryContainer)
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.FolderOpen,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(40.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = "Permission Required",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onBackground,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Muzic needs access to your music files to play them.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(32.dp))
                        Button(
                            onClick = { permissionLauncher.launch(PermissionHelper.getRequiredPermissions()) }
                        ) {
                            Text("Grant Access")
                        }
                    }
                }
            } else {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    // Tab Row - Responsive padding
                    ScrollableTabRow(
                        selectedTabIndex = pagerState.currentPage,
                        containerColor = Color.Transparent,
                        contentColor = MaterialTheme.colorScheme.primary,
                        edgePadding = adaptivePadding,
                        indicator = { tabPositions ->
                            if (pagerState.currentPage < tabPositions.size) {
                                TabRowDefaults.SecondaryIndicator(
                                    modifier = Modifier
                                        .tabIndicatorOffset(tabPositions[pagerState.currentPage]),
                                    color = MaterialTheme.colorScheme.primary,
                                    height = 3.dp
                                )
                            }
                        },
                        divider = {}
                    ) {
                        tabs.forEachIndexed { index, title ->
                            Tab(
                                selected = pagerState.currentPage == index,
                                onClick = {
                                    coroutineScope.launch {
                                        pagerState.animateScrollToPage(index)
                                    }
                                },
                                text = {
                                    Text(
                                        text = title,
                                        fontWeight = if (pagerState.currentPage == index) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 14.sp
                                    )
                                },
                                selectedContentColor = MaterialTheme.colorScheme.primary,
                                unselectedContentColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                            )
                        }
                    }

                    // Content
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.weight(1f)
                    ) { page ->
                        when (page) {
                            0 -> SongsTab(
                                songs = uiState.songs,
                                isLoading = uiState.isLoading,
                                currentSongId = playbackState.currentSong?.id,
                                onSongClick = { song -> viewModel.playSong(song, uiState.songs) },
                                onFavoriteClick = { songId -> viewModel.toggleFavorite(songId) },
                                listState = songsListState
                            )
                            1 -> AlbumsTab(
                                albums = uiState.albums,
                                isLoading = uiState.isLoading,
                                onAlbumClick = { },
                                gridState = albumsGridState
                            )
                            2 -> ArtistsTab(
                                artists = uiState.artists,
                                isLoading = uiState.isLoading,
                                onArtistClick = { artist -> onNavigateToArtist(artist.name) },
                                gridState = artistsGridState
                            )
                            3 -> PlaylistsTab(
                                playlists = uiState.playlists,
                                isLoading = uiState.isLoading,
                                onPlaylistClick = { },
                                onCreatePlaylistClick = { showCreatePlaylistDialog = true },
                                onDeletePlaylistClick = { viewModel.deletePlaylist(it) },
                                listState = playlistsListState
                            )
                            4 -> FoldersTab(
                                folders = uiState.folders,
                                isLoading = uiState.isLoading,
                                onFolderClick = { },
                                listState = foldersListState
                            )
                        }
                    }
                }
            }
        }
    }

    // Create playlist dialog
    if (showCreatePlaylistDialog) {
        AlertDialog(
            onDismissRequest = { showCreatePlaylistDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(20.dp),
            title = {
                Text("New Playlist", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
            },
            text = {
                OutlinedTextField(
                    value = newPlaylistName,
                    onValueChange = { newPlaylistName = it },
                    label = { Text("Playlist name") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        cursorColor = MaterialTheme.colorScheme.primary,
                        focusedLabelColor = MaterialTheme.colorScheme.primary
                    )
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newPlaylistName.isNotBlank()) {
                            viewModel.createPlaylist(newPlaylistName)
                            newPlaylistName = ""
                            showCreatePlaylistDialog = false
                        }
                    }
                ) {
                    Text("Create", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreatePlaylistDialog = false }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                }
            }
        )
    }
}
