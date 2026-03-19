package com.vyn.player.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

fun Modifier.bounceClick(
    scaleDown: Float = 0.97f,
    onClick: () -> Unit
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) scaleDown else 1f,
        animationSpec = tween(durationMillis = 80),
        label = "BounceClickScale"
    )
    
    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clickable(
            interactionSource = interactionSource,
            indication = androidx.compose.foundation.LocalIndication.current,
            onClick = onClick
        )
}

@OptIn(ExperimentalFoundationApi::class)
fun Modifier.bounceCombinedClickable(
    scaleDown: Float = 0.97f,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) scaleDown else 1f,
        animationSpec = tween(durationMillis = 80),
        label = "BounceCombinedClickScale"
    )
    
    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .combinedClickable(
            interactionSource = interactionSource,
            indication = androidx.compose.foundation.LocalIndication.current,
            onClick = onClick,
            onLongClick = onLongClick
        )
}

fun Modifier.animateListEntry(
    index: Int = 0,
    delay: Int = 0 
): Modifier = this // No-op, removes composed overhead entirely for maximum list perf

fun Modifier.frostedSurfaceCard(
    shape: Shape,
    elevation: Dp = 12.dp,
    borderWidth: Dp = 0.8.dp,
    borderAlpha: Float = 0.25f,
    lightSurfaceAlpha: Float = 0.96f,
    darkSurfaceAlpha: Float = 0.92f,
    tintAlpha: Float = 0.06f
): Modifier = composed {
    val surfaceBase = MaterialTheme.colorScheme.surface.copy(
        alpha = darkSurfaceAlpha
    )
    val surfaceColor = if (tintAlpha > 0f) {
        MaterialTheme.colorScheme.primary.copy(alpha = tintAlpha).compositeOver(surfaceBase)
    } else {
        surfaceBase
    }
    val borderColor = MaterialTheme.colorScheme.outline.copy(alpha = borderAlpha)
    val shadowElevation = 16.dp

    this
        .shadow(
            elevation = shadowElevation,
            shape = shape,
            ambientColor = Color.Black.copy(alpha = 0.08f),
            spotColor = Color.Black.copy(alpha = 0.18f)
        )
        .clip(shape)
        .background(surfaceColor, shape)
        .border(
            width = borderWidth,
            color = borderColor,
            shape = shape
        )
}

fun Modifier.frostedGlassBar(
    shape: Shape,
    lightColor: Color,
    darkColor: Color,
    borderWidth: Dp = 0.8.dp,
    borderColor: Color = Color.White.copy(alpha = 0.08f),
    elevation: Dp = 8.dp
): Modifier = composed {
    val surfaceColor = darkColor

    this
        .shadow(
            elevation = elevation,
            shape = shape,
            ambientColor = Color.Black.copy(alpha = 0.08f),
            spotColor = Color.Black.copy(alpha = 0.18f)
        )
        .clip(shape)
        .background(surfaceColor, shape)
        .border(
            width = borderWidth,
            color = borderColor,
            shape = shape
        )
}

@Composable
fun balancedBarSurfaceColor(
    lightSurfaceAlpha: Float = 0.96f,
    darkSurfaceAlpha: Float = 0.92f,
    tintAlpha: Float = 0.06f
): Color {
    val surfaceBase = MaterialTheme.colorScheme.surface.copy(
        alpha = darkSurfaceAlpha
    )
    val tintColor = MaterialTheme.colorScheme.primary.copy(alpha = tintAlpha)
    return tintColor.compositeOver(surfaceBase)
}
