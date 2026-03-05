package com.muzic.player.ui.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.muzic.player.ui.screens.home.HomeScreen
import com.muzic.player.ui.screens.library.LibraryScreen
import com.muzic.player.ui.screens.nowplaying.NowPlayingScreen
import com.muzic.player.ui.screens.search.SearchScreen
import com.muzic.player.ui.screens.settings.SettingsScreen
import com.muzic.player.ui.screens.splash.SplashScreen
import com.muzic.player.ui.screens.discover.DiscoverScreen
import com.muzic.player.ui.screens.profile.ProfileScreen
import com.muzic.player.ui.screens.profile.FavoritesScreen
import com.muzic.player.ui.screens.profile.RecentlyPlayedScreen
import com.muzic.player.ui.screens.profile.PlaylistsScreen
import com.muzic.player.ui.screens.profile.PlaylistDetailScreen
import com.muzic.player.ui.screens.artist.ArtistDetailScreen
import androidx.navigation.NavType
import androidx.navigation.navArgument

@Composable
fun MuzicNavGraph(
    navController: NavHostController,
    startDestination: String = Screen.Splash.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        enterTransition = { fadeIn(animationSpec = tween(300)) },
        exitTransition = { fadeOut(animationSpec = tween(300)) },
        popEnterTransition = { fadeIn(animationSpec = tween(300)) },
        popExitTransition = { fadeOut(animationSpec = tween(300)) },
        modifier = Modifier.fillMaxSize()
    ) {
        composable(Screen.Splash.route) {
            SplashScreen(
                onNavigateToLibrary = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToSearch = { navController.navigate(Screen.Search.route) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                onNavigateToArtist = { artistName: String ->
                    navController.navigate(Screen.ArtistDetail.createRoute(artistName))
                }
            )
        }

        composable(Screen.Discover.route) {
            DiscoverScreen()
        }

        composable(Screen.Profile.route) {
            ProfileScreen(
                onNavigateToFavorites = { navController.navigate(Screen.Favorites.route) },
                onNavigateToRecentlyPlayed = { navController.navigate(Screen.RecentlyPlayed.route) },
                onNavigateToPlaylists = { navController.navigate("playlists") }
            )
        }

        composable(Screen.Search.route) {
            SearchScreen()
        }

        composable(Screen.Library.route) {
            LibraryScreen(
                onNavigateToArtist = { artistName ->
                    navController.navigate(Screen.ArtistDetail.createRoute(artistName))
                }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.NowPlaying.route,
            enterTransition = {
                slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = tween(400)
                ) + fadeIn(animationSpec = tween(400))
            },
            exitTransition = {
                slideOutVertically(
                    targetOffsetY = { it },
                    animationSpec = tween(400)
                ) + fadeOut(animationSpec = tween(400))
            }
        ) {
            NowPlayingScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Favorites.route) {
            FavoritesScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.RecentlyPlayed.route) {
            RecentlyPlayedScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable("playlists") {
            PlaylistsScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToPlaylistDetail = { playlistId ->
                    navController.navigate(Screen.PlaylistDetail.createRoute(playlistId))
                }
            )
        }

        composable(
            route = Screen.PlaylistDetail.route,
            arguments = listOf(navArgument("playlistId") { type = NavType.LongType })
        ) { backStackEntry ->
            val playlistId = backStackEntry.arguments?.getLong("playlistId") ?: 0L
            PlaylistDetailScreen(
                playlistId = playlistId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.ArtistDetail.route,
            arguments = listOf(navArgument("artistName") { type = NavType.StringType })
        ) { backStackEntry ->
            val artistName = backStackEntry.arguments?.getString("artistName") ?: "Unknown Artist"
            ArtistDetailScreen(
                artistName = artistName,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
