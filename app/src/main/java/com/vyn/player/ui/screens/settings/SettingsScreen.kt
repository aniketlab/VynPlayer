package com.vyn.player.ui.screens.settings

import android.content.Intent
import android.net.Uri
import android.widget.Toast
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vyn.player.BuildConfig
import com.vyn.player.R
import com.vyn.player.data.model.GithubRelease
import com.vyn.player.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current

    var showAboutDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showAudioEnhancementDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    val adaptivePadding = getAdaptivePadding()
    val listState = rememberLazyListState()

    LaunchedEffect(uiState.infoMessage) {
        uiState.infoMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearInfoMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
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
                            onClick = { showAudioEnhancementDialog = true }
                        )
                        SettingsDivider()
                        SettingsItem(
                            icon = Icons.Rounded.GraphicEq,
                            iconTint = MaterialTheme.colorScheme.primary,
                            title = "Bass Boost",
                            subtitle = "Enhance low frequencies",
                            onClick = { showAudioEnhancementDialog = true }
                        )
                        SettingsDivider()
                        SettingsItem(
                            icon = Icons.Rounded.SurroundSound,
                            iconTint = MaterialTheme.colorScheme.tertiary,
                            title = "Virtualizer",
                            subtitle = "3D surround sound effect",
                            onClick = { showAudioEnhancementDialog = true }
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
                            subtitle = "Dark Mode",
                            onClick = { }
                        )
                    }
                }
            }

            item {
                SettingsSection("Updates") {
                    SettingsCard {
                        SettingsItem(
                            icon = Icons.Rounded.SystemUpdate,
                            iconTint = MaterialTheme.colorScheme.tertiary,
                            title = "Check for Updates",
                            subtitle = if (uiState.isCheckingForUpdates) {
                                "Checking latest version from server..."
                            } else {
                                "Check latest version from server"
                            },
                            trailing = {
                                if (uiState.isCheckingForUpdates) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Rounded.ChevronRight,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            },
                            onClick = {
                                if (!uiState.isCheckingForUpdates) {
                                    viewModel.checkForUpdates()
                                }
                            }
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
                            title = "VYN Player",
                            subtitle = "Version 1.1 • No Ads • Pure Music",
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
                        text = "VYN Player",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = stringResource(R.string.app_version),
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
                        text = "Aniket Sharma",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        textDecoration = TextDecoration.Underline,
                        modifier = Modifier.clickable {
                            uriHandler.openUri("https://github.com/aniketlab")
                        }
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
                Text(
                    "Privacy & Data Policy",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "VYN Player is designed as a completely offline music experience.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                    )
                    PrivacyPolicySection("Internet Access", "This application does not require internet permission. Your device never sends music data outside your phone.")
                    PrivacyPolicySection("Data Collection", "VYN Player does not collect, store, or transmit any personal data.")
                    PrivacyPolicySection("Advertising", "This application contains no advertisements and integrates no analytics or tracking frameworks.")
                    PrivacyPolicySection("Local Playback", "All audio files are played directly from your device storage.")
                    PrivacyPolicySection("Offline First", "The player is designed to function entirely offline with zero background network activity.")
                }
            },
            confirmButton = {
                TextButton(onClick = { showPrivacyDialog = false }) {
                    Text("Got it", color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // ─── AUDIO ENHANCEMENTS DIALOG ───
    if (showAudioEnhancementDialog) {
        AlertDialog(
            onDismissRequest = { showAudioEnhancementDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(20.dp),
            title = {
                Text("Audio Enhancements", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
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
                        text = "Advanced audio enhancements such as Equalizer, Bass Boost, and Virtualizer will be available in the next major update.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Stay tuned for V2",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showAudioEnhancementDialog = false }) {
                    Text("OK", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    if (uiState.showUpdateDialog && uiState.latestRelease != null) {
        UpdateAvailableDialog(
            release = uiState.latestRelease!!,
            currentVersion = BuildConfig.VERSION_NAME,
            onDismiss = viewModel::dismissUpdateDialog,
            onUpdate = { apkUrl ->
                if (apkUrl.startsWith("https://") && apkUrl.isNotBlank()) {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(apkUrl)))
                    viewModel.dismissUpdateDialog()
                } else {
                    Toast.makeText(context, "Invalid secure APK link", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    if (uiState.showRetryDialog) {
        AlertDialog(
            onDismissRequest = viewModel::dismissRetryDialog,
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(20.dp),
            title = {
                Text(
                    text = "Update Check Failed",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = uiState.updateErrorMessage ?: "Something went wrong while checking for updates.",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.dismissRetryDialog()
                    viewModel.checkForUpdates(forceRefresh = true)
                }) {
                    Text("Retry", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissRetryDialog) {
                    Text("Cancel", fontWeight = FontWeight.Bold)
                }
            }
        )
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
    trailing: @Composable (() -> Unit)? = null,
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
        trailing?.invoke() ?: Icon(
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
private fun PrivacyPolicySection(title: String, description: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
        )
    }
}

@Composable
private fun UpdateAvailableDialog(
    release: GithubRelease,
    currentVersion: String,
    onDismiss: () -> Unit,
    onUpdate: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text(
                text = "Update Available 🚀",
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Current version: $currentVersion",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                )
                Text(
                    text = "Latest version: ${release.tag_name}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Changelog",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = release.body.ifBlank { "No changelog provided." },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onUpdate(release.apkUrl) }) {
                Text("Update", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", fontWeight = FontWeight.Bold)
            }
        }
    )
}






