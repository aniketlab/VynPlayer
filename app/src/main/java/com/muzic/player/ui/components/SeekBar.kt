package com.muzic.player.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muzic.player.ui.theme.*
import com.muzic.player.util.TimeUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MuzicSeekBar(
    currentPosition: Long,
    duration: Long,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier,
    accentColor: Color = MaterialTheme.colorScheme.primary
) {
    var isDragging by remember { mutableStateOf(false) }
    var dragPosition by remember { mutableFloatStateOf(0f) }

    val progress = if (duration > 0L) {
        if (isDragging) dragPosition 
        else {
            if (currentPosition >= duration) 1f
            else (currentPosition.toFloat() / duration.toFloat()).coerceIn(0f, 0.999f)
        }
    } else 0f

    val thumbScale by animateFloatAsState(
        targetValue = if (isDragging) 1.25f else 1.0f,
        animationSpec = tween(150),
        label = "thumbScale"
    )

    Column(modifier = modifier.fillMaxWidth()) {
        // Slider
        Slider(
            value = progress.coerceIn(0f, 1f),
            onValueChange = { value ->
                isDragging = true
                dragPosition = value
            },
            onValueChangeFinished = {
                onSeek((dragPosition * duration).toLong())
                isDragging = false
            },
            colors = SliderDefaults.colors(
                thumbColor = accentColor,
                activeTrackColor = accentColor,
                inactiveTrackColor = MaterialTheme.colorScheme.onSurface.copy(0.15f)
            ),
            thumb = {
                // Centering thumb manually in a box that matches slider height (32.dp)
                Box(
                    modifier = Modifier.size(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .scale(thumbScale)
                            .background(accentColor, CircleShape)
                    )
                }
            },
            track = { sliderState ->
                // Centering track manually in a box that matches slider height (32.dp)
                Box(
                    modifier = Modifier.fillMaxWidth().height(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val progress = sliderState.value
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.onSurface.copy(0.15f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(progress)
                                .fillMaxHeight()
                                .background(accentColor)
                        )
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(32.dp)
        )

        // Time labels
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = TimeUtils.formatDuration(
                    if (isDragging) (dragPosition * duration).toLong() else currentPosition
                ),
                color = MaterialTheme.colorScheme.onSurface.copy(0.7f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = TimeUtils.formatDuration(duration),
                color = MaterialTheme.colorScheme.onSurface.copy(0.7f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
