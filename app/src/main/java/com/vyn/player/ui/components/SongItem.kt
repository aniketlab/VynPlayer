package com.vyn.player.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import com.vyn.player.ui.components.bounceClick
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.vyn.player.data.model.Song
import com.vyn.player.ui.actions.SongAction
import com.vyn.player.ui.theme.*
import com.vyn.player.util.TimeUtils
import androidx.compose.foundation.ExperimentalFoundationApi

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SongItem(
    song: Song,
    isPlaying: Boolean,
    isPlaybackActive: Boolean = isPlaying,
    onSongClick: () -> Unit,
    onAction: (SongAction) -> Unit,
    modifier: Modifier = Modifier,
    isScrolling: Boolean = false
) {
    var showActionSheet by remember { mutableStateOf(false) }
    val cleanTitle = remember(song.title) { com.vyn.player.util.MetadataUtils.cleanTitle(song.title) }
    val subtitle = remember(song.artist, song.duration) {
        "${com.vyn.player.util.MetadataUtils.cleanArtist(song.artist)} • ${TimeUtils.formatDuration(song.duration)}"
    }

    val bgColor by animateColorAsState(
        targetValue = if (isPlaying) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else Color.Transparent,
        animationSpec = tween(300),
        label = "songBg"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .bounceCombinedClickable(
                onClick = onSongClick,
                onLongClick = { showActionSheet = true }
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(contentAlignment = Alignment.Center) {
            // Album Art
            SharedArtworkImage(
                song = song,
                contentDescription = null,
                modifier = Modifier.size(52.dp),
                cornerRadius = 12.dp,
                iconSize = 24.dp,
                isScrolling = isScrolling,
                thumbnailMode = true
            )

            // Overlay Playing indicator if playing
            if (isPlaying) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)),
                    contentAlignment = Alignment.Center
                ) {
                    NowPlayingIndicator(
                        isAnimating = isPlaybackActive,
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Song info
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = cleanTitle,
                style = MaterialTheme.typography.bodyLarge,
                color = if (isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                fontWeight = if (isPlaying) FontWeight.Bold else FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                fontSize = 15.sp,
                lineHeight = 18.sp
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontSize = 12.sp
            )
        }

        // Overflow menu
        IconButton(
            onClick = { showActionSheet = true },
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.MoreVert,
                contentDescription = "More Options",
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.size(20.dp)
            )
        }
    }

    if (showActionSheet) {
        SongActionSheet(
            song = song,
            onDismissRequest = { showActionSheet = false },
            onAction = onAction
        )
    }
}

@Composable
private fun NowPlayingIndicator(
    isAnimating: Boolean,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary
) {
    val infiniteTransition = rememberInfiniteTransition(label = "nowPlayingIndicator")
    val firstBar by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 560, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "firstBar"
    )
    val secondBar by infiniteTransition.animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 640, easing = LinearEasing, delayMillis = 90),
            repeatMode = RepeatMode.Reverse
        ),
        label = "secondBar"
    )
    val thirdBar by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 600, easing = LinearEasing, delayMillis = 180),
            repeatMode = RepeatMode.Reverse
        ),
        label = "thirdBar"
    )

    val barHeights = if (isAnimating) {
        listOf(firstBar, secondBar, thirdBar)
    } else {
        listOf(0.45f, 0.8f, 0.6f)
    }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.Bottom
    ) {
        barHeights.forEach { heightFraction ->
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight(heightFraction)
                    .clip(RoundedCornerShape(999.dp))
                    .background(color)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongActionSheet(
    song: Song,
    onDismissRequest: () -> Unit,
    onAction: (SongAction) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp, top = 8.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SharedArtworkImage(
                    song = song,
                    contentDescription = null,
                    modifier = Modifier.size(56.dp),
                    cornerRadius = 12.dp,
                    iconSize = 24.dp
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = com.vyn.player.util.MetadataUtils.cleanTitle(song.title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = com.vyn.player.util.MetadataUtils.cleanArtist(song.artist),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            
            HorizontalDivider(modifier = Modifier.padding(horizontal = 24.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
            Spacer(modifier = Modifier.height(12.dp))

            ActionSheetItem(icon = Icons.Rounded.QueueMusic, label = "Play next") {
                onAction(SongAction.PlayNext(song))
                onDismissRequest()
            }
            ActionSheetItem(icon = Icons.Rounded.PlaylistAdd, label = "Add to playlist") {
                onAction(SongAction.AddToPlaylist(song))
                onDismissRequest()
            }
            ActionSheetItem(icon = if (song.isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder, label = if (song.isFavorite) "Remove from favorites" else "Add to favorites") {
                onAction(SongAction.ToggleFavorite(song))
                onDismissRequest()
            }
            ActionSheetItem(icon = Icons.Rounded.Share, label = "Share") {
                onAction(SongAction.Share(song))
                onDismissRequest()
            }
            ActionSheetItem(icon = Icons.Rounded.Info, label = "Song details") {
                onAction(SongAction.ShowDetails(song))
                onDismissRequest()
            }
        }
    }
}

@Composable
fun ActionSheetItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .bounceClick(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
            modifier = Modifier.size(26.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium
        )
    }
}
