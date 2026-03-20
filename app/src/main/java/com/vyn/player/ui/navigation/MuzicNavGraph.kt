package com.vyn.player.ui.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.vyn.player.ui.screens.home.HomeScreen
import com.vyn.player.ui.screens.home.SmartMixScreen
import com.vyn.player.ui.screens.library.LibraryScreen
import com.vyn.player.ui.screens.search.SearchScreen
import com.vyn.player.ui.screens.settings.SettingsScreen
import com.vyn.player.ui.screens.discover.DiscoverScreen
import com.vyn.player.ui.screens.profile.ProfileScreen
import com.vyn.player.ui.screens.profile.FavoritesScreen
import com.vyn.player.ui.screens.profile.RecentlyPlayedScreen
import com.vyn.player.ui.screens.profile.PlaylistsScreen
import com.vyn.player.ui.screens.profile.PlaylistDetailScreen
import com.vyn.player.ui.screens.artist.ArtistDetailScreen
import androidx.navigation.NavType
import androidx.navigation.navArgument

@Composable
fun MuzicNavGraph(
    navController: NavHostController,
    startDestination: String = Screen.Home.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        // ─── Global Transitions (applied to ALL screens) ───
        enterTransition = NavigationTransitions.enterTransition,
        exitTransition = NavigationTransitions.exitTransition,
        popEnterTransition = NavigationTransitions.popEnterTransition,
        popExitTransition = NavigationTransitions.popExitTransition,
        modifier = Modifier.fillMaxSize()
    ) {
        // ─── Bottom Tab Screens (use crossfade, not slide) ───
        composable(
            Screen.Home.route,
            enterTransition = { fadeIn(animationSpec = tween(200)) },
            exitTransition = { fadeOut(animationSpec = tween(200)) },
            popEnterTransition = { fadeIn(animationSpec = tween(200)) },
            popExitTransition = { fadeOut(animationSpec = tween(200)) }
        ) {
            HomeScreen(
                onNavigateToSearch = { navController.navigate(Screen.Search.route) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                onNavigateToArtist = { artistName: String ->
                    navController.navigate(Screen.ArtistDetail.createRoute(artistName))
                },
                onNavigateToFolders = { navController.navigate(Screen.AllFolders.route) },
                onNavigateToArtists = { navController.navigate(Screen.AllArtists.route) },
                onNavigateToFavorites = { navController.navigate(Screen.Favorites.route) },
                onNavigateToPlaylists = { navController.navigate("playlists") },
                onNavigateToSmartMix = { navController.navigate(Screen.SmartMix.route) },
                onNavigateToHomeRecentlyPlayed = { navController.navigate(Screen.HomeRecentlyPlayed.route) },
                onNavigateToHomeMostPlayed = { navController.navigate(Screen.HomeMostPlayed.route) }
            )
        }

        // ─── Home "See All" Screens ───
        composable(route = Screen.HomeRecentlyPlayed.route) {
            com.vyn.player.ui.screens.home.HomeRecentlyPlayedScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(route = Screen.HomeMostPlayed.route) {
            com.vyn.player.ui.screens.home.HomeMostPlayedScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.SmartMix.route) {
            SmartMixScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            Screen.Discover.route,
            enterTransition = { fadeIn(animationSpec = tween(200)) },
            exitTransition = { fadeOut(animationSpec = tween(200)) },
            popEnterTransition = { fadeIn(animationSpec = tween(200)) },
            popExitTransition = { fadeOut(animationSpec = tween(200)) }
        ) {
            DiscoverScreen(
                onNavigateToArtist = { artistName ->
                    navController.navigate(Screen.ArtistDetail.createRoute(artistName))
                },
                onNavigateToAlbumDetail = { albumId, albumName ->
                    navController.navigate(Screen.AlbumDetail.createRoute(albumId, albumName))
                },
                onNavigateToFolderDetail = { folderPath ->
                    navController.navigate(Screen.FolderDetail.createRoute(folderPath))
                },
                onNavigateToAllRecentlyAdded = {
                    navController.navigate(Screen.AllRecentlyAdded.route)
                },
                onNavigateToAllArtists = {
                    navController.navigate(Screen.AllArtists.route)
                },
                onNavigateToAllAlbums = {
                    navController.navigate(Screen.AllAlbums.route)
                },
                onNavigateToAllFolders = {
                    navController.navigate(Screen.AllFolders.route)
                }
            )
        }

        // ─── Discover "See All" Screens ───

        composable(route = Screen.AllRecentlyAdded.route) {
            com.vyn.player.ui.screens.discover.AllRecentlyAddedScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(route = Screen.AllArtists.route) {
            com.vyn.player.ui.screens.discover.AllArtistsScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToArtist = { artistName ->
                    navController.navigate(Screen.ArtistDetail.createRoute(artistName))
                }
            )
        }

        composable(route = Screen.AllAlbums.route) {
            com.vyn.player.ui.screens.discover.AllAlbumsScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToAlbumDetail = { albumId, albumName ->
                    navController.navigate(Screen.AlbumDetail.createRoute(albumId, albumName))
                }
            )
        }

        composable(route = Screen.AllFolders.route) {
            com.vyn.player.ui.screens.discover.AllFoldersScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToFolderDetail = { folderPath ->
                    navController.navigate(Screen.FolderDetail.createRoute(folderPath))
                }
            )
        }

        composable(
            Screen.Profile.route,
            enterTransition = { fadeIn(animationSpec = tween(200)) },
            exitTransition = { fadeOut(animationSpec = tween(200)) },
            popEnterTransition = { fadeIn(animationSpec = tween(200)) },
            popExitTransition = { fadeOut(animationSpec = tween(200)) }
        ) {
            ProfileScreen(
                onNavigateToFavorites = { navController.navigate(Screen.Favorites.route) },
                onNavigateToRecentlyPlayed = { navController.navigate(Screen.RecentlyPlayed.route) },
                onNavigateToPlaylists = { navController.navigate("playlists") }
            )
        }

        composable(Screen.Search.route) {
            SearchScreen()
        }

        composable(
            Screen.Library.route,
            enterTransition = { fadeIn(animationSpec = tween(200)) },
            exitTransition = { fadeOut(animationSpec = tween(200)) },
            popEnterTransition = { fadeIn(animationSpec = tween(200)) },
            popExitTransition = { fadeOut(animationSpec = tween(200)) }
        ) {
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

        // ─── Album Detail ───
        composable(
            route = Screen.AlbumDetail.route,
            arguments = listOf(
                navArgument("albumId") { type = NavType.LongType },
                navArgument("albumName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val albumId = backStackEntry.arguments?.getLong("albumId") ?: 0L
            val albumName = backStackEntry.arguments?.getString("albumName") ?: "Unknown Album"
            com.vyn.player.ui.screens.discover.AlbumDetailScreen(
                albumId = albumId,
                albumName = albumName,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // ─── Folder Detail ───
        composable(
            route = Screen.FolderDetail.route,
            arguments = listOf(navArgument("folderPath") { type = NavType.StringType })
        ) { backStackEntry ->
            val folderPath = backStackEntry.arguments?.getString("folderPath") ?: ""
            com.vyn.player.ui.screens.discover.FolderDetailScreen(
                folderPath = folderPath,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
