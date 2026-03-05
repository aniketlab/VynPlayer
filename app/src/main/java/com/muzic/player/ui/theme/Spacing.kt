package com.muzic.player.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun getAdaptivePadding(): Dp {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    return when {
        screenWidth < 400.dp -> 12.dp
        screenWidth < 600.dp -> 16.dp
        screenWidth < 720.dp -> 20.dp
        else -> 24.dp
    }
}

@Composable
fun getResponsiveColumns(minWidth: Dp = 160.dp): Int {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    return (screenWidth / minWidth).toInt().coerceAtLeast(2)
}
