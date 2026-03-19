package com.vyn.player.ui.components

import android.net.Uri
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.vyn.player.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.absoluteValue

@Composable
fun MuzicImage(
    model: Any?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    fallbackText: String? = null,
    cornerRadius: Dp = 20.dp,
    iconSize: Dp = 24.dp,
    showGradient: Boolean = true,
    elevation: Dp = 2.dp,
    albumName: String? = null,
    artistName: String? = null,
    thumbnailMode: Boolean = true,
    isScrolling: Boolean = false
) {
    var isError by remember { mutableStateOf(false) }
    val isEmpty = model == null || (model is Uri && model.toString().isEmpty()) || (model is String && model.isEmpty())
    
    val artworkRepository = LocalArtworkRepository.current
    
    var internetModel by remember(albumName, artistName) { mutableStateOf<Any?>(null) }

    // Check disk cache only when primary model is missing - NO network calls in UI
    LaunchedEffect(isEmpty, isError, albumName, artistName) {
        if ((isEmpty || isError) && artworkRepository != null && !albumName.isNullOrBlank()) {
            val newModel = withContext(Dispatchers.IO) {
                artworkRepository.getCachedAlbumArtwork(albumName, artistName ?: "", thumbnailMode)
            }
            if (internetModel != newModel) {
                internetModel = newModel
            }
        }
    }

    val showPlaceholder = (isEmpty || isError) && internetModel == null

    val placeholderBrush = remember(fallbackText) {
        val hash = fallbackText?.hashCode() ?: 0
        val hue = (hash % 360).toFloat().absoluteValue
        val c1 = Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, 0.62f, 0.20f)))
        val c2 = Color(android.graphics.Color.HSVToColor(floatArrayOf((hue + 30f) % 360f, 0.50f, 0.30f)))
        val c3 = Color(android.graphics.Color.HSVToColor(floatArrayOf((hue + 60f) % 360f, 0.40f, 0.40f)))
        Brush.linearGradient(listOf(c1, c2, c3))
    }

    val fallbackTextColor = Color(0xFFFFFFFF)


    Surface(
        modifier = modifier.clip(RoundedCornerShape(cornerRadius)),
        shape = RoundedCornerShape(cornerRadius),
        color = MaterialTheme.colorScheme.surfaceVariant, // Paint over
        tonalElevation = elevation,
        shadowElevation = elevation
    ) {
        val actualModel = if (!isEmpty && !isError) model
            else if (internetModel != null) internetModel
            else null
            
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(placeholderBrush),
            contentAlignment = Alignment.Center
        ) {
            val displayFallback = if (fallbackText.isNullOrBlank() ||
                fallbackText == "0" ||
                fallbackText.lowercase().contains("unknown")) null else fallbackText

            if (displayFallback != null) {
                Text(
                    text = displayFallback.take(1).uppercase(),
                    color = fallbackTextColor.copy(alpha = 0.85f),
                    fontWeight = FontWeight.Bold,
                    fontSize = (cornerRadius.value * 3.5f).coerceIn(24f, 130f).sp,
                    textAlign = TextAlign.Center
                )
            } else {
                Icon(
                    imageVector = Icons.Rounded.MusicNote,
                    contentDescription = null,
                    tint = fallbackTextColor.copy(alpha = 0.85f),
                    modifier = Modifier.size(iconSize * 1.5f)
                )
            }

            if (actualModel != null) {
                val context = LocalContext.current
                val request = remember(actualModel, thumbnailMode, albumName, artistName) {
                    val builder = coil.request.ImageRequest.Builder(context)
                        .data(actualModel)
                        .size(if (thumbnailMode) 256 else 512)
                        .crossfade(150)
                        .bitmapConfig(if (thumbnailMode) android.graphics.Bitmap.Config.RGB_565 else android.graphics.Bitmap.Config.ARGB_8888)
                        .allowHardware(!thumbnailMode) // false for thumbnails allows bitmap memory reuse
                        .diskCachePolicy(coil.request.CachePolicy.ENABLED)
                        .memoryCachePolicy(coil.request.CachePolicy.ENABLED)
                        
                    if (!albumName.isNullOrBlank() && !albumName.equals("unknown", ignoreCase = true)) {
                        builder.memoryCacheKey("${albumName}_${artistName}_${if (thumbnailMode) "thumb" else "full"}")
                    }
                    builder.build()
                }
                
                AsyncImage(
                    model = request,
                    contentDescription = contentDescription,
                    modifier = Modifier.fillMaxWidth(),
                    contentScale = ContentScale.Crop,
                    alignment = Alignment.Center,
                    onState = { state ->
                        if (state is coil.compose.AsyncImagePainter.State.Error) {
                            if (actualModel == model) {
                                isError = true
                            }
                        }
                    }
                )
            }
        }
    }
}
