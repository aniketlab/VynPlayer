package com.muzic.player.ui.components

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.muzic.player.ui.theme.*

@Composable
fun MuzicImage(
    model: Any?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    fallbackText: String? = null,
    cornerRadius: Dp = 20.dp,
    iconSize: Dp = 24.dp,
    showGradient: Boolean = true,
    elevation: Dp = 2.dp
) {
    var isError by remember { mutableStateOf(false) }
    val isEmpty = model == null || (model is Uri && model.toString().isEmpty()) || (model is String && model.isEmpty())

    Surface(
        modifier = modifier.clip(RoundedCornerShape(cornerRadius)),
        shape = RoundedCornerShape(cornerRadius),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = elevation,
        shadowElevation = elevation
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (!isEmpty && !isError) {
                val context = LocalContext.current
                val request = remember(model) {
                    coil.request.ImageRequest.Builder(context)
                        .data(model)
                        .crossfade(true)
                        .build()
                }
                AsyncImage(
                    model = request,
                    contentDescription = contentDescription,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    onState = { state ->
                        isError = state is coil.compose.AsyncImagePainter.State.Error
                    }
                )
            }

            if (isEmpty || isError) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            if (showGradient) {
                                Brush.linearGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.surfaceVariant,
                                        MaterialTheme.colorScheme.surface
                                    )
                                )
                            } else {
                                Brush.verticalGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.surfaceVariant,
                                        MaterialTheme.colorScheme.surface
                                    )
                                )
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    // Subtle music icon background overlay
                    Icon(
                        imageVector = Icons.Rounded.MusicNote,
                        contentDescription = null,
                        tint = Color.White.copy(0.05f),
                        modifier = Modifier.fillMaxSize(0.7f)
                    )

                    val displayFallback = if (fallbackText.isNullOrBlank() || 
                        fallbackText == "0" || 
                        fallbackText.lowercase().contains("unknown")) null else fallbackText

                    if (displayFallback != null) {
                        // Show Letter
                        Text(
                            text = displayFallback.take(1).uppercase(),
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White.copy(0.4f),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = (cornerRadius.value * 1.5f).coerceAtLeast(16f).sp
                        )
                        
                        // Small Music Icon Overlay in bottom right
                        Icon(
                            imageVector = Icons.Rounded.MusicNote,
                            contentDescription = null,
                            tint = Color.White.copy(0.3f),
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(8.dp)
                                .size(iconSize * 0.7f)
                        )
                    } else {
                        // Just show Music Icon if no text
                        Icon(
                            imageVector = Icons.Rounded.MusicNote,
                            contentDescription = null,
                            tint = Color.White.copy(0.2f),
                            modifier = Modifier.size(iconSize)
                        )
                    }
                }
            }
        }
    }
}
