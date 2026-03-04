package com.muzic.player.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muzic.player.ui.theme.*
import com.muzic.player.util.TimeUtils

@Composable
fun MuzicSeekBar(
    currentPosition: Long,
    duration: Long,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var isDragging by remember { mutableStateOf(false) }
    var dragPosition by remember { mutableFloatStateOf(0f) }

    val progress = if (duration > 0) {
        if (isDragging) dragPosition else currentPosition.toFloat() / duration.toFloat()
    } else 0f

    Column(modifier = modifier.fillMaxWidth()) {
        Slider(
            value = progress.coerceIn(0f, 1f),
            onValueChange = { value ->
                isDragging = true
                dragPosition = value
            },
            onValueChangeFinished = {
                isDragging = false
                onSeek((dragPosition * duration).toLong())
            },
            colors = SliderDefaults.colors(
                thumbColor = MuzicRed,
                activeTrackColor = MuzicRed,
                inactiveTrackColor = DarkSurfaceSecondary
            ),
            modifier = Modifier.height(20.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = TimeUtils.formatDuration(
                    if (isDragging) (dragPosition * duration).toLong() else currentPosition
                ),
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = TimeUtils.formatDuration(duration),
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

