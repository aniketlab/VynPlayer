package com.muzic.player.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Settings",
                        color = SoftWhite,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Rounded.ArrowBack,
                            contentDescription = "Back",
                            tint = SoftWhite
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkCharcoal
                )
            )
        },
        containerColor = DarkCharcoal
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // Library section
            SettingsSectionHeader("Library")

            SettingsItem(
                icon = Icons.Rounded.Refresh,
                title = "Rescan Library",
                subtitle = if (uiState.isScanning) "Scanning..." else "Scan device for new music",
                onClick = { viewModel.rescanLibrary() }
            )

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                color = DividerColor
            )

            // Audio section
            SettingsSectionHeader("Audio")

            SettingsItem(
                icon = Icons.Rounded.Equalizer,
                title = "Equalizer",
                subtitle = "10-band equalizer (Coming in V2)",
                onClick = { }
            )

            SettingsItem(
                icon = Icons.Rounded.GraphicEq,
                title = "Bass Boost",
                subtitle = "Enhance bass frequencies (Coming in V2)",
                onClick = { }
            )

            SettingsItem(
                icon = Icons.Rounded.SurroundSound,
                title = "Virtualizer",
                subtitle = "Surround sound effect (Coming in V2)",
                onClick = { }
            )

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                color = DividerColor
            )

            // Appearance section
            SettingsSectionHeader("Appearance")

            SettingsItem(
                icon = Icons.Rounded.DarkMode,
                title = "Theme",
                subtitle = "Dark mode (default)",
                onClick = { }
            )

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                color = DividerColor
            )

            // About section
            SettingsSectionHeader("About")

            SettingsItem(
                icon = Icons.Rounded.Info,
                title = "Muzic",
                subtitle = "Version 1.0.0 • Pure Sound. No Noise.",
                onClick = { }
            )

            SettingsItem(
                icon = Icons.Rounded.Security,
                title = "Privacy",
                subtitle = "No tracking • No ads • No analytics • Fully offline",
                onClick = { }
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = ElectricPurple,
        letterSpacing = 2.sp,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
    )
}

@Composable
private fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = TextSecondary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = SoftWhite
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }
        Icon(
            imageVector = Icons.Rounded.ChevronRight,
            contentDescription = null,
            tint = TextTertiary,
            modifier = Modifier.size(20.dp)
        )
    }
}
