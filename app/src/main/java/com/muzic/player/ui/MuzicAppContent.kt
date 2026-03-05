package com.muzic.player.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.zIndex
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeChild
import dev.chrisbanes.haze.HazeStyle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.muzic.player.ui.components.MiniPlayer
import com.muzic.player.ui.navigation.BottomNavScreens
import com.muzic.player.ui.navigation.MuzicNavGraph
import com.muzic.player.ui.navigation.Screen
import com.muzic.player.ui.theme.*

@Composable
fun MuzicAppContent(
    viewModel: MainViewModel = hiltViewModel()
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val hiddenRoutes = setOf(Screen.Splash.route, Screen.NowPlaying.route)
    val showBottomNav = currentDestination?.route != null && currentDestination.route !in hiddenRoutes
    val playbackState by viewModel.playbackState.collectAsStateWithLifecycle()
    val hazeState = remember { HazeState() }

    Scaffold(
        containerColor = Color.Transparent
    ) { paddingValues ->
        // Outer box spans the entire screen (edge to edge)
        Box(modifier = Modifier.fillMaxSize()) {
            
            // Content box (handles top/bottom status bars)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .zIndex(0f)
                    .haze(state = hazeState)
            ) {
                MuzicNavGraph(navController = navController)
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
                    // ─── MINI PLAYER ───
                    AnimatedVisibility(
                        visible = playbackState.currentSong != null,
                        enter = slideInVertically(initialOffsetY = { it }, animationSpec = tween(400)) + fadeIn(animationSpec = tween(400)),
                        exit = slideOutVertically(targetOffsetY = { it }, animationSpec = tween(400)) + fadeOut(animationSpec = tween(400))
                    ) {
                        MiniPlayer(
                            currentSong = playbackState.currentSong,
                            isPlaying = playbackState.isPlaying,
                            progress = if (playbackState.duration > 0)
                                playbackState.currentPosition.toFloat() / playbackState.duration.toFloat()
                            else 0f,
                            onPlayerClick = { navController.navigate(Screen.NowPlaying.route) },
                            onPlayPauseClick = { viewModel.togglePlayPause() },
                            onNextClick = { viewModel.skipToNext() },
                            onDismiss = { viewModel.stopPlayback() },
                            modifier = Modifier
                                .padding(horizontal = 16.dp, vertical = 4.dp)
                                .zIndex(3f),
                            hazeState = hazeState
                        )
                    }

                    // ─── FLOATING DOCK ───
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .fillMaxWidth()
                            .height(64.dp)
                            .zIndex(2f)
                            .clip(RoundedCornerShape(26.dp))
                            .hazeChild(state = hazeState, shape = RoundedCornerShape(26.dp), style = HazeStyle(blurRadius = 22.dp))
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f), RoundedCornerShape(26.dp))
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f), RoundedCornerShape(26.dp))
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
                                            navController.navigate(screen.route) {
                                                popUpTo(navController.graph.findStartDestination().id) {
                                                    saveState = true
                                                }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        },
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = screen.icon!!,
                                        contentDescription = screen.title,
                                        tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier
                                            .size(22.dp)
                                            .scale(scale)
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = screen.title!!.uppercase(),
                                        fontSize = 9.sp,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                        letterSpacing = 0.3.sp,
                                        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
