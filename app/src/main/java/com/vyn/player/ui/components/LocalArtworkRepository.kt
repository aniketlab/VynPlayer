package com.vyn.player.ui.components

import androidx.compose.runtime.staticCompositionLocalOf
import com.vyn.player.data.repository.ArtworkRepository

/**
 * CompositionLocal that provides the ArtworkRepository to all composables
 * in the tree. This avoids having to pass it as a parameter to every
 * SharedArtworkImage / MuzicImage call site.
 */
val LocalArtworkRepository = staticCompositionLocalOf<ArtworkRepository?> { null }
