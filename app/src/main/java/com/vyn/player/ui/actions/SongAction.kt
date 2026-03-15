package com.vyn.player.ui.actions

import androidx.compose.runtime.compositionLocalOf
import com.vyn.player.data.model.Song

sealed class SongAction(open val song: Song) {
    data class Play(
        override val song: Song,
        val queue: List<Song>? = null
    ) : SongAction(song)

    data class PlayNext(override val song: Song) : SongAction(song)
    data class AddToPlaylist(override val song: Song) : SongAction(song)
    data class Share(override val song: Song) : SongAction(song)
    data class ShowDetails(override val song: Song) : SongAction(song)
    data class ToggleFavorite(override val song: Song) : SongAction(song)
}

val LocalSongActionDispatcher = compositionLocalOf<(SongAction) -> Unit> {
    error("LocalSongActionDispatcher not provided")
}
