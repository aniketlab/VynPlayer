package com.vyn.player.ui.components

import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.hazeChild
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import coil.compose.AsyncImage
import com.vyn.player.data.model.Song
import com.vyn.player.ui.theme.*
import androidx.compose.animation.*
import androidx.compose.ui.graphics.graphicsLayer

@Composable
fun MiniPlayer(
    currentSong: Song?,
    isPlaying: Boolean,
    progress: Float,
    onPlayerClick: () -> Unit,
    onPlayPauseClick: () -> Unit,
    onNextClick: () -> Unit,
    onPreviousClick: () -> Unit,
    modifier: Modifier = Modifier,
    hazeState: HazeState? = null
) {
    val surfaceColor = MaterialTheme.colorScheme.surface
    val isDarkTheme = androidx.compose.foundation.isSystemInDarkTheme()
    
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp)
            .graphicsLayer {
                shadowElevation = if (isDarkTheme) 6f else 12f
                spotShadowColor = if (isDarkTheme) Color.Black.copy(alpha = 0.25f) else Color.Black.copy(alpha = 0.1f)
                ambientShadowColor = if (isDarkTheme) Color.Black.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.05f)
                shape = RoundedCornerShape(18.dp)
            }
            .clip(RoundedCornerShape(18.dp))
            .then(
                if (isDarkTheme && hazeState != null) 
                    Modifier.hazeChild(state = hazeState, shape = RoundedCornerShape(18.dp), style = HazeStyle(blurRadius = 16.dp)) 
                else Modifier
            )
            .background(
                if (isDarkTheme) MaterialTheme.colorScheme.surface.copy(alpha = 0.65f)
                else MaterialTheme.colorScheme.surface
            )
            .border(
                width = 1.dp, 
                color = if (isDarkTheme) Color.White.copy(alpha = 0.1f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), 
                shape = RoundedCornerShape(18.dp)
            )
            .clickable(
                onClick = onPlayerClick
            )
    ) {
        if (currentSong == null) {
            // ─── EMPTY STATE ───
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
        } else {
            // ─── PLAYING STATE ───
            val screenWidth = maxWidth
            val horizontalPadding = if (screenWidth < 400.dp) 12.dp else 16.dp
            
            Box(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = horizontalPadding),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Album art
                    SharedArtworkImage(
                        song = currentSong,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        cornerRadius = 10.dp,
                        iconSize = 20.dp
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    // Song info
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

                    // Controls
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Previous
                        IconButton(onClick = onPreviousClick, modifier = Modifier.size(40.dp)) {
                            Icon(
                                imageVector = Icons.Rounded.SkipPrevious,
                                contentDescription = "Prev",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        // Play/Pause
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

                        // Next
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

                // Removed LinearProgressIndicator to keep UI clean per user request
            }
        }
    }
}
