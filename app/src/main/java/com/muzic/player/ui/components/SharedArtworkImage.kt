package com.muzic.player.ui.components

import android.media.MediaMetadataRetriever
import android.util.LruCache
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.draw.alpha
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
import com.muzic.player.data.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.absoluteValue
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import java.util.concurrent.ConcurrentHashMap

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

    /**
     * Invalidate cache entries for a specific album so they re-resolve
     * (used when background prefetch downloads new artwork)
     */
    fun invalidateAlbum(albumName: String) {
        // We can't iterate LruCache easily, so we use a simple snapshot
        val snapshot = cache.snapshot()
        for (key in snapshot.keys) {
            // Remove entries where the value was "NONE" — they may now have artwork
            if (snapshot[key] === "NONE") {
                cache.remove(key)
            }
        }
    }
}

/**
 * Extract artwork model — LOCAL ONLY, no network requests.
 * Priority:
 * 1. Embedded artwork from audio file
 * 2. Folder artwork (cover.jpg, folder.jpg, etc.)
 * 3. Cached internet artwork (disk cache check only)
 * 4. null → placeholder
 */
private val extractionJobs = ConcurrentHashMap<String, Deferred<Any?>>()

suspend fun extractArtworkModel(
    song: Song, 
    artworkRepo: com.muzic.player.data.repository.ArtworkRepository? = null,
    thumbnailMode: Boolean = false
): Any? = withContext(Dispatchers.IO) {
    val cacheKey = song.path
    if (cacheKey.isEmpty()) return@withContext null

    val cached = ArtworkModelCache.cache.get(cacheKey)
    if (cached != null) {
        return@withContext if (cached === "NONE") null else cached
    }

    val deferred = extractionJobs.getOrPut(cacheKey) {
        async(Dispatchers.IO) {
            var result: Any? = null

            if (song.artworkUrl != null) {
                val file = File(song.artworkUrl)
                if (file.exists()) {
                    if (thumbnailMode) {
                        val thumbFile = File(file.parentFile, "${file.nameWithoutExtension}_thumb.${file.extension}")
                        if (thumbFile.exists()) result = thumbFile
                    }
                    if (result == null) result = file
                }
            }

            if (result == null) {
                val retriever = MediaMetadataRetriever()
                try {
                    retriever.setDataSource(song.path)
                    val picture = retriever.embeddedPicture
                    if (picture != null) {
                        result = picture
                    }
                } catch (e: Exception) {
                } finally {
                    try { retriever.release() } catch (e: Exception) {}
                }
            }

            if (result == null && song.folderPath.isNotBlank()) {
                val folder = File(song.folderPath)
                if (folder.exists() && folder.isDirectory) {
                    val validNames = listOf("cover.jpg", "folder.jpg", "album.jpg", "front.jpg", "cover.png", "folder.png")
                    for (name in validNames) {
                        val file = File(folder, name)
                        if (file.exists()) {
                            result = file
                            break
                        }
                    }
                }
            }

            if (result == null && artworkRepo != null) {
                val cachedFile = artworkRepo.getCachedAlbumArtwork(song.album, song.artist, thumbnailMode)
                if (cachedFile != null) {
                    result = cachedFile
                }
            }

            ArtworkModelCache.cache.put(cacheKey, result ?: "NONE")
            result
        }
    }
    
    val finalResult = deferred.await()
    extractionJobs.remove(cacheKey)
    return@withContext finalResult
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
    var activeModel by remember(song?.id) { mutableStateOf<Any?>(null) }
    var modelReady by remember(song?.id) { mutableStateOf(false) }

    // Observe cache version — recompose when background prefetch downloads new artwork
    val cacheVersion by artworkRepository?.cacheVersion?.collectAsState() ?: remember { mutableStateOf(0L) }

    LaunchedEffect(song, cacheVersion) {
        if (song != null) {
            // When cacheVersion changes, invalidate "NONE" entries so they re-check cache
            if (cacheVersion > 0L) {
                val currentCached = ArtworkModelCache.cache.get(song.path)
                if (currentCached === "NONE") {
                    ArtworkModelCache.cache.remove(song.path)
                }
            }
            val newModel = extractArtworkModel(song, artworkRepository, thumbnailMode)
            if (activeModel != newModel) {
                activeModel = newModel
            }
        }
        modelReady = true
    }

    val isDark = isSystemInDarkTheme()
    val placeholderBrush = fallbackBrush ?: remember(song?.id, isDark) {
        val hash = (song?.title?.hashCode() ?: 0) + (song?.artist?.hashCode() ?: 0)
        val hue = (hash % 360).toFloat().absoluteValue
        if (isDark) {
            // Dark Mode example: Deeper gradient #0F2027 -> #203A43 -> #2C5364
            // Scaled HSV values to be brighter
            val c1 = Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, 0.62f, 0.20f)))
            val c2 = Color(android.graphics.Color.HSVToColor(floatArrayOf((hue + 30f) % 360f, 0.50f, 0.30f)))
            val c3 = Color(android.graphics.Color.HSVToColor(floatArrayOf((hue + 60f) % 360f, 0.40f, 0.40f)))
            Brush.linearGradient(listOf(c1, c2, c3))
        } else {
            // Light Mode example: Pastel gradient #F2F5F9 -> #E4EBF3
            val c1 = Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, 0.04f, 0.98f)))
            val c2 = Color(android.graphics.Color.HSVToColor(floatArrayOf((hue + 20f) % 360f, 0.08f, 0.95f)))
            Brush.linearGradient(listOf(c1, c2))
        }
    }
    
    val fallbackTextColor = if (isDark) Color(0xFFFFFFFF) else Color(0xFF111111)

    // Fade-in animation for fallback
    var fallbackVisible by remember(song?.id) { mutableStateOf(false) }
    LaunchedEffect(modelReady, activeModel) {
        if (modelReady && activeModel == null) {
            fallbackVisible = true
        }
    }
    val fallbackAlpha by animateFloatAsState(
        targetValue = if (fallbackVisible) 1f else 0f,
        animationSpec = tween(200),
        label = "fallbackAlpha"
    )

    // First letter of song title
    val fallbackLetter = remember(song?.title, song?.artist) {
        val title = song?.title ?: ""
        val artist = song?.artist ?: ""
        val cleanTitle = title.trim()
        val cleanArtist = artist.trim()
        when {
            cleanTitle.isNotBlank() && !cleanTitle.lowercase().contains("unknown") ->
                cleanTitle.take(1).uppercase()
            cleanArtist.isNotBlank() && !cleanArtist.lowercase().contains("unknown") ->
                cleanArtist.take(1).uppercase()
            else -> null
        }
    }

    Surface(
        modifier = modifier.clip(RoundedCornerShape(cornerRadius)),
        shape = RoundedCornerShape(cornerRadius),
        color = surfaceColor,
        tonalElevation = elevation,
        shadowElevation = elevation
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (fallbackAlpha > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .alpha(fallbackAlpha)
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
                }
            }

            // Album art on top when available
            if (modelReady && activeModel != null) {
                val context = LocalContext.current
                val request = remember(activeModel, thumbnailMode, isScrolling, song?.album, song?.artist) {
                    val builder = ImageRequest.Builder(context)
                        .data(activeModel)
                        .size(if (thumbnailMode) 256 else 512)
                        .crossfade(if (isScrolling) 0 else 200)
                        .bitmapConfig(if (thumbnailMode) android.graphics.Bitmap.Config.RGB_565 else android.graphics.Bitmap.Config.ARGB_8888)
                        .allowHardware(true)
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
