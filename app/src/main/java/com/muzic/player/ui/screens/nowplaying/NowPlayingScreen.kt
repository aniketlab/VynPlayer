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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.palette.graphics.Palette
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.muzic.player.ui.components.MuzicSeekBar
import com.muzic.player.ui.theme.*
import com.muzic.player.util.MetadataUtils
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun NowPlayingScreen(
    onNavigateBack: () -> Unit,
    viewModel: NowPlayingViewModel = hiltViewModel()
) {
    val playbackState by viewModel.playbackState.collectAsStateWithLifecycle()
    val currentPosition by viewModel.currentPosition.collectAsStateWithLifecycle()
    val isFavorite by viewModel.isFavorite.collectAsStateWithLifecycle()
    val song = playbackState.currentSong

    var showDetails by remember { mutableStateOf(false) }

    // ─── Dynamic color extraction ───
    val context = LocalContext.current
    val surfaceColor = MaterialTheme.colorScheme.surface
    var dominantColor by remember { mutableStateOf(surfaceColor) }
    var mutedColor by remember { mutableStateOf(surfaceColor) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(song?.albumArtUri) {
        if (song?.albumArtUri != null) {
            scope.launch {
                try {
                    val loader = ImageLoader(context)
                    val request = ImageRequest.Builder(context)
                        .data(song.albumArtUri)
                        .allowHardware(false)
                        .size(128)
                        .build()
                    val result = loader.execute(request)
                    if (result is SuccessResult) {
                        val bitmap = (result.drawable as? BitmapDrawable)?.bitmap
                        if (bitmap != null) {
                            val palette = Palette.from(bitmap).generate()
                            palette.dominantSwatch?.let { swatch ->
                                dominantColor = Color(swatch.rgb)
                            }
                            palette.mutedSwatch?.let { swatch ->
                                mutedColor = Color(swatch.rgb)
                            } ?: run {
                                mutedColor = dominantColor.copy(0.6f)
                            }
                        }
                    }
                } catch (_: Exception) { }
            }
        }
    }

    val animDominant by animateColorAsState(
        targetValue = dominantColor,
        animationSpec = tween(800),
        label = "dominantAnim"
    )
    val animMuted by animateColorAsState(
        targetValue = mutedColor,
        animationSpec = tween(800),
        label = "mutedAnim"
    )

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
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        animDominant.copy(0.35f),
                        animMuted.copy(0.2f),
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.background
                    )
                )
            )
            .pointerInput(Unit) {
                detectHorizontalDragGestures { _, dragAmount ->
                    if (dragAmount < -80f) {
                        viewModel.skipToNext()
                    } else if (dragAmount > 80f) {
                        viewModel.skipToPrevious()
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
                modifier = Modifier.constrainAs(topSection) {
                    top.linkTo(parent.top)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                    width = Dimension.fillToConstraints
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
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "PLAYING FROM",
                            color = Color.White.copy(0.6f),
                            letterSpacing = 2.sp,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = cleanAlbum.ifBlank { "Unknown Album" },
                            color = Color.White,
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
                            tint = Color.White
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
                        color = Color.White,
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
                        color = Color.White.copy(0.7f),
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        fontSize = 15.sp,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // ─── Zone 2: Album Artwork Section (Centered Vertically) ───
            Box(
                modifier = Modifier
                    .constrainAs(artSection) {
                        top.linkTo(topSection.bottom)
                        bottom.linkTo(playbackSection.top)
                        start.linkTo(parent.start)
                        end.linkTo(parent.end)
                    }
                    .width(screenWidth * 0.7f)
                    .aspectRatio(1f)
                    .shadow(
                        elevation = (artSize.value * 0.1f).coerceIn(20f, 40f).dp,
                        shape = RoundedCornerShape(20.dp),
                        ambientColor = animDominant.copy(0.3f),
                        spotColor = animDominant.copy(0.4f)
                    )
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(animDominant, animMuted)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (song != null && song.albumArtUri != null) {
                    AsyncImage(
                        model = song.albumArtUri,
                        contentDescription = "Album art",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Rounded.MusicNote,
                        contentDescription = null,
                        tint = Color.White.copy(0.8f),
                        modifier = Modifier.size((artSize.value * 0.25f).coerceIn(40f, 100f).dp)
                    )
                }
            }

            // ─── Zone 3: Playback Controls Section (Anchored to Bottom) ───
            Column(
                modifier = Modifier
                    .constrainAs(playbackSection) {
                        bottom.linkTo(parent.bottom)
                        start.linkTo(parent.start)
                        end.linkTo(parent.end)
                        width = Dimension.fillToConstraints
                    }
                    .padding(top = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Seek Bar
                MuzicSeekBar(
                    currentPosition = currentPosition,
                    duration = playbackState.duration,
                    onSeek = { viewModel.seekTo(it) },
                    accentColor = animDominant
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
                    BounceIconButton(onClick = { viewModel.toggleShuffle() }, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Rounded.Shuffle,
                            contentDescription = "Shuffle",
                            tint = if (playbackState.isShuffleEnabled) animDominant else Color.White.copy(0.4f),
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // Previous
                    BounceIconButton(onClick = { viewModel.skipToPrevious() }, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Rounded.SkipPrevious,
                            contentDescription = "Previous",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    // Play/Pause (64dp)
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .clickable { viewModel.togglePlayPause() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (playbackState.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = if (playbackState.isPlaying) "Pause" else "Play",
                            tint = Color.Black,
                            modifier = Modifier.size(34.dp)
                        )
                    }

                    // Next
                    BounceIconButton(onClick = { viewModel.skipToNext() }, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Rounded.SkipNext,
                            contentDescription = "Next",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    // Repeat
                    BounceIconButton(onClick = { viewModel.cycleRepeatMode() }, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = when (playbackState.repeatMode) {
                                com.muzic.player.player.RepeatMode.ONE -> Icons.Rounded.RepeatOne
                                else -> Icons.Rounded.Repeat
                            },
                            contentDescription = "Repeat",
                            tint = if (playbackState.repeatMode != com.muzic.player.player.RepeatMode.OFF) animDominant else Color.White.copy(0.4f),
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
                        .background(Color.White.copy(0.08f))
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
                            color = if (isFavorite) animDominant else Color.White.copy(0.7f),
                            onClick = { viewModel.toggleFavorite() }
                        )
                        ActionItem(icon = Icons.Rounded.QueueMusic, label = "QUEUE")
                        ActionItem(icon = Icons.Rounded.Lyrics, label = "LYRICS")
                        ActionItem(icon = Icons.Rounded.Share, label = "SHARE")
                        ActionItem(icon = Icons.Rounded.Info, label = "DETAILS", onClick = { showDetails = true })
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
        targetValue = if (isPressed) 1.1f else 1.0f,
        animationSpec = tween(120),
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
    color: Color = Color.White.copy(0.6f),
    onClick: () -> Unit = {}
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
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
