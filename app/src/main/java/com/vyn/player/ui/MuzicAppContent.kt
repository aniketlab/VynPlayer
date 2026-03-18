package com.vyn.player.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.zIndex
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.vyn.player.ui.components.MiniPlayer
import com.vyn.player.ui.components.frostedGlassBar
import com.vyn.player.ui.components.frostedSurfaceCard
import com.vyn.player.ui.navigation.BottomNavScreens
import com.vyn.player.ui.navigation.MuzicNavGraph
import com.vyn.player.ui.navigation.Screen
import com.vyn.player.ui.actions.LocalSongActionDispatcher
import com.vyn.player.ui.screens.nowplaying.NowPlayingContent
import com.vyn.player.ui.screens.nowplaying.QueueScreen
import com.vyn.player.ui.screens.nowplaying.SongDetailsDialog
import com.vyn.player.ui.theme.*
import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext

@Composable
fun MuzicAppContent(
    viewModel: MainViewModel = hiltViewModel()
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val hiddenRoutes = setOf(Screen.Splash.route)
    val showBottomNav = currentDestination?.route != null && currentDestination.route !in hiddenRoutes
    val playbackProgress by viewModel.playbackProgress.collectAsStateWithLifecycle()
    val playbackState by viewModel.playbackState.collectAsStateWithLifecycle()
    val selectedSong by viewModel.selectedSong.collectAsStateWithLifecycle()
    val showSongDetails by viewModel.showSongDetails.collectAsStateWithLifecycle()
    val isQueueScreenVisible by viewModel.isQueueScreenVisible.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = context as? Activity

    val isPlayerExpanded by viewModel.isPlayerExpanded.collectAsStateWithLifecycle()
    val expansionProgress by animateFloatAsState(
        targetValue = if (isPlayerExpanded) 1f else 0f,
        animationSpec = tween(
            durationMillis = 220,
            easing = FastOutSlowInEasing
        ),
        label = "ExpansionProgress"
    )

    val isMiniPlayerVisible by remember {
        derivedStateOf { playbackProgress.currentTrack != null }
    }

    CompositionLocalProvider(
        LocalSongActionDispatcher provides viewModel::handleSongAction
    ) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        // Outer box spans the entire screen (edge to edge)
        Box(
            modifier = Modifier
                .fillMaxSize()
        ) {
            
            // Content box (handles top/bottom status bars)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .zIndex(0f)
            ) {
                MuzicNavGraph(navController = navController)
                
                // ─── BOTTOM TABS BACK NAVIGATION ───
                // Composed AFTER NavGraph so it intercepts back presses for main tabs
                val currentRoute = currentDestination?.route
                val isBottomNavTab = BottomNavScreens.any { it.route == currentRoute }
                
                BackHandler(enabled = !isPlayerExpanded && isBottomNavTab) {
                    if (currentRoute == Screen.Home.route) {
                        activity?.moveTaskToBack(true)
                    } else {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                }
            }

            // Floating bottom section (Anchored to absolute screen bottom)
            if (showBottomNav) {
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(Color.Transparent)
                        // 1. apply gesture bar inset
                        .navigationBarsPadding()
                        // 2. apply small gap exactly above the gesture bar
                        .padding(bottom = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // ─── MINI PLAYER (Persistent) ───
                    AnimatedVisibility(
                        visible = isMiniPlayerVisible,
                        enter = slideInVertically(initialOffsetY = { it }, animationSpec = tween(200, easing = FastOutSlowInEasing)) + fadeIn(animationSpec = tween(200)),
                        exit = slideOutVertically(targetOffsetY = { it }, animationSpec = tween(200, easing = FastOutLinearInEasing)) + fadeOut(animationSpec = tween(200))
                    ) {
                        MiniPlayer(
                            currentSong = playbackProgress.currentTrack,
                            isPlaying = playbackProgress.isPlaying,
                            progress = if (playbackProgress.duration > 0L) {
                                (playbackProgress.currentPosition.toLong().toFloat() / playbackProgress.duration.toLong().toFloat()).coerceIn(0f, 1f)
                            } else 0f,
                            onPlayerClick = { 
                                if (playbackProgress.currentTrack != null) {
                                    viewModel.setPlayerExpanded(true)
                                }
                            },
                            onPlayPauseClick = { viewModel.togglePlayPause() },
                            onNextClick = { viewModel.skipToNext() },
                            onPreviousClick = { viewModel.skipToPrevious() },
                            modifier = Modifier
                                .padding(horizontal = 16.dp, vertical = 4.dp)
                                .graphicsLayer {
                                    alpha = 1f - expansionProgress
                                    translationY = 50f * expansionProgress
                                }
                                .zIndex(3f)
                        )
                    }

                    // ─── FLOATING DOCK ───
                    val dockShape = RoundedCornerShape(26.dp)
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .fillMaxWidth()
                            .height(64.dp)
                            .zIndex(2f)
                            .frostedGlassBar(
                                shape = dockShape,
                                lightColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.93f),
                                darkColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                                elevation = 8.dp,
                                borderWidth = 0.8.dp,
                                borderColor = Color.White.copy(alpha = 0.08f)
                            )
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            BottomNavScreens.forEach { screen ->
                                val selected = currentDestination?.hierarchy?.any {
                                    it.route == screen.route
                                } == true

                                val scale by animateFloatAsState(
                                    targetValue = if (selected) 1.08f else 1.0f,
                                    animationSpec = tween(200),
                                    label = "dockScale"
                                )

                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .clickable(
                                            indication = null,
                                            interactionSource = remember { MutableInteractionSource() }
                                        ) {
                                            if (selected) {
                                                // ─── TAP SAME TAB: Scroll to Top ───
                                                viewModel.requestScrollToTop(screen.route)
                                            } else {
                                                // ─── NAVIGATE TO DIFFERENT TAB ───
                                                navController.navigate(screen.route) {
                                                    popUpTo(navController.graph.startDestinationId) {
                                                        saveState = true
                                                    }
                                                    launchSingleTop = true
                                                    restoreState = true
                                                }
                                            }
                                        },
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .background(
                                                color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f) else Color.Transparent,
                                                shape = RoundedCornerShape(12.dp)
                                            )
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Icon(
                                            imageVector = screen.icon!!,
                                            contentDescription = screen.title,
                                            tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                                            modifier = Modifier
                                                .size(22.dp)
                                                .scale(scale)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = screen.title!!.uppercase(),
                                        fontSize = 9.sp,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                        letterSpacing = 0.3.sp,
                                        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ─── FULL PLAYER OVERLAY ───
            if (expansionProgress > 0.001f) {
                val currentIsFav by viewModel.isFavorite.collectAsStateWithLifecycle()

                // This BackHandler is composed AFTER NavHost, so it has
                // the HIGHEST priority and intercepts back before NavController.
                BackHandler(enabled = isPlayerExpanded) {
                    viewModel.setPlayerExpanded(false)
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .zIndex(10f)
                        .graphicsLayer {
                            translationY = size.height * (1f - expansionProgress)
                            alpha = expansionProgress.coerceIn(0f, 1f)
                        }
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    NowPlayingContent(
                        playbackState = playbackState,
                        currentPosition = playbackProgress.currentPosition,
                        isFavorite = currentIsFav,
                        onNavigateBack = { viewModel.setPlayerExpanded(false) },
                        onOpenQueue = { viewModel.setQueueScreenVisible(true) },
                        onTogglePlayPause = { viewModel.togglePlayPause() },
                        onSkipToNext = { viewModel.skipToNext() },
                        onSkipToPrevious = { viewModel.skipToPrevious() },
                        onSeek = { viewModel.seekTo(it) },
                        onToggleFavorite = { viewModel.toggleFavorite() },
                        onCycleRepeatMode = { viewModel.cycleRepeatMode() },
                        onToggleShuffle = { viewModel.toggleShuffle() },
                        expansionProgress = expansionProgress
                    )
                }
            }

            if (isPlayerExpanded && isQueueScreenVisible) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .zIndex(11f)
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    BackHandler(enabled = true) {
                        viewModel.setQueueScreenVisible(false)
                    }

                    QueueScreen(
                        playbackState = playbackState,
                        onNavigateBack = { viewModel.setQueueScreenVisible(false) }
                    )
                }
            }

            if (showSongDetails && selectedSong != null) {
                SongDetailsDialog(
                    song = selectedSong!!,
                    onDismiss = viewModel::dismissSongDetails
                )
            }
        }
    }
    }
}
