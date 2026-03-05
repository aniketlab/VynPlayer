package com.muzic.player.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String? = null, val icon: ImageVector? = null) {
    object Splash : Screen("splash")
    
    // Bottom Navigation Screens
    object Home : Screen("home", "Home", Icons.Rounded.Home)
    object Discover : Screen("discover", "Discover", Icons.Rounded.Explore)
    object Library : Screen("library", "Library", Icons.Rounded.LibraryMusic)
    object Profile : Screen("profile", "Profile", Icons.Rounded.Person)
    
    // Other Screens
    object Search : Screen("search")
    object Settings : Screen("settings")
    object NowPlaying : Screen("now_playing")
    object Favorites : Screen("favorites")
    object RecentlyPlayed : Screen("recently_played")
    object PlaylistDetail : Screen("playlist_detail/{playlistId}") {
        fun createRoute(playlistId: Long) = "playlist_detail/$playlistId"
    }
    object ArtistDetail : Screen("artist_detail/{artistName}") {
        fun createRoute(artistName: String) = "artist_detail/${android.net.Uri.encode(artistName)}"
    }
}

val BottomNavScreens = listOf(
    Screen.Home,
    Screen.Discover,
    Screen.Library,
    Screen.Profile
)
