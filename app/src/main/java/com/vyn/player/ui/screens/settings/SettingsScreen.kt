package com.vyn.player.ui.screens.settings

import android.content.Context
import android.content.Intent
import android.media.audiofx.AudioEffect
import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.*
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
import com.vyn.player.ui.theme.*

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
    
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()

    val adaptivePadding = getAdaptivePadding()
    val listState = rememberLazyListState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Settings",
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        com.vyn.player.ui.components.ObserveScrollState(listState)
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            state = listState
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
            }

            // ─── LIBRARY ───
            item {
                SettingsSection("Library") {
                    SettingsCard {
                        SettingsItem(
                            icon = Icons.Rounded.Refresh,
                            iconTint = MaterialTheme.colorScheme.secondary,
                            title = "Rescan Library",
                            subtitle = if (uiState.isScanning) "Scanning for new music..." else if (uiState.scanMessage.isNotEmpty()) uiState.scanMessage else "Scan device for new music",
                            onClick = {
                                viewModel.rescanLibrary()
                                Toast.makeText(context, "🔄 Scanning library...", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            }

            // ─── AUDIO ───
            item {
                SettingsSection("Audio") {
                    SettingsCard {
                        SettingsItem(
                            icon = Icons.Rounded.Equalizer,
                            iconTint = MaterialTheme.colorScheme.primary,
                            title = "Equalizer",
                            subtitle = "Adjust audio frequencies",
                            onClick = { openSystemEqualizer(context) }
                        )
                        SettingsDivider()
                        SettingsItem(
                            icon = Icons.Rounded.GraphicEq,
                            iconTint = MaterialTheme.colorScheme.primary,
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
                            iconTint = MaterialTheme.colorScheme.tertiary,
                            title = "Virtualizer",
                            subtitle = "3D surround sound effect",
                            onClick = {
                                comingSoonFeature = "Virtualizer"
                                showComingSoonDialog = true
                            }
                        )
                    }
                }
            }

            // ─── APPEARANCE ───
            item {
                SettingsSection("Appearance") {
                    SettingsCard {
                        SettingsItem(
                            icon = Icons.Rounded.DarkMode,
                            iconTint = MaterialTheme.colorScheme.secondary,
                            title = "Theme",
                            subtitle = when (themeMode) {
                                1 -> "Light Mode"
                                2 -> "Dark Mode"
                                else -> "System Default"
                            },
                            onClick = { showThemeDialog = true }
                        )
                    }
                }
            }

            // ─── ABOUT ───
            item {
                SettingsSection("About") {
                    SettingsCard {
                        SettingsItem(
                            icon = Icons.Rounded.Info,
                            iconTint = MaterialTheme.colorScheme.primary,
                            title = "About VYN PLAYER",
                            subtitle = "Version 2.1.0 • No Ads • Pure Music",
                            onClick = { showAboutDialog = true }
                        )
                        SettingsDivider()
                        SettingsItem(
                            icon = Icons.Rounded.Security,
                            iconTint = MaterialTheme.colorScheme.tertiary,
                            title = "Privacy Policy",
                            subtitle = "Your data is safe with us",
                            onClick = { showPrivacyDialog = true }
                        )
                        SettingsDivider()
                        SettingsItem(
                            icon = Icons.Rounded.SettingsApplications,
                            iconTint = MaterialTheme.colorScheme.secondary,
                            title = "App Info",
                            subtitle = "System app settings",
                            onClick = { 
                                val intent = android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                                intent.data = android.net.Uri.parse("package:${context.packageName}")
                                context.startActivity(intent)
                            }
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(144.dp))
            }
        }
    }

    // ─── ABOUT DIALOG ───
    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
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
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primary,
                                        MaterialTheme.colorScheme.primaryContainer
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.MusicNote,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(40.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "VYN PLAYER",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = "Version 2.1.0",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Features badges
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FeatureBadge("No Ads", MaterialTheme.colorScheme.primary)
                        FeatureBadge("Offline", MaterialTheme.colorScheme.secondary)
                        FeatureBadge("Pure Music", MaterialTheme.colorScheme.tertiary)
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = "A premium offline music player crafted for audiophiles who value clean design and pure sound quality.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Developed by",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                    Text(
                        text = "officialtechrom",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) {
                    Text("Close", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // ─── PRIVACY DIALOG ───
    if (showPrivacyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(20.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Shield,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Your Privacy is Safe",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    PrivacyItem("🚫", "No Internet Access", "VYN PLAYER has no internet permission. Your data never leaves your device.")
                    PrivacyItem("🔒", "No Data Collection", "We don't collect, store, or share any personal information.")
                    PrivacyItem("📵", "No Ads or Trackers", "Zero advertising SDKs. Zero analytics. Zero tracking.")
                    PrivacyItem("🎵", "Music Stays Local", "Your music files are read directly from your device storage.")
                    PrivacyItem("✅", "100% Offline", "VYN PLAYER works completely without internet. Always.")
                }
            },
            confirmButton = {
                TextButton(onClick = { showPrivacyDialog = false }) {
                    Text("Got it!", color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(20.dp),
            title = {
                Text("Select Theme", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
            },
            text = {
                Column {
                    ThemeOption(
                        title = "System Default", 
                        subtitle = "Follows device theme", 
                        isSelected = themeMode == 0,
                        onClick = {
                            viewModel.setThemeMode(0)
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    ThemeOption(
                        title = "Light Mode", 
                        subtitle = "A bright interface", 
                        isSelected = themeMode == 1,
                        onClick = {
                            viewModel.setThemeMode(1)
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    ThemeOption(
                        title = "Dark Mode", 
                        subtitle = "AMOLED optimized", 
                        isSelected = themeMode == 2,
                        onClick = {
                            viewModel.setThemeMode(2)
                        }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemeDialog = false }) {
                    Text("Close", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // ─── COMING SOON DIALOG ───
    if (showComingSoonDialog) {
        AlertDialog(
            onDismissRequest = { showComingSoonDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(20.dp),
            title = {
                Text(comingSoonFeature, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Icon(
                        imageVector = Icons.Rounded.Rocket,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "$comingSoonFeature will be available in the next update!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Stay tuned for V2 🎵",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showComingSoonDialog = false }) {
                    Text("OK", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
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
    val adaptivePadding = getAdaptivePadding()
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        letterSpacing = 1.5.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(start = adaptivePadding + 16.dp, top = 20.dp, bottom = 8.dp)
    )
    content()
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    val adaptivePadding = getAdaptivePadding()
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = adaptivePadding),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
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
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
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
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                fontSize = 12.sp
            )
        }
        Icon(
            imageVector = Icons.Rounded.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
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
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
private fun ThemeOption(title: String, subtitle: String, isSelected: Boolean, onClick: () -> Unit = {}) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Medium)
                Text(subtitle, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), style = MaterialTheme.typography.bodySmall)
            }
            if (isSelected) {
                Icon(
                    imageVector = Icons.Rounded.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
