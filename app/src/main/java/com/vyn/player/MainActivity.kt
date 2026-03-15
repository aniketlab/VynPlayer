package com.vyn.player

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.vyn.player.data.repository.ArtworkRepository
import com.vyn.player.ui.MuzicAppContent
import com.vyn.player.ui.components.LocalArtworkRepository
import com.vyn.player.ui.theme.MuzicTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.util.UnstableApi
import com.vyn.player.player.PlaybackManager
import com.vyn.player.data.preferences.UserPreferencesManager
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.media.MediaMetadataRetriever
import com.vyn.player.data.model.Song

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var userPreferencesManager: UserPreferencesManager

    @Inject
    lateinit var artworkRepository: ArtworkRepository

    @Inject
    lateinit var playbackManager: PlaybackManager

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val themeMode by userPreferencesManager.themeMode.collectAsState(initial = 0)
            val isDarkTheme = when (themeMode) {
                1 -> false
                2 -> true
                else -> isSystemInDarkTheme()
            }

            MuzicTheme(darkTheme = isDarkTheme) {
                CompositionLocalProvider(LocalArtworkRepository provides artworkRepository) {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        MuzicAppContent()
                    }
                }
            }
        }

        handleExternalAudioIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleExternalAudioIntent(intent)
    }

    private fun handleExternalAudioIntent(intent: Intent?) {
        if (intent == null) return

        val uris = extractExternalAudioUris(intent)
        if (uris.isEmpty()) return

        persistReadableUris(uris, intent.flags)

        lifecycleScope.launch {
            val songs = withContext(Dispatchers.IO) {
                uris.mapNotNull { uri -> buildExternalSong(uri) }
            }

            when {
                songs.isEmpty() -> {
                    Toast.makeText(this@MainActivity, "Unsupported audio format", Toast.LENGTH_SHORT).show()
                }
                else -> {
                    playbackManager.playTemporaryQueue(songs, startIndex = 0)
                    intent.action = null
                    intent.data = null
                    intent.clipData = null
                }
            }
        }
    }

    private fun extractExternalAudioUris(intent: Intent): List<Uri> {
        val uris = LinkedHashSet<Uri>()

        when (intent.action) {
            Intent.ACTION_VIEW -> {
                intent.data?.let(uris::add)
            }
            Intent.ACTION_SEND -> {
                intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)?.let(uris::add)
                    ?: intent.data?.let(uris::add)
            }
            Intent.ACTION_SEND_MULTIPLE -> {
                intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM, Uri::class.java)
                    ?.forEach(uris::add)
            }
        }

        val clipData = intent.clipData
        if (clipData != null) {
            for (index in 0 until clipData.itemCount) {
                clipData.getItemAt(index).uri?.let(uris::add)
            }
        }

        return uris.toList()
    }

    private fun persistReadableUris(uris: List<Uri>, intentFlags: Int) {
        val permissionFlags = intentFlags and (Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        uris.forEach { uri ->
            if (uri.scheme != "content") return@forEach
            try {
                contentResolver.takePersistableUriPermission(uri, permissionFlags)
            } catch (_: SecurityException) {
                // Provider may not support persistable permissions.
            } catch (_: IllegalArgumentException) {
                // Ignore non-persistable content providers.
            }
        }
    }

    private fun buildExternalSong(uri: Uri): Song? {
        return try {
            val metadataRetriever = MediaMetadataRetriever()
            metadataRetriever.setDataSource(this, uri)

            val title = metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
            val artist = metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
            val album = metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
            val duration = metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            val mimeType = contentResolver.getType(uri).orEmpty()
            val displayName = resolveDisplayName(uri)
            metadataRetriever.release()

            Song(
                id = -1L * ((uri.toString().hashCode().toLong().let { if (it == 0L) 1L else kotlin.math.abs(it) })),
                title = title?.takeIf { it.isNotBlank() } ?: displayName,
                artist = artist?.takeIf { it.isNotBlank() } ?: "Unknown Artist",
                album = album?.takeIf { it.isNotBlank() } ?: "External Audio",
                albumId = -1L,
                duration = duration,
                path = "",
                uri = uri,
                mimeType = mimeType,
                folderName = "",
                folderPath = "",
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun resolveDisplayName(uri: Uri): String {
        if (uri.scheme == "file") {
            return uri.lastPathSegment?.substringAfterLast('/')?.ifBlank { "External Audio" } ?: "External Audio"
        }

        return runCatching {
            contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0 && cursor.moveToFirst()) {
                    cursor.getString(nameIndex)
                } else null
            }
        }.getOrNull()?.ifBlank { null } ?: (uri.lastPathSegment?.substringAfterLast('/') ?: "External Audio")
    }
}
