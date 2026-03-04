package com.muzic.player.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val MuzicDarkColorScheme = darkColorScheme(
    primary = ElectricPurple,
    onPrimary = Color.White,
    primaryContainer = DeepIndigo,
    onPrimaryContainer = SoftWhite,
    secondary = ElectricPurpleLight,
    onSecondary = Color.Black,
    secondaryContainer = DeepIndigoLight,
    onSecondaryContainer = SoftWhite,
    tertiary = ElectricPurpleDark,
    onTertiary = Color.White,
    background = DarkCharcoal,
    onBackground = SoftWhite,
    surface = DarkSurface,
    onSurface = SoftWhite,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = TextTertiary,
    outlineVariant = DividerColor,
    error = ErrorRed,
    onError = Color.White,
    inverseSurface = SoftWhite,
    inverseOnSurface = DarkCharcoal
)

@Composable
fun MuzicTheme(content: @Composable () -> Unit) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = false
                isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = MuzicDarkColorScheme,
        typography = MuzicTypography,
        content = content
    )
}
