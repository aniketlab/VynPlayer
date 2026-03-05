package com.muzic.player.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.muzic.player.ui.screens.library.LibraryViewModel
import com.muzic.player.ui.theme.*

@Composable
fun ProfileScreen(
    onNavigateToFavorites: () -> Unit = {},
    onNavigateToRecentlyPlayed: () -> Unit = {},
    onNavigateToPlaylists: () -> Unit = {},
    viewModel: LibraryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showEditDialog by remember { mutableStateOf(false) }

    val totalSongs = uiState.songs.size
    val totalAlbums = uiState.albums.size
    val totalArtists = uiState.artists.size

    // Listening time from play count: approximate 3 mins per play
    val totalPlays = uiState.topSongs.sumOf { it.playCount }
    val hours = (totalPlays * 3) / 60

    // Helper to check if artist name is valid (not a website)
    fun isValidArtist(artist: String): Boolean {
        val lower = artist.lowercase()
        return !lower.contains(".com") && 
               !lower.contains(".in") && 
               !lower.contains(".net") && 
               !lower.contains(".org") && 
               !lower.contains("http") &&
               !lower.contains("www.") &&
               artist.isNotBlank() &&
               artist != "<unknown>"
    }

    // Compute real listening activity
    val topArtist = remember(uiState.topSongs, uiState.songs) {
        if (uiState.topSongs.isNotEmpty() && uiState.songs.isNotEmpty()) {
            val topSongIds = uiState.topSongs.map { it.songId }.toSet()
            val playedSongs = uiState.songs.filter { it.id in topSongIds }
            playedSongs.groupBy { it.artist }
                .filter { isValidArtist(it.key) }
                .mapValues { entry ->
                    entry.value.sumOf { song ->
                        uiState.topSongs.find { it.songId == song.id }?.playCount ?: 0
                    }
                }
                .maxByOrNull { it.value }?.key ?: "Play some songs!"
        } else "Play some songs!"
    }

    val topSongName = remember(uiState.topSongs, uiState.songs) {
        if (uiState.topSongs.isNotEmpty()) {
            val topId = uiState.topSongs.first().songId
            uiState.songs.find { it.id == topId }?.title ?: "Unknown"
        } else "Play some songs!"
    }

    val adaptivePadding = getAdaptivePadding()
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding(),
        contentPadding = PaddingValues(bottom = 144.dp)
    ) {

        // ─── HEADER ───
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 40.dp, bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Avatar
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                        .clickable { showEditDialog = true },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = getAvatarEmoji(uiState.userAvatarUrl),
                        fontSize = 48.sp
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = 6.dp, end = 6.dp),
                        contentAlignment = Alignment.BottomEnd
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                                .padding(6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Edit,
                                contentDescription = "Edit Avatar",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = uiState.userDisplayName,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = uiState.userSubtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.padding(top = 4.dp)
                )

                Text(
                    text = "Listening since 2026",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }

        // ─── STATISTICS ───
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = adaptivePadding)
                    .padding(bottom = 24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(24.dp),
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        StatItem(title = "Songs", value = "$totalSongs")
                        StatItem(title = "Albums", value = "$totalAlbums")
                        StatItem(title = "Artists", value = "$totalArtists")
                        StatItem(title = "Total Listening", value = "${hours}h")
                    }
                }
            }
        }

        // ─── QUICK ACCESS ───
        item {
            Column(
                modifier = Modifier.padding(horizontal = adaptivePadding)
            ) {
                Text(
                    text = "Quick Access",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                QuickAccessItem(
                    icon = Icons.Rounded.Favorite,
                    title = "Favorites",
                    subtitle = "Your liked songs",
                    iconTint = MaterialTheme.colorScheme.primary,
                    onClick = onNavigateToFavorites
                )
                Spacer(modifier = Modifier.height(12.dp))

                QuickAccessItem(
                    icon = Icons.Rounded.History,
                    title = "Recently Played",
                    subtitle = "Pickup where you left off",
                    iconTint = MaterialTheme.colorScheme.secondary,
                    onClick = {
                        if (uiState.recentSongs.isEmpty()) {
                            android.widget.Toast.makeText(context, "No recently played songs yet", android.widget.Toast.LENGTH_SHORT).show()
                        } else {
                            onNavigateToRecentlyPlayed()
                        }
                    }
                )
                Spacer(modifier = Modifier.height(12.dp))

                QuickAccessItem(
                    icon = Icons.Rounded.PlaylistPlay,
                    title = "Your Playlists",
                    subtitle = "Your custom collections",
                    iconTint = MaterialTheme.colorScheme.tertiary,
                    onClick = onNavigateToPlaylists
                )
            }
        }

        // ─── LISTENING ACTIVITY ───
        item {
            Column(
                modifier = Modifier
                    .padding(horizontal = adaptivePadding)
                    .padding(top = 24.dp)
            ) {
                Text(
                    text = "Listening Activity",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(20.dp),
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp)
                    ) {
                        ActivityStatItem(label = "Top Artist", value = topArtist)
                        Spacer(modifier = Modifier.height(16.dp))
                        ActivityStatItem(label = "Top Song", value = topSongName)
                        Spacer(modifier = Modifier.height(16.dp))
                        ActivityStatItem(label = "Total Plays", value = "$totalPlays")
                    }
                }
            }
        }
    }

    // Edit Profile Dialog
    if (showEditDialog) {
        EditProfileDialog(
            currentName = uiState.userDisplayName,
            currentSubtitle = uiState.userSubtitle,
            currentAvatarUrl = uiState.userAvatarUrl,
            onDismissRequest = { showEditDialog = false },
            onSaveProfile = { name, subtitle, avatarUrl ->
                viewModel.updateUserProfile(name, subtitle, avatarUrl)
            }
        )
    }
}

@Composable
fun ActivityStatItem(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
fun StatItem(title: String, value: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = title,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
fun QuickAccessItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    iconTint: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = iconTint,
                modifier = Modifier.size(24.dp)
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }

        Icon(
            imageVector = Icons.Rounded.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
            modifier = Modifier.size(24.dp)
        )
    }
}
