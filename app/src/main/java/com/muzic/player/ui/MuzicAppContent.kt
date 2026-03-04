package com.muzic.player.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
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

    // Check if bottom nav should be shown
    val showBottomNav = currentDestination?.route in BottomNavScreens.map { it.route }

    val playbackState by viewModel.playbackState.collectAsStateWithLifecycle()

    Scaffold(
        bottomBar = {
            AnimatedVisibility(
                visible = showBottomNav,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it })
            ) {
                Surface(
                    color = DarkSurface,
                    shadowElevation = 16.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    NavigationBar(
                        containerColor = DarkSurface,
                        contentColor = TextSecondary,
                        modifier = Modifier
                            .height(80.dp)
                            .padding(top = 4.dp), // Apple-style padding
                        tonalElevation = 0.dp
                    ) {
                        BottomNavScreens.forEach { screen ->
                            val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                            val scale by animateFloatAsState(targetValue = if (selected) 1.2f else 1.0f, label = "scaleAnim")
                            
                            NavigationBarItem(
                                icon = {
                                    Icon(
                                        imageVector = screen.icon!!,
                                        contentDescription = screen.title,
                                        modifier = Modifier.size(26.dp).scale(scale)
                                    )
                                },
                                label = {
                                    Text(
                                        text = screen.title!!,
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                },
                                selected = selected,
                                onClick = {
                                    navController.navigate(screen.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MuzicRed,
                                    selectedTextColor = MuzicRed,
                                    unselectedIconColor = TextSecondary,
                                    unselectedTextColor = TextSecondary,
                                    indicatorColor = Color.Transparent
                                )
                            )
                        }
                    }
                }
            }
        },
        containerColor = DarkBg
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            MuzicNavGraph(navController = navController)
            
            // Floating MiniPlayer above Bottom Navigation
            AnimatedVisibility(
                visible = showBottomNav && playbackState.currentSong != null,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it }),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 8.dp) // padding between bottom nav and mini player
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
                    onDismiss = { viewModel.stopPlayback() }
                )
            }
        }
    }
}
