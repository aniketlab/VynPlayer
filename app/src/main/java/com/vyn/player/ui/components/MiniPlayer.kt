package com.vyn.player.ui.components

import androidx.collection.LruCache
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.palette.graphics.Palette
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.vyn.player.data.model.Song
import com.vyn.player.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

private object MiniPlayerPaletteCache {
    private val cache = object : LruCache<String, Color>(100) {}
    private val emptyKeys = ConcurrentHashMap.newKeySet<String>()

    fun get(key: String): Color? = cache.get(key)

    fun put(key: String, color: Color) {
        cache.put(key, color)
        emptyKeys.remove(key)
    }

    fun markEmpty(key: String) {
        cache.remove(key)
        emptyKeys.add(key)
    }

    fun isMarkedEmpty(key: String): Boolean = emptyKeys.contains(key)
}

private suspend fun extractMiniPlayerDominantColor(
    song: Song,
    context: android.content.Context,
    imageLoader: ImageLoader
): Color? = withContext(Dispatchers.Default) {
    val cacheKey = song.artworkUrl ?: song.path.ifBlank { song.id.toString() }
    MiniPlayerPaletteCache.get(cacheKey)?.let { return@withContext it }
    if (MiniPlayerPaletteCache.isMarkedEmpty(cacheKey)) return@withContext null

    val artworkModel = extractArtworkModel(song, context, thumbnailMode = true) ?: run {
        MiniPlayerPaletteCache.markEmpty(cacheKey)
        return@withContext null
    }

    val request = ImageRequest.Builder(context)
        .data(artworkModel)
        .allowHardware(false)
        .size(256)
        .build()

    val result = imageLoader.execute(request) as? SuccessResult ?: run {
        MiniPlayerPaletteCache.markEmpty(cacheKey)
        return@withContext null
    }

    val palette = Palette.from(
        result.drawable.toBitmap(config = android.graphics.Bitmap.Config.ARGB_8888)
    ).clearFilters().generate()

    val dominantColor = listOf(
        palette.getDominantColor(0),
        palette.getMutedColor(0),
        palette.getVibrantColor(0),
        palette.getDarkMutedColor(0),
        palette.getDarkVibrantColor(0)
    ).firstOrNull { it != 0 }?.let(::Color)

    if (dominantColor == null) {
        MiniPlayerPaletteCache.markEmpty(cacheKey)
        null
    } else {
        MiniPlayerPaletteCache.put(cacheKey, dominantColor)
        dominantColor
    }
}

@Composable
fun MiniPlayer(
    currentSong: Song?,
    isPlaying: Boolean,
    progress: Float,
    onPlayerClick: () -> Unit,
    onPlayPauseClick: () -> Unit,
    onNextClick: () -> Unit,
    onPreviousClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerShape = RoundedCornerShape(18.dp)
    val context = LocalContext.current
    val imageLoader = remember(context) { ImageLoader(context) }
    var dominantTintColor by remember(currentSong?.id) { mutableStateOf<Color?>(null) }

    LaunchedEffect(currentSong?.id) {
        if (currentSong == null) {
            dominantTintColor = null
            return@LaunchedEffect
        }

        dominantTintColor = extractMiniPlayerDominantColor(
            song = currentSong,
            context = context,
            imageLoader = imageLoader
        )
    }

    val tintedMiniPlayerSurface = dominantTintColor
        ?.copy(alpha = 0.12f)
        ?.compositeOver(MaterialTheme.colorScheme.surface)
    val miniPlayerSurfaceColor = tintedMiniPlayerSurface ?: MaterialTheme.colorScheme.surface

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .frostedGlassBar(
                shape = containerShape,
                lightColor = (if (currentSong != null && dominantTintColor != null) {
                    miniPlayerSurfaceColor
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                }).copy(alpha = 0.96f),
                darkColor = (if (currentSong != null && dominantTintColor != null) {
                    miniPlayerSurfaceColor
                } else {
                    MaterialTheme.colorScheme.surface
                }).copy(alpha = 0.96f),
                elevation = 8.dp,
                borderWidth = 0.dp,
                borderColor = Color.Transparent
            )
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    onClick = onPlayerClick
                )
        ) {
            if (currentSong == null) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.MusicNote,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Start playing music",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            } else {
                val screenWidth = maxWidth
                val horizontalPadding = if (screenWidth < 400.dp) 12.dp else 16.dp

                Box(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = horizontalPadding),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SharedArtworkImage(
                            song = currentSong,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            cornerRadius = 10.dp,
                            iconSize = 20.dp
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = currentSong.title,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                fontSize = 15.sp
                            )
                            Text(
                                text = currentSong.artist,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                fontSize = 13.sp
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            IconButton(onClick = onPreviousClick, modifier = Modifier.size(40.dp)) {
                                Icon(
                                    imageVector = Icons.Rounded.SkipPrevious,
                                    contentDescription = "Prev",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.size(44.dp)
                            ) {
                                CircularProgressIndicator(
                                    progress = progress.coerceIn(0f, 1f),
                                    modifier = Modifier.fillMaxSize(),
                                    color = MaterialTheme.colorScheme.primary,
                                    trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                                    strokeWidth = 2.dp
                                )
                                IconButton(
                                    onClick = onPlayPauseClick,
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                                ) {
                                    Icon(
                                        imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            IconButton(onClick = onNextClick, modifier = Modifier.size(40.dp)) {
                                Icon(
                                    imageVector = Icons.Rounded.SkipNext,
                                    contentDescription = "Next",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
