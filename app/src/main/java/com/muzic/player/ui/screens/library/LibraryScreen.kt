package com.muzic.player.ui.screens.library

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.muzic.player.ui.components.MiniPlayer
import com.muzic.player.ui.screens.library.tabs.*
import com.muzic.player.ui.theme.*
import com.muzic.player.util.PermissionHelper
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun LibraryScreen(
    onNavigateToNowPlaying: () -> Unit,
    onNavigateToSettings: () -> Unit,
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

    // Create playlist dialog
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Muzic",
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp,
                        color = SoftWhite,
                        letterSpacing = 2.sp
                    )
                },
                actions = {
                    IconButton(onClick = {
                        viewModel.refreshLibrary()
                    }) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = "Refresh library",
                            tint = TextSecondary
                        )
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            imageVector = Icons.Rounded.Settings,
                            contentDescription = "Settings",
                            tint = TextSecondary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkCharcoal
                )
            )
        },
        containerColor = DarkCharcoal
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (!hasPermission) {
                // Permission required screen
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.FolderOpen,
                            contentDescription = null,
                            tint = ElectricPurple,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Permission Required",
                            style = MaterialTheme.typography.headlineSmall,
                            color = SoftWhite
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Muzic needs access to your music files to play them.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = {
                                permissionLauncher.launch(PermissionHelper.getRequiredPermissions())
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ElectricPurple
                            )
                        ) {
                            Text("Grant Permission")
                        }
                    }
                }
            } else {
                // Tab Row
                ScrollableTabRow(
                    selectedTabIndex = pagerState.currentPage,
                    containerColor = DarkCharcoal,
                    contentColor = SoftWhite,
                    edgePadding = 16.dp,
                    indicator = { tabPositions ->
                        if (pagerState.currentPage < tabPositions.size) {
                            TabRowDefaults.SecondaryIndicator(
                                modifier = Modifier.fillMaxWidth(),
                                color = ElectricPurple,
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
                                    color = if (pagerState.currentPage == index) ElectricPurple else TextSecondary
                                )
                            }
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
                            onSongClick = { song -> viewModel.playSong(song) },
                            onFavoriteClick = { songId -> viewModel.toggleFavorite(songId) }
                        )
                        1 -> AlbumsTab(
                            albums = uiState.albums,
                            isLoading = uiState.isLoading,
                            onAlbumClick = { /* Navigate to album detail */ }
                        )
                        2 -> ArtistsTab(
                            artists = uiState.artists,
                            isLoading = uiState.isLoading,
                            onArtistClick = { /* Navigate to artist detail */ }
                        )
                        3 -> PlaylistsTab(
                            playlists = uiState.playlists,
                            isLoading = uiState.isLoading,
                            onPlaylistClick = { /* Navigate to playlist detail */ },
                            onCreatePlaylistClick = { showCreatePlaylistDialog = true },
                            onDeletePlaylistClick = { playlistId -> viewModel.deletePlaylist(playlistId) }
                        )
                        4 -> FoldersTab(
                            folders = uiState.folders,
                            isLoading = uiState.isLoading,
                            onFolderClick = { /* Navigate to folder detail */ }
                        )
                    }
                }
            }

            // Mini Player
            MiniPlayer(
                currentSong = playbackState.currentSong,
                isPlaying = playbackState.isPlaying,
                progress = if (playbackState.duration > 0)
                    playbackState.currentPosition.toFloat() / playbackState.duration.toFloat()
                else 0f,
                onPlayerClick = onNavigateToNowPlaying,
                onPlayPauseClick = { viewModel.togglePlayPause() },
                onNextClick = { viewModel.skipToNext() }
            )
        }
    }

    // Create playlist dialog
    if (showCreatePlaylistDialog) {
        AlertDialog(
            onDismissRequest = { showCreatePlaylistDialog = false },
            title = {
                Text("New Playlist", color = SoftWhite)
            },
            text = {
                OutlinedTextField(
                    value = newPlaylistName,
                    onValueChange = { newPlaylistName = it },
                    label = { Text("Playlist name") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ElectricPurple,
                        cursorColor = ElectricPurple,
                        focusedLabelColor = ElectricPurple
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
                    Text("Create", color = ElectricPurple)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showCreatePlaylistDialog = false }
                ) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = DarkSurface
        )
    }
}
