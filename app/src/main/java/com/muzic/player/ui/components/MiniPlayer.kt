package com.muzic.player.ui.components

import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.hazeChild
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectVerticalDragGestures
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import coil.compose.AsyncImage
import com.muzic.player.data.model.Song
import com.muzic.player.ui.theme.*

@Composable
fun MiniPlayer(
    currentSong: Song?,
    isPlaying: Boolean,
    progress: Float,
    onPlayerClick: () -> Unit,
    onPlayPauseClick: () -> Unit,
    onNextClick: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    hazeState: HazeState? = null
) {
    if (currentSong == null) return

    var offsetY by remember { mutableFloatStateOf(0f) }
    val animatedOffsetY by animateFloatAsState(targetValue = offsetY, label = "mp_offset")

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp) // Slightly taller for better touch targets on large screens
            .offset { IntOffset(0, animatedOffsetY.roundToInt()) }
            .clip(RoundedCornerShape(20.dp))
            .then(
                if (hazeState != null) Modifier.hazeChild(state = hazeState, shape = RoundedCornerShape(20.dp), style = HazeStyle(blurRadius = 18.dp)) else Modifier
            )
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f), RoundedCornerShape(20.dp))
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragEnd = {
                        if (offsetY > 100f) {
                            onDismiss()
                        } else if (offsetY < -100f) {
                            onPlayerClick()
                            offsetY = 0f
                        } else {
                            offsetY = 0f
                        }
                    },
                    onDragCancel = { offsetY = 0f }
                ) { _, dragAmount ->
                    offsetY += dragAmount
                }
            }
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f), RoundedCornerShape(20.dp))
    ) {
        val screenWidth = maxWidth
        val horizontalPadding = if (screenWidth < 400.dp) 12.dp else 16.dp
        
        Column {
            // Thin progress bar
            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.5.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
            )

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(onClick = onPlayerClick)
                    .padding(horizontal = horizontalPadding),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Album art — 48dp rounded square
                MuzicImage(
                    model = currentSong.albumArtUri,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    fallbackText = currentSong.artist,
                    cornerRadius = 12.dp,
                    iconSize = 22.dp
                )

                Spacer(modifier = Modifier.width(14.dp))

                // Song info — Flexible container
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
                        fontSize = 15.sp,
                        lineHeight = 18.sp
                    )
                    Text(
                        text = currentSong.artist,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 13.sp,
                        lineHeight = 16.sp
                    )
                }

                // Controls aligned to end
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Play/Pause button
                    IconButton(
                        onClick = onPlayPauseClick,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Next button
                    IconButton(
                        onClick = onNextClick,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.SkipNext,
                            contentDescription = "Next",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
            }
        }
    }
}
