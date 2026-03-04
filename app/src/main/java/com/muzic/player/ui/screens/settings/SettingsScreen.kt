package com.muzic.player.ui.screens.settings

import android.content.Context
import android.content.Intent
import android.media.audiofx.AudioEffect
import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.muzic.player.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var showAboutDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showComingSoonDialog by remember { mutableStateOf(false) }
    var comingSoonFeature by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Settings",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 28.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Rounded.ArrowBack,
                            contentDescription = "Back",
                            tint = MuzicRed
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkBg
                )
            )
        },
        containerColor = DarkBg
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // ─── LIBRARY ───
            SettingsSection("Library") {
                SettingsCard {
                    SettingsItem(
                        icon = Icons.Rounded.Refresh,
                        iconTint = AccentBlue,
                        title = "Rescan Library",
                        subtitle = if (uiState.isScanning) "Scanning for new music..." else if (uiState.scanMessage.isNotEmpty()) uiState.scanMessage else "Scan device for new music",
                        onClick = {
                            viewModel.rescanLibrary()
                            Toast.makeText(context, "🔄 Scanning library...", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }

            // ─── AUDIO ───
            SettingsSection("Audio") {
                SettingsCard {
                    SettingsItem(
                        icon = Icons.Rounded.Equalizer,
                        iconTint = AccentPurple,
                        title = "Equalizer",
                        subtitle = "Adjust audio frequencies",
                        onClick = { openSystemEqualizer(context) }
                    )
                    SettingsDivider()
                    SettingsItem(
                        icon = Icons.Rounded.GraphicEq,
                        iconTint = AccentOrange,
                        title = "Bass Boost",
                        subtitle = "Enhance low frequencies",
                        onClick = {
                            comingSoonFeature = "Bass Boost"
                            showComingSoonDialog = true
                        }
                    )
                    SettingsDivider()
                    SettingsItem(
                        icon = Icons.Rounded.SurroundSound,
                        iconTint = AccentGreen,
                        title = "Virtualizer",
                        subtitle = "3D surround sound effect",
                        onClick = {
                            comingSoonFeature = "Virtualizer"
                            showComingSoonDialog = true
                        }
                    )
                }
            }

            // ─── APPEARANCE ───
            SettingsSection("Appearance") {
                SettingsCard {
                    SettingsItem(
                        icon = Icons.Rounded.DarkMode,
                        iconTint = AccentBlue,
                        title = "Theme",
                        subtitle = "Dark Mode",
                        onClick = { showThemeDialog = true }
                    )
                }
            }

            // ─── ABOUT ───
            SettingsSection("About") {
                SettingsCard {
                    SettingsItem(
                        icon = Icons.Rounded.Info,
                        iconTint = MuzicRed,
                        title = "About Muzic",
                        subtitle = "Version 2.1.0 • No Ads • Pure Music",
                        onClick = { showAboutDialog = true }
                    )
                    SettingsDivider()
                    SettingsItem(
                        icon = Icons.Rounded.Security,
                        iconTint = AccentGreen,
                        title = "Privacy Policy",
                        subtitle = "Your data is safe with us",
                        onClick = { showPrivacyDialog = true }
                    )
                    SettingsDivider()
                    SettingsItem(
                        icon = Icons.Rounded.Code,
                        iconTint = AccentPurple,
                        title = "Developer",
                        subtitle = "Made by officialtechrom",
                        onClick = {
                            Toast.makeText(context, "Made with ❤️ by officialtechrom", Toast.LENGTH_LONG).show()
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }

    // ─── ABOUT DIALOG ───
    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            containerColor = DarkSurface,
            shape = RoundedCornerShape(20.dp),
            title = null,
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // App icon
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(MuzicGradientStart, MuzicGradientEnd)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.MusicNote,
                            contentDescription = null,
                            tint = TextPrimary,
                            modifier = Modifier.size(40.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Muzic",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    Text(
                        text = "Version 2.1.0",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Features badges
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FeatureBadge("No Ads", MuzicRed)
                        FeatureBadge("Offline", AccentBlue)
                        FeatureBadge("Pure Music", AccentGreen)
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = "A premium offline music player crafted for audiophiles who value clean design and pure sound quality.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    HorizontalDivider(color = DividerColor)

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Developed by",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextTertiary
                    )
                    Text(
                        text = "officialtechrom",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MuzicRed
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) {
                    Text("Close", color = MuzicRed, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // ─── PRIVACY DIALOG ───
    if (showPrivacyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyDialog = false },
            containerColor = DarkSurface,
            shape = RoundedCornerShape(20.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Shield,
                        contentDescription = null,
                        tint = AccentGreen,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Your Privacy is Safe",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    PrivacyItem("🚫", "No Internet Access", "Muzic has no internet permission. Your data never leaves your device.")
                    PrivacyItem("🔒", "No Data Collection", "We don't collect, store, or share any personal information.")
                    PrivacyItem("📵", "No Ads or Trackers", "Zero advertising SDKs. Zero analytics. Zero tracking.")
                    PrivacyItem("🎵", "Music Stays Local", "Your music files are read directly from your device storage.")
                    PrivacyItem("✅", "100% Offline", "Muzic works completely without internet. Always.")
                }
            },
            confirmButton = {
                TextButton(onClick = { showPrivacyDialog = false }) {
                    Text("Got it!", color = AccentGreen, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // ─── THEME DIALOG ───
    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            containerColor = DarkSurface,
            shape = RoundedCornerShape(20.dp),
            title = {
                Text("Theme", color = TextPrimary, fontWeight = FontWeight.Bold)
            },
            text = {
                Column {
                    ThemeOption("Dark Mode", "AMOLED black", true)
                    Spacer(modifier = Modifier.height(8.dp))
                    ThemeOption("Light Mode", "Coming soon", false)
                    Spacer(modifier = Modifier.height(8.dp))
                    ThemeOption("Dynamic Colors", "Coming soon", false)
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemeDialog = false }) {
                    Text("Done", color = MuzicRed, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // ─── COMING SOON DIALOG ───
    if (showComingSoonDialog) {
        AlertDialog(
            onDismissRequest = { showComingSoonDialog = false },
            containerColor = DarkSurface,
            shape = RoundedCornerShape(20.dp),
            title = {
                Text(comingSoonFeature, color = TextPrimary, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Icon(
                        imageVector = Icons.Rounded.Rocket,
                        contentDescription = null,
                        tint = AccentOrange,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "$comingSoonFeature will be available in the next update!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Stay tuned for V2 🎵",
                        style = MaterialTheme.typography.bodySmall,
                        color = AccentOrange,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showComingSoonDialog = false }) {
                    Text("OK", color = MuzicRed, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

// ─── System Equalizer ───
private fun openSystemEqualizer(context: Context) {
    try {
        val intent = Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL)
        intent.putExtra(AudioEffect.EXTRA_AUDIO_SESSION, 0)
        intent.putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC)
        if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
        } else {
            Toast.makeText(context, "No equalizer app found on this device", Toast.LENGTH_SHORT).show()
        }
    } catch (e: Exception) {
        Toast.makeText(context, "Could not open equalizer", Toast.LENGTH_SHORT).show()
    }
}

// ─── Composable Helpers ───

@Composable
private fun SettingsSection(title: String, content: @Composable () -> Unit) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = TextSecondary,
        letterSpacing = 1.5.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(start = 32.dp, top = 20.dp, bottom = 8.dp)
    )
    content()
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(14.dp),
        color = DarkSurface
    ) {
        Column {
            content()
        }
    }
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 56.dp),
        color = DividerColor,
        thickness = 0.5.dp
    )
}

@Composable
private fun SettingsItem(
    icon: ImageVector,
    iconTint: androidx.compose.ui.graphics.Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(iconTint.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = TextPrimary,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                fontSize = 12.sp
            )
        }
        Icon(
            imageVector = Icons.Rounded.ChevronRight,
            contentDescription = null,
            tint = TextTertiary,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun FeatureBadge(text: String, color: androidx.compose.ui.graphics.Color) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = color.copy(alpha = 0.15f)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun PrivacyItem(emoji: String, title: String, description: String) {
    Row {
        Text(emoji, fontSize = 20.sp)
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }
    }
}

@Composable
private fun ThemeOption(title: String, subtitle: String, isSelected: Boolean) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) MuzicRed.copy(alpha = 0.12f) else DarkSurfaceElevated
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = TextPrimary, fontWeight = FontWeight.Medium)
                Text(subtitle, color = TextSecondary, style = MaterialTheme.typography.bodySmall)
            }
            if (isSelected) {
                Icon(
                    imageVector = Icons.Rounded.CheckCircle,
                    contentDescription = null,
                    tint = MuzicRed,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
