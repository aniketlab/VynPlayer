package com.vyn.player.ui.components

import android.media.MediaMetadataRetriever
import android.util.LruCache
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.vyn.player.data.model.Song
import com.vyn.player.data.repository.ArtworkRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.absoluteValue

/**
 * In-memory cache: song.path -> File/ByteArray/null
 * "NONE" means we already checked and found nothing.
 */
object ArtworkModelCache {
    private val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
    private val cacheSize = maxMemory / 8

    val cache = object : LruCache<String, Any>(cacheSize) {
        override fun sizeOf(key: String, value: Any): Int {
            return when (value) {
                is ByteArray -> value.size / 1024
                else -> 1
            }
        }
    }

    fun invalidateNoneEntries() {
        val snapshot = cache.snapshot()
        for ((key, value) in snapshot) {
            if (value === "NONE") cache.remove(key)
        }
    }
}

// Tracks in-flight extractions so we don't double-launch for the same song
private val inFlightKeys = ConcurrentHashMap.newKeySet<String>()

/**
 * Extract artwork for a song — pure background, never blocks UI.
 * Returns the resolved model (File or ByteArray) or null.
 * Results are stored in ArtworkModelCache permanently.
 */
suspend fun extractArtworkModel(
    song: Song,
    context: android.content.Context,
    artworkRepo: ArtworkRepository? = null,
    thumbnailMode: Boolean = false
): Any? = withContext(Dispatchers.IO) {
    val cacheKey = song.path
    if (cacheKey.isEmpty()) return@withContext null

    val cached = ArtworkModelCache.cache.get(cacheKey)
    if (cached != null) {
        return@withContext if (cached === "NONE") null else cached
    }

    // Only one coroutine per song at a time
    if (!inFlightKeys.add(cacheKey)) return@withContext null

    val thumbCacheDir = File(context.cacheDir, "artwork_thumbs")
    if (!thumbCacheDir.exists()) thumbCacheDir.mkdirs()
    val cacheFile = File(thumbCacheDir, "${song.id}_thumb.jpg")

    // Check disk cache first
    if (cacheFile.exists() && cacheFile.length() > 0) {
        inFlightKeys.remove(cacheKey)
        ArtworkModelCache.cache.put(cacheKey, cacheFile)
        return@withContext cacheFile
    }

    try {
        var result: Any? = null

        // Priority 1: artworkUrl from DB (previously downloaded)
        if (song.artworkUrl != null) {
            val file = File(song.artworkUrl)
            if (file.exists()) {
                if (thumbnailMode) {
                    val thumb = File(file.parentFile, "${file.nameWithoutExtension}_thumb.${file.extension}")
                    if (thumb.exists()) result = thumb
                }
                if (result == null) result = file
            }
        }

        // Priority 2: embedded artwork (MediaMetadataRetriever — heavy, only if needed)
        if (result == null) {
            try {
                val retriever = MediaMetadataRetriever()
                retriever.setDataSource(song.path)
                val picture = retriever.embeddedPicture
                retriever.release()
                if (picture != null) {
                    try {
                        cacheFile.writeBytes(picture)
                        result = cacheFile
                    } catch (e: Exception) {
                        result = picture
                    }
                }
            } catch (_: Exception) {}
        }

        // Priority 3: folder cover image
        if (result == null && song.folderPath.isNotBlank()) {
            val folder = File(song.folderPath)
            if (folder.exists() && folder.isDirectory) {
                val names = listOf("cover.jpg", "folder.jpg", "album.jpg", "front.jpg", "cover.png", "folder.png")
                for (name in names) {
                    val f = File(folder, name)
                    if (f.exists()) { result = f; break }
                }
            }
        }

        // Priority 4: network-cached artwork on disk
        if (result == null && artworkRepo != null) {
            val f = artworkRepo.getCachedAlbumArtwork(song.album, song.artist, thumbnailMode)
            if (f != null) result = f
        }

        ArtworkModelCache.cache.put(cacheKey, result ?: "NONE")
        result
    } finally {
        inFlightKeys.remove(cacheKey)
    }
}

@Composable
fun SharedArtworkImage(
    song: Song?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 20.dp,
    iconSize: Dp = 24.dp,
    elevation: Dp = 2.dp,
    surfaceColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    fallbackBrush: Brush? = null,
    thumbnailMode: Boolean = true,
    isScrolling: Boolean = false
) {
    val artworkRepository = LocalArtworkRepository.current
    val context = LocalContext.current

    // ── 1. Synchronous memory-cache read (no coroutine, no delay) ──────────
    val initialModel = remember(song?.id) {
        if (song == null) return@remember null
        val cached = ArtworkModelCache.cache.get(song.path)
        if (cached != null && cached !== "NONE") cached else null
    }

    // ── 2. Async state — only updated in background coroutine ──────────────
    var activeModel by remember(song?.id) { mutableStateOf<Any?>(initialModel) }

    // Launch exactly ONE background extraction per song (keyed by song.id).
    // This coroutine is cancelled automatically when the composable leaves composition.
    LaunchedEffect(song?.id) {
        if (song == null) return@LaunchedEffect
        // If we already have it, nothing to do
        if (activeModel != null) return@LaunchedEffect
        // Also skip if cache already says NONE
        val cached = ArtworkModelCache.cache.get(song.path)
        if (cached === "NONE") return@LaunchedEffect

        // Run extraction off the main thread — never blocks scroll
        val context = context
        val model = withContext(Dispatchers.IO) {
            extractArtworkModel(song, context, artworkRepository, thumbnailMode)
        }
        if (model != null && model !== activeModel) {
            activeModel = model
        }
    }

    // No online artwork downloads — local files only

    // ── 4. Fallback gradient ───────────────────────────────────────────────
    val isDark = isSystemInDarkTheme()
    val placeholderBrush = fallbackBrush ?: remember(song?.id, isDark) {
        val hash = (song?.title?.hashCode() ?: 0) + (song?.artist?.hashCode() ?: 0)
        val hue = (hash % 360).toFloat().absoluteValue
        if (isDark) {
            val c1 = Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, 0.62f, 0.20f)))
            val c2 = Color(android.graphics.Color.HSVToColor(floatArrayOf((hue + 30f) % 360f, 0.50f, 0.30f)))
            val c3 = Color(android.graphics.Color.HSVToColor(floatArrayOf((hue + 60f) % 360f, 0.40f, 0.40f)))
            Brush.linearGradient(listOf(c1, c2, c3))
        } else {
            val c1 = Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, 0.04f, 0.98f)))
            val c2 = Color(android.graphics.Color.HSVToColor(floatArrayOf((hue + 20f) % 360f, 0.08f, 0.95f)))
            Brush.linearGradient(listOf(c1, c2))
        }
    }
    val fallbackTextColor = if (isDark) Color(0xFFFFFFFF) else Color(0xFF111111)
    val fallbackLetter = remember(song?.title, song?.artist) {
        val title = song?.title?.trim() ?: ""
        val artist = song?.artist?.trim() ?: ""
        when {
            title.isNotBlank() && !title.lowercase().contains("unknown") -> title.take(1).uppercase()
            artist.isNotBlank() && !artist.lowercase().contains("unknown") -> artist.take(1).uppercase()
            else -> null
        }
    }

    // ── 5. Render ──────────────────────────────────────────────────────────
    Surface(
        modifier = modifier.clip(RoundedCornerShape(cornerRadius)),
        shape = RoundedCornerShape(cornerRadius),
        color = surfaceColor, // Will be completely painting over
        tonalElevation = elevation,
        shadowElevation = elevation
    ) {
        // Flattened Box with background brush directly
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(placeholderBrush),
            contentAlignment = Alignment.Center
        ) {
            if (fallbackLetter != null) {
                Text(
                    text = fallbackLetter,
                    color = fallbackTextColor.copy(alpha = 0.85f),
                    fontWeight = FontWeight.Bold,
                    fontSize = (cornerRadius.value * 3.5f).coerceIn(24f, 130f).sp,
                    textAlign = TextAlign.Center
                )
            } else {
                Icon(
                    imageVector = Icons.Rounded.MusicNote,
                    contentDescription = null,
                    tint = fallbackTextColor.copy(alpha = 0.85f),
                    modifier = Modifier.size(iconSize * 1.5f)
                )
            }

            // Real artwork overlaid on top with crossfade
            if (activeModel != null) {
                val request = remember(activeModel, thumbnailMode, song?.id) {
                    val builder = ImageRequest.Builder(context)
                        .data(activeModel)
                        .size(if (thumbnailMode) 256 else 512)
                        .crossfade(150)
                        .bitmapConfig(
                            if (thumbnailMode) android.graphics.Bitmap.Config.RGB_565
                            else android.graphics.Bitmap.Config.ARGB_8888
                        )
                        .allowHardware(!thumbnailMode)
                        .diskCachePolicy(coil.request.CachePolicy.ENABLED)
                        .memoryCachePolicy(coil.request.CachePolicy.ENABLED)

                    if (song != null && song.album.isNotBlank() && !song.album.equals("unknown", ignoreCase = true)) {
                        builder.memoryCacheKey("${song.album}_${song.artist}_${if (thumbnailMode) "thumb" else "full"}")
                    }
                    builder.build()
                }
                AsyncImage(
                    model = request,
                    contentDescription = contentDescription,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    alignment = Alignment.Center
                )
            }
        }
    }
}
