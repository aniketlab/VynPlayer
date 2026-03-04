package com.muzic.player.ui.navigation

sealed class Screen(val route: String) {
    data object Splash : Screen("splash")
    data object Library : Screen("library")
    data object NowPlaying : Screen("now_playing")
    data object Settings : Screen("settings")

    data object PlaylistDetail : Screen("playlist/{playlistId}") {
        fun createRoute(playlistId: Long) = "playlist/$playlistId"
    }

    data object AlbumDetail : Screen("album/{albumId}/{albumName}") {
        fun createRoute(albumId: Long, albumName: String) = "album/$albumId/$albumName"
    }

    data object ArtistDetail : Screen("artist/{artistName}") {
        fun createRoute(artistName: String) = "artist/$artistName"
    }

    data object FolderDetail : Screen("folder/{folderPath}") {
        fun createRoute(folderPath: String) = "folder/${java.net.URLEncoder.encode(folderPath, "UTF-8")}"
    }
}
