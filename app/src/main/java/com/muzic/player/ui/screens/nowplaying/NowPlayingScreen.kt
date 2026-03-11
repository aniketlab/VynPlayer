package com.muzic.player.ui.screens.nowplaying

import android.graphics.drawable.BitmapDrawable
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.MarqueeAnimationMode
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.palette.graphics.Palette
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.muzic.player.ui.components.MuzicSeekBar
import com.muzic.player.ui.components.SharedArtworkImage
import com.muzic.player.ui.components.bounceClick
import com.muzic.player.ui.theme.*
import com.muzic.player.util.MetadataUtils
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.util.lerp
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun NowPlayingScreen(
    onNavigateBack: () -> Unit,
    viewModel: NowPlayingViewModel = hiltViewModel()
) {
    val playbackState by viewModel.playbackState.collectAsState()
    val currentPosition by viewModel.currentPosition.collectAsState()
    val isFavorite by viewModel.isFavorite.collectAsState()
    
    NowPlayingContent(
        playbackState = playbackState,
        currentPosition = currentPosition,
        isFavorite = isFavorite,
        onNavigateBack = onNavigateBack,
        onTogglePlayPause = { viewModel.togglePlayPause() },
        onSkipToNext = { viewModel.skipToNext() },
        onSkipToPrevious = { viewModel.skipToPrevious() },
        onSeek = { viewModel.seekTo(it) },
        onToggleFavorite = { viewModel.toggleFavorite() },
        onCycleRepeatMode = { viewModel.cycleRepeatMode() },
        onToggleShuffle = { viewModel.toggleShuffle() }
    )
}

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun NowPlayingContent(
    playbackState: com.muzic.player.player.PlaybackState,
    currentPosition: Long,
    isFavorite: Boolean,
    onNavigateBack: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onSkipToNext: () -> Unit,
    onSkipToPrevious: () -> Unit,
    onSeek: (Long) -> Unit,
    onToggleFavorite: () -> Unit,
    onCycleRepeatMode: () -> Unit,
    onToggleShuffle: () -> Unit,
    modifier: Modifier = Modifier,
    expansionProgress: Float = 1.0f 
) {
    val song = playbackState.currentSong
    var showDetails by remember { mutableStateOf(false) }

    // ─── Clean metadata ───
    val cleanTitle = remember(song?.title) {
        MetadataUtils.cleanTitle(song?.title ?: "No song playing")
    }
    val cleanArtist = remember(song?.artist) {
        MetadataUtils.cleanArtist(song?.artist ?: "")
    }
    val cleanAlbum = remember(song?.album) {
        MetadataUtils.cleanTitle(song?.album ?: "")
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectHorizontalDragGestures { _, dragAmount ->
                    if (dragAmount < -80f) {
                        onSkipToNext()
                    } else if (dragAmount > 80f) {
                        onSkipToPrevious()
                    }
                }
            }
    ) {
        val screenHeight = maxHeight
        val screenWidth = maxWidth
        val adaptivePadding = getAdaptivePadding()
        val artSize = (screenWidth.value * 0.7f).coerceAtMost(screenHeight.value * 0.42f).toFloat().dp

        ConstraintLayout(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = adaptivePadding)
        ) {
            val (topSection, artSection, playbackSection) = createRefs()

            // ─── Zone 1: Top Metadata Section (Pinned to Top) ───
            Column(
                modifier = Modifier
                    .constrainAs(topSection) {
                        top.linkTo(parent.top)
                        start.linkTo(parent.start)
                        end.linkTo(parent.end)
                        width = Dimension.fillToConstraints
                    }
                    .graphicsLayer {
                        alpha = expansionProgress
                        translationY = 50f * (1f - expansionProgress)
                    },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Navigation Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = (screenHeight.value * 0.015f).coerceAtLeast(8f).dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BounceIconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Rounded.KeyboardArrowDown,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "PLAYING FROM",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            letterSpacing = 2.sp,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = cleanAlbum.ifBlank { "Unknown Album" },
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontSize = 12.sp
                        )
                    }

                    BounceIconButton(onClick = { }) {
                        Icon(
                            imageVector = Icons.Rounded.MoreHoriz,
                            contentDescription = "More",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Song Title & Artist Area (Now Full Width)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = cleanTitle,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        fontSize = (screenWidth.value * 0.065f).coerceIn(22f, 28f).sp,
                        letterSpacing = (-0.5).sp,
                        lineHeight = (screenWidth.value * 0.075f).coerceIn(26f, 32f).sp,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = cleanArtist,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        fontSize = 15.sp,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // ─── Zone 2: Album Artwork Section (Centered Vertically) ───
            val navPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
            val miniArtCenterY = screenHeight.value - (navPadding.value + 36 + 12) // Approx mini-player center Y relative to bottom
            val fullArtCenterY = screenHeight.value / 2 // Approx center
            
            com.muzic.player.ui.components.SharedArtworkImage(
                song = song,
                contentDescription = "Album art",
                modifier = Modifier
                    .constrainAs(artSection) {
                        top.linkTo(topSection.bottom)
                        bottom.linkTo(playbackSection.top)
                        start.linkTo(parent.start)
                        end.linkTo(parent.end)
                    }
                    .width(screenWidth * 0.7f)
                    .aspectRatio(1f)
                    .graphicsLayer {
                        // Target Size: screenWidth * 0.7f
                        // Start Size: 48dp
                        val targetSizePx = screenWidth.toPx() * 0.7f
                        val startSizePx = 48.dp.toPx()
                        
                        val currentScale = startSizePx / targetSizePx + (1f - (startSizePx / targetSizePx)) * expansionProgress
                        scaleX = currentScale
                        scaleY = currentScale
                        
                        // Origin in Full Player (Center of ConstraintLayout)
                        // Note: Constraints center it in parent.
                        // Mini player art is at: x = 16dp + 16dp = 32dp. y = bottom - navPadding - 8dp - 4dp - 12dp - 24dp
                        val startX = 32.dp.toPx()
                        val startY = size.height - navPadding.toPx() - 8.dp.toPx() - 4.dp.toPx() - 36.dp.toPx() // center of 48dp art
                        
                        // Full Player Center (where it would be at progress = 1)
                        val centerX = size.width / 2
                        val centerY = size.height / 2
                        
                        // We need to translate from start to center
                        translationX = (startX - centerX) * (1f - expansionProgress)
                        translationY = (startY - centerY) * (1f - expansionProgress)
                    }
                    .shadow(
                        elevation = (artSize.value * 0.1f).coerceIn(20f, 40f).dp * expansionProgress,
                        shape = RoundedCornerShape(lerp(10f, 20f, expansionProgress).dp),
                        ambientColor = MaterialTheme.colorScheme.primary.copy(0.3f),
                        spotColor = MaterialTheme.colorScheme.primary.copy(0.4f)
                    ),
                cornerRadius = lerp(10f, 20f, expansionProgress).dp,
                iconSize = (artSize.value * 0.25f).coerceIn(40f, 100f).dp,
                elevation = 0.dp
            )

            // ─── Zone 3: Playback Controls Section (Anchored to Bottom) ───
            Column(
                modifier = Modifier
                    .constrainAs(playbackSection) {
                        bottom.linkTo(parent.bottom)
                        start.linkTo(parent.start)
                        end.linkTo(parent.end)
                        width = Dimension.fillToConstraints
                    }
                    .padding(top = 24.dp)
                    .graphicsLayer {
                        alpha = expansionProgress
                        translationY = 100f * (1f - expansionProgress)
                    },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Seek Bar
                MuzicSeekBar(
                    currentPosition = currentPosition,
                    duration = playbackState.duration,
                    onSeek = onSeek
                )

                // Playback Controls
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 20.dp, bottom = (screenHeight.value * 0.03f).coerceIn(12f, 32f).dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Shuffle
                    BounceIconButton(onClick = onToggleShuffle, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Rounded.Shuffle,
                            contentDescription = "Shuffle",
                            tint = if (playbackState.isShuffleEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // Previous
                    BounceIconButton(onClick = onSkipToPrevious, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Rounded.SkipPrevious,
                            contentDescription = "Previous",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    // Play/Pause (64dp)
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                            .bounceClick { onTogglePlayPause() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (playbackState.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = if (playbackState.isPlaying) "Pause" else "Play",
                            tint = Color.White,
                            modifier = Modifier.size(34.dp)
                        )
                    }

                    // Next
                    BounceIconButton(onClick = onSkipToNext, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Rounded.SkipNext,
                            contentDescription = "Next",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    // Repeat
                    BounceIconButton(onClick = onCycleRepeatMode, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = when (playbackState.repeatMode) {
                                com.muzic.player.player.RepeatMode.ONE -> Icons.Rounded.RepeatOne
                                else -> Icons.Rounded.Repeat
                            },
                            contentDescription = "Repeat",
                            tint = if (playbackState.repeatMode != com.muzic.player.player.RepeatMode.OFF) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                // Bottom Actions Bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = (screenHeight.value * 0.02f).dp)
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ActionItem(
                            icon = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                            label = if (isFavorite) "LIKED" else "LIKE",
                            color = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            onClick = onToggleFavorite
                        )
                        ActionItem(icon = Icons.AutoMirrored.Rounded.QueueMusic, label = "QUEUE", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        ActionItem(icon = Icons.Rounded.Lyrics, label = "LYRICS", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        ActionItem(icon = Icons.Rounded.Share, label = "SHARE", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        ActionItem(icon = Icons.Rounded.Info, label = "DETAILS", onClick = { showDetails = true }, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    }
                }
            }
        }
    }

    if (showDetails && song != null) {
        SongDetailsDialog(song = song) { showDetails = false }
    }
}

@Composable
fun BounceIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1.0f,
        animationSpec = tween(80),
        label = "scale"
    )

    IconButton(
        onClick = onClick,
        interactionSource = interactionSource,
        modifier = modifier.scale(scale)
    ) {
        content()
    }
}

@Composable
private fun ActionItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit = {}
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .bounceClick(onClick = onClick)
            .padding(8.dp)
    ) {
        Icon(icon, label, tint = color, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            color = color,
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp,
            textAlign = TextAlign.Center
        )
    }
}
