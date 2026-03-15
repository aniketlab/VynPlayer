package com.vyn.player.ui.screens.nowplaying

import android.graphics.Color as AndroidColor
import androidx.collection.LruCache
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.*
import androidx.compose.foundation.MarqueeAnimationMode
import androidx.compose.foundation.LocalIndication
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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.palette.graphics.Palette
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.request.SuccessResult
import com.vyn.player.ui.components.MuzicSeekBar
import com.vyn.player.ui.components.SharedArtworkImage
import com.vyn.player.ui.components.extractArtworkModel
import com.vyn.player.ui.components.bounceClick
import com.vyn.player.ui.theme.*
import com.vyn.player.util.MetadataUtils
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.util.lerp
import coil.request.ImageRequest
import com.vyn.player.data.model.Song
import com.vyn.player.ui.actions.LocalSongActionDispatcher
import com.vyn.player.ui.actions.SongAction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

private data class DynamicArtworkBackgroundPalette(
    val dominant: Color,
    val accent: Color
)

private fun Color.adjustForNowPlayingBackground(): Color {
    val hsv = FloatArray(3)
    AndroidColor.colorToHSV(this.toArgb(), hsv)
    hsv[1] = (hsv[1] * 0.88f).coerceIn(0f, 1f)
    hsv[2] = (hsv[2] * 0.84f).coerceIn(0f, 1f)
    return Color(AndroidColor.HSVToColor(hsv))
}

private object NowPlayingPaletteCache {
    private val cache = object : LruCache<String, DynamicArtworkBackgroundPalette>(100) {}
    private val emptyKeys = ConcurrentHashMap.newKeySet<String>()

    fun get(key: String): DynamicArtworkBackgroundPalette? = cache.get(key)
    fun put(key: String, palette: DynamicArtworkBackgroundPalette) {
        cache.put(key, palette)
        emptyKeys.remove(key)
    }

    fun markEmpty(key: String) {
        emptyKeys.add(key)
        cache.remove(key)
    }

    fun isMarkedEmpty(key: String): Boolean = emptyKeys.contains(key)
}

private suspend fun extractNowPlayingPalette(
    song: Song,
    context: android.content.Context,
    imageLoader: ImageLoader
): DynamicArtworkBackgroundPalette? = withContext(Dispatchers.Default) {
    val cacheKey = song.artworkUrl ?: song.path.ifBlank { song.id.toString() }
    NowPlayingPaletteCache.get(cacheKey)?.let { return@withContext it }
    if (NowPlayingPaletteCache.isMarkedEmpty(cacheKey)) return@withContext null

    val artworkModel = extractArtworkModel(song, context, thumbnailMode = false) ?: run {
        NowPlayingPaletteCache.markEmpty(cacheKey)
        return@withContext null
    }

    val request = ImageRequest.Builder(context)
        .data(artworkModel)
        .allowHardware(false)
        .size(512)
        .build()

    val result = imageLoader.execute(request) as? SuccessResult ?: run {
        NowPlayingPaletteCache.markEmpty(cacheKey)
        return@withContext null
    }

    val bitmap = result.drawable.toBitmap(config = android.graphics.Bitmap.Config.ARGB_8888)
    val palette = Palette.from(bitmap).clearFilters().generate()
    val vibrant = palette.getVibrantColor(0)
    val darkVibrant = palette.getDarkVibrantColor(0)
    val muted = palette.getMutedColor(0)
    val darkMuted = palette.getDarkMutedColor(0)
    val dominant = palette.getDominantColor(0)

    val topColorArgb = listOf(vibrant, darkVibrant, muted, dominant).firstOrNull { it != 0 } ?: run {
        NowPlayingPaletteCache.markEmpty(cacheKey)
        return@withContext null
    }
    val accentArgb = listOf(darkVibrant, darkMuted, muted, dominant).firstOrNull { it != 0 } ?: topColorArgb

    val adjustedTopColor = Color(topColorArgb).adjustForNowPlayingBackground()
    val adjustedAccentColor = Color(accentArgb).adjustForNowPlayingBackground()

    return@withContext DynamicArtworkBackgroundPalette(
        dominant = adjustedTopColor,
        accent = adjustedAccentColor
    ).also { NowPlayingPaletteCache.put(cacheKey, it) }
}

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
        onOpenQueue = {},
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
    playbackState: com.vyn.player.player.PlaybackState,
    currentPosition: Long,
    isFavorite: Boolean,
    onNavigateBack: () -> Unit,
    onOpenQueue: () -> Unit,
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
    val onSongAction = LocalSongActionDispatcher.current
    val context = LocalContext.current
    val imageLoader = remember(context) { ImageLoader(context) }
    var showLyricsDialog by remember { mutableStateOf(false) }

    var dynamicPalette by remember(song?.id) { mutableStateOf<DynamicArtworkBackgroundPalette?>(null) }
    var activePaletteKey by remember { mutableStateOf<String?>(null) }
    var backgroundArtworkModel by remember(song?.id) { mutableStateOf<Any?>(null) }
    val rippleProgress = remember { Animatable(1f) }

    LaunchedEffect(song?.id) {
        if (song == null) {
            dynamicPalette = null
            backgroundArtworkModel = null
            activePaletteKey = null
            return@LaunchedEffect
        }
        val paletteKey = song.artworkUrl ?: song.path.ifBlank { song.id.toString() }
        if (activePaletteKey == paletteKey && dynamicPalette != null && backgroundArtworkModel != null) return@LaunchedEffect

        delay(250)
        val artworkModel = extractArtworkModel(song, context, thumbnailMode = false)
        if (artworkModel == null) {
            activePaletteKey = null
            backgroundArtworkModel = null
            dynamicPalette = null
            return@LaunchedEffect
        }
        val extractedPalette = extractNowPlayingPalette(song, context, imageLoader)
        if (extractedPalette != null) {
            activePaletteKey = paletteKey
            backgroundArtworkModel = artworkModel
            dynamicPalette = extractedPalette
            rippleProgress.snapTo(0f)
            rippleProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing)
            )
        } else {
            activePaletteKey = null
            backgroundArtworkModel = null
            dynamicPalette = null
        }
    }

    val defaultBackground = MaterialTheme.colorScheme.background
    val dynamicBackgroundEnabled = dynamicPalette != null && backgroundArtworkModel != null
    val animatedTopColor by animateColorAsState(
        targetValue = dynamicPalette?.dominant?.copy(alpha = 0.36f) ?: defaultBackground,
        animationSpec = tween(durationMillis = 1400, easing = FastOutSlowInEasing),
        label = "nowPlayingDynamicTopColor"
    )
    val animatedAccentColor by animateColorAsState(
        targetValue = dynamicPalette?.accent?.copy(alpha = 0.18f) ?: defaultBackground,
        animationSpec = tween(durationMillis = 1400, easing = FastOutSlowInEasing),
        label = "nowPlayingDynamicAccentColor"
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
        val density = LocalDensity.current
        val artSize = (screenWidth.value * 0.7f).coerceAtMost(screenHeight.value * 0.42f).toFloat().dp
        val rippleRadius = remember(screenWidth, screenHeight, rippleProgress.value) {
            val minRadius = minOf(screenWidth.value, screenHeight.value) * 0.18f
            val maxRadius = maxOf(screenWidth.value, screenHeight.value) * 1.05f
            lerp(minRadius, maxRadius, rippleProgress.value).dp
        }
        val rippleColor = animatedTopColor.copy(alpha = (1f - rippleProgress.value) * 0.22f)
        val rippleRadiusPx = with(density) { rippleRadius.toPx() }
        val backgroundGradient = remember(animatedTopColor, animatedAccentColor, defaultBackground) {
            Brush.verticalGradient(
                colors = listOf(animatedTopColor, animatedAccentColor, defaultBackground)
            )
        }

        Box(
            modifier = Modifier
                .matchParentSize()
                .background(defaultBackground)
        )

        if (dynamicBackgroundEnabled) {
            AsyncImage(
                model = remember(backgroundArtworkModel) {
                    ImageRequest.Builder(context)
                        .data(backgroundArtworkModel)
                        .allowHardware(false)
                        .crossfade(false)
                        .size(768)
                        .build()
                },
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .matchParentSize()
                    .blur(52.dp)
                    .graphicsLayer { alpha = 0.23f }
            )

            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(backgroundGradient)
            )

            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                rippleColor,
                                rippleColor.copy(alpha = rippleColor.alpha * 0.45f),
                                Color.Transparent
                            ),
                            center = androidx.compose.ui.geometry.Offset(
                                x = constraints.maxWidth / 2f,
                                y = constraints.maxHeight / 2f
                            ),
                            radius = rippleRadiusPx,
                            tileMode = TileMode.Clamp
                        )
                    )
            )
        }

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
            
            com.vyn.player.ui.components.SharedArtworkImage(
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
                                com.vyn.player.player.RepeatMode.ONE -> Icons.Rounded.RepeatOne
                                else -> Icons.Rounded.Repeat
                            },
                            contentDescription = "Repeat",
                            tint = if (playbackState.repeatMode != com.vyn.player.player.RepeatMode.OFF) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
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
                            icon = Icons.AutoMirrored.Rounded.QueueMusic,
                            label = "QUEUE",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            onClick = onOpenQueue
                        )
                        ActionItem(
                            icon = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                            label = if (isFavorite) "LIKED" else "LIKE",
                            color = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            onClick = onToggleFavorite,
                            pressedScale = 0.9f
                        )
                        ActionItem(
                            icon = Icons.Rounded.Lyrics,
                            label = "LYRICS",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            onClick = { showLyricsDialog = true }
                        )
                        ActionItem(
                            icon = Icons.Rounded.Share,
                            label = "SHARE",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            onClick = {
                                song?.let { onSongAction(SongAction.Share(it)) }
                            }
                        )
                        ActionItem(
                            icon = Icons.Rounded.Info,
                            label = "DETAILS",
                            onClick = {
                                song?.let { onSongAction(SongAction.ShowDetails(it)) }
                            },
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }
    }

    if (showLyricsDialog) {
        AlertDialog(
            onDismissRequest = { showLyricsDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(20.dp),
            title = {
                Text(
                    text = "Lyrics",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Lyrics,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Lyrics support is currently under development and will be available in a future update.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Coming soon in V2",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showLyricsDialog = false }) {
                    Text("OK", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
            }
        )
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
    onClick: () -> Unit = {},
    pressedScale: Float = 0.97f
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) pressedScale else 1f,
        animationSpec = tween(durationMillis = 120),
        label = "actionItemScale"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                onClick = onClick
            )
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
