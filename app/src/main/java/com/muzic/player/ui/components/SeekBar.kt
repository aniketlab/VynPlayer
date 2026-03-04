package com.muzic.player.ui.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.muzic.player.ui.theme.*
import com.muzic.player.util.TimeUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MuzicSeekBar(
    currentPosition: Long,
    duration: Long,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var sliderPosition by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }

    val progress = if (isDragging) {
        sliderPosition
    } else {
        if (duration > 0) currentPosition.toFloat() / duration.toFloat() else 0f
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Slider(
            value = progress.coerceIn(0f, 1f),
            onValueChange = { newValue ->
                isDragging = true
                sliderPosition = newValue
            },
            onValueChangeFinished = {
                isDragging = false
                onSeek((sliderPosition * duration).toLong())
            },
            colors = SliderDefaults.colors(
                thumbColor = ElectricPurple,
                activeTrackColor = ElectricPurple,
                inactiveTrackColor = SeekbarInactive
            ),
            thumb = {
                SliderDefaults.Thumb(
                    interactionSource = remember { MutableInteractionSource() },
                    thumbSize = DpSize(14.dp, 14.dp),
                    colors = SliderDefaults.colors(thumbColor = ElectricPurple)
                )
            },
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = TimeUtils.formatDuration(
                    if (isDragging) (sliderPosition * duration).toLong() else currentPosition
                ),
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary
            )
            Text(
                text = TimeUtils.formatDuration(duration),
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary
            )
        }
    }
}
