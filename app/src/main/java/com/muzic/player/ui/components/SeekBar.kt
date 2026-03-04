package com.muzic.player.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muzic.player.ui.theme.*
import com.muzic.player.util.TimeUtils
import kotlin.math.sin

@Composable
fun MuzicSeekBar(
    currentPosition: Long,
    duration: Long,
    isPlaying: Boolean,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var isDragging by remember { mutableStateOf(false) }
    var dragPosition by remember { mutableFloatStateOf(0f) }

    val progress = if (duration > 0) {
        if (isDragging) dragPosition else currentPosition.toFloat() / duration.toFloat()
    } else 0f

    Column(modifier = modifier.fillMaxWidth()) {
        // Android 16 Style Squiggly Wave Slider
        SquigglySlider(
            progress = progress.coerceIn(0f, 1f),
            isPlaying = isPlaying,
            activeColor = MuzicRed,
            inactiveColor = DarkSurfaceSecondary,
            onProgressChange = { value ->
                isDragging = true
                dragPosition = value
                // Optional: live seek while dragging
                // onSeek((value * duration).toLong())
            },
            onDragEnd = {
                isDragging = false
                onSeek((dragPosition * duration).toLong())
            },
            modifier = Modifier.padding(vertical = 8.dp)
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

@Composable
fun SquigglySlider(
    progress: Float,
    isPlaying: Boolean,
    onProgressChange: (Float) -> Unit,
    onDragEnd: () -> Unit,
    modifier: Modifier = Modifier,
    activeColor: Color = MuzicRed,
    inactiveColor: Color = DarkSurfaceSecondary,
    thumbColor: Color = MuzicRed
) {
    val phase by rememberInfiniteTransition(label = "wave_phase").animateFloat(
        initialValue = 0f,
        targetValue = -(2f * Math.PI.toFloat()), // Negative for leftward wave flow
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase_anim"
    )

    // Smooth amplitude transition based on playing state
    val amplitudeScale by animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0.1f, // flatten when paused
        animationSpec = tween(400, easing = FastOutSlowInEasing),
        label = "amp_scale"
    )

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(32.dp)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = { offset ->
                        onProgressChange((offset.x / size.width).coerceIn(0f, 1f))
                        val success = tryAwaitRelease()
                        if (success) onDragEnd()
                    }
                )
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset -> onProgressChange((offset.x / size.width).coerceIn(0f, 1f)) },
                    onDragEnd = { onDragEnd() },
                    onDragCancel = { onDragEnd() }
                ) { change, _ ->
                    onProgressChange((change.position.x / size.width).coerceIn(0f, 1f))
                }
            }
    ) {
        val width = constraints.maxWidth.toFloat()
        val height = constraints.maxHeight.toFloat()
        val centerY = height / 2f
        val amplitude = (height / 4f) * amplitudeScale
        val frequency = 0.04f

        Canvas(modifier = Modifier.fillMaxSize()) {
            val progressWidth = width * progress

            // Active Track (Squiggly)
            val activePath = Path()
            activePath.moveTo(0f, centerY)
            for (x in 0..progressWidth.toInt() step 3) {
                val y = centerY + sin(x * frequency + phase) * amplitude
                activePath.lineTo(x.toFloat(), y)
            }

            drawPath(
                path = activePath,
                color = activeColor,
                style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            )

            // Inactive Track (Straight)
            drawLine(
                color = inactiveColor,
                start = Offset(progressWidth, centerY),
                end = Offset(width, centerY),
                strokeWidth = 3.dp.toPx(),
                cap = StrokeCap.Round
            )

            // Thumb
            val thumbY = if (progressWidth > 0 && progressWidth < width) {
                centerY + sin(progressWidth * frequency + phase) * amplitude
            } else centerY

            drawCircle(
                color = thumbColor,
                radius = 7.dp.toPx(),
                center = Offset(progressWidth.coerceIn(7.dp.toPx(), width - 7.dp.toPx()), thumbY)
            )
        }
    }
}
