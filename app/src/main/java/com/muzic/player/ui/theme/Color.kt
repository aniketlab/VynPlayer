package com.muzic.player.ui.theme

import androidx.compose.ui.graphics.Color

// ─── Primary Brand Colors ───
val MuzicRed = Color(0xFFFC3C44)           // Apple Music inspired red accent
val MuzicRedDark = Color(0xFFD42F36)
val MuzicPink = Color(0xFFFF2D55)          // Vibrant pink for highlights
val MuzicGradientStart = Color(0xFFFA233B)
val MuzicGradientEnd = Color(0xFFFC3C44)

// ─── Backgrounds ───
val DarkBg = Color(0xFF000000)             // Pure black for AMOLED
val DarkSurface = Color(0xFF1C1C1E)        // iOS-style dark surface
val DarkSurfaceElevated = Color(0xFF2C2C2E) // Elevated cards
val DarkSurfaceSecondary = Color(0xFF3A3A3C) // Secondary surfaces
val DarkSurfaceTertiary = Color(0xFF48484A)  // Tertiary
val CardBackground = Color(0xFF1C1C1E)
val CardBackgroundHover = Color(0xFF2C2C2E)

// ─── Text Colors ───
val TextPrimary = Color(0xFFFFFFFF)        // Pure white text
val TextSecondary = Color(0xFF8E8E93)      // iOS secondary gray
val TextTertiary = Color(0xFF636366)       // Muted text
val TextOnAccent = Color(0xFFFFFFFF)

// ─── Accent & Functional ───
val AccentBlue = Color(0xFF0A84FF)         // iOS blue
val AccentGreen = Color(0xFF30D158)        // Success green
val AccentOrange = Color(0xFFFF9F0A)       // Warning orange
val AccentPurple = Color(0xFFBF5AF2)       // Purple accent
val FavoriteRed = Color(0xFFFC3C44)

// ─── Dividers & Separators ───
val DividerColor = Color(0xFF38383A)
val SeparatorColor = Color(0xFF545458).copy(alpha = 0.6f)

// ─── Glassmorphism ───
val GlassWhite = Color(0xFFFFFFFF).copy(alpha = 0.08f)
val GlassBorder = Color(0xFFFFFFFF).copy(alpha = 0.12f)
val GlassOverlay = Color(0xFF000000).copy(alpha = 0.4f)

// ─── Now Playing ───
val NowPlayingBg = Color(0xFF000000)
val NowPlayingOverlay = Color(0xFF1C1C1E).copy(alpha = 0.85f)

// ─── Mini Player ───
val MiniPlayerBg = Color(0xFF1C1C1E)
val MiniPlayerBorder = Color(0xFF38383A)

// ─── Tab Bar ───
val TabActive = Color(0xFFFC3C44)
val TabInactive = Color(0xFF8E8E93)

// ─── Backward Compatibility Aliases ───
val SoftWhite = TextPrimary
val ElectricPurple = MuzicRed
val ElectricPurpleLight = MuzicPink
val DeepIndigo = Color(0xFF1C1C1E)
val DeepIndigoLight = DarkSurfaceElevated
val DarkCharcoal = DarkBg
val DarkSurfaceVariant = DarkSurfaceElevated
val WarningOrange = AccentOrange
val ErrorRed = MuzicRed
