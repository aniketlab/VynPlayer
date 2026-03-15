package com.vyn.player.data.repository

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.media.MediaMetadataRetriever
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import com.vyn.player.data.model.Album
import com.vyn.player.data.model.Artist
import com.vyn.player.data.model.Folder
import com.vyn.player.data.model.Song
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton
import com.vyn.player.util.MetadataUtils
import com.vyn.player.util.MetadataUtils.MetadataType

@Singleton
class MediaStoreScanner @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private val RECORDING_PATH_KEYWORDS = listOf(
            "record",
            "recording",
            "call",
            "recorder",
            "voice",
            "sound_recorder",
            "dialer",
            "truecaller"
        )

        private val RECORDING_FILE_PATTERNS = listOf(
            Regex("^\\d{8}_?\\d{6}", RegexOption.IGNORE_CASE),
            Regex("^REC(?:_|-)?\\d*", RegexOption.IGNORE_CASE),
            Regex("^Call_", RegexOption.IGNORE_CASE)
        )
        private val NUMERIC_FILENAME_PATTERN = Regex("^(?:\\d{8}_?\\d{6}|\\d{6,})$", RegexOption.IGNORE_CASE)
    }

    private val audioUri: Uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
    } else {
        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
    }

    // ─── Scan guard: prevents concurrent scans ───
    private val isScanning = AtomicBoolean(false)

    // ─── Fast projection: only essential fields ───
    private val fastProjection = arrayOf(
        MediaStore.Audio.Media._ID,
        MediaStore.Audio.Media.TITLE,
        MediaStore.Audio.Media.ARTIST,
        MediaStore.Audio.Media.ALBUM,
        MediaStore.Audio.Media.ALBUM_ID,
        MediaStore.Audio.Media.DURATION,
        MediaStore.Audio.Media.DATA,
        MediaStore.Audio.Media.TRACK,
        MediaStore.Audio.Media.YEAR,
        MediaStore.Audio.Media.SIZE,
        MediaStore.Audio.Media.DATE_ADDED,
        MediaStore.Audio.Media.DATE_MODIFIED,
        MediaStore.Audio.Media.MIME_TYPE,
        MediaStore.Audio.Media.IS_MUSIC
    )

    /**
     * FAST SCAN: Queries MediaStore for essential fields only.
     * No MediaMetadataRetriever calls — those are deferred to background.
     * Returns results immediately for UI display.
     */
    suspend fun scanAllSongs(lastModifiedAfter: Long = 0L): List<Song> = withContext(Dispatchers.IO) {
        if (!isScanning.compareAndSet(false, true)) {
            Log.d("MediaStoreScanner", "Scan already in progress, skipping")
            return@withContext emptyList()
        }

        val startTime = System.currentTimeMillis()
        Log.d("MediaStoreScanner", "Starting fast scan...")

        try {
            val songs = ArrayList<Song>(500) // Pre-allocate for performance
            val seenPaths = HashSet<String>() // For deduplication by file path
            val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

            context.contentResolver.query(
                audioUri,
                fastProjection,
                buildSelection(lastModifiedAfter),
                null,
                sortOrder
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
                val trackColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK)
                val yearColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.YEAR)
                val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
                val dateAddedColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
                val dateModifiedColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_MODIFIED)
                val mimeTypeColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)

                Log.d("MediaStoreScanner", "Cursor has ${cursor.count} rows")

                while (cursor.moveToNext()) {
                    val path = cursor.getString(dataColumn) ?: continue
                    val normalizedPath = path.replace('\\', '/').lowercase().trim()
                    val file = File(path)
                    val fileName = file.nameWithoutExtension

                    val rawTitle = cursor.getString(titleColumn)
                    val rawArtist = cursor.getString(artistColumn)
                    val rawAlbum = cursor.getString(albumColumn)

                    if (shouldSkipRecordingCandidate(
                            normalizedPath = normalizedPath,
                            fileName = fileName,
                            title = rawTitle,
                            artist = rawArtist,
                            album = rawAlbum
                        )
                    ) continue

                    // ─── Extract and clean metadata (CPU-only, no file I/O) ───
                    val id = cursor.getLong(idColumn)
                    val folderPath = file.parent ?: ""
                    val folderName = File(folderPath).name

                    var resolvedTitle = rawTitle
                    var resolvedArtist = rawArtist
                    var resolvedAlbum = rawAlbum
                    val duration = cursor.getLong(durationColumn)
                    val albumId = cursor.getLong(albumIdColumn)

                    // ─── Fast filename fallback (no disk I/O) ───
                    if (isSuspicious(resolvedArtist) || isSuspicious(resolvedTitle)) {
                        val (extractedTitle, extractedArtist) = MetadataUtils.extractFromFileName(file.name)
                        if (isSuspicious(resolvedTitle) && extractedTitle != null) resolvedTitle = extractedTitle
                        if (isSuspicious(resolvedArtist) && extractedArtist != null) resolvedArtist = extractedArtist
                    }

                    // Never copy TITLE into ARTIST
                    if (resolvedArtist != null && resolvedTitle != null && resolvedArtist.equals(resolvedTitle, ignoreCase = true)) {
                        resolvedArtist = null
                    }

                    // Fallback to Folder Name for Album if missing
                    if (isSuspicious(resolvedAlbum)) {
                        resolvedAlbum = folderName
                    }

                    // ─── Clean & sanitize ───
                    val cleanTitle = MetadataUtils.cleanTitle(resolvedTitle ?: file.nameWithoutExtension)
                    val cleanArtist = MetadataUtils.cleanArtist(resolvedArtist ?: "")
                    val cleanAlbum = MetadataUtils.cleanTitle(resolvedAlbum ?: "")

                    val finalTitle = MetadataUtils.sanitize(cleanTitle, MetadataType.TITLE)
                    val finalArtist = MetadataUtils.sanitize(cleanArtist, MetadataType.ARTIST)
                    val finalAlbum = MetadataUtils.sanitize(cleanAlbum, MetadataType.ALBUM)

                    val contentUri = ContentUris.withAppendedId(audioUri, id)
                    val dedupeKey = normalizedPath.ifBlank {
                        contentUri.toString().lowercase().trim()
                    }

                    // ─── Deduplicate after existing filters using normalized path, fallback URI ───
                    if (!seenPaths.add(dedupeKey)) {
                        continue
                    }

                    songs.add(
                        Song(
                            id = id,
                            title = finalTitle,
                            artist = finalArtist,
                            album = finalAlbum,
                            albumId = albumId,
                            duration = duration,
                            path = path,
                            uri = contentUri,
                            trackNumber = cursor.getInt(trackColumn),
                            year = cursor.getInt(yearColumn),
                            size = cursor.getLong(sizeColumn),
                            dateAdded = cursor.getLong(dateAddedColumn),
                            dateModified = cursor.getLong(dateModifiedColumn),
                            mimeType = cursor.getString(mimeTypeColumn) ?: "",
                            folderName = folderName,
                            folderPath = folderPath
                        )
                    )
                }
            }

            val elapsed = System.currentTimeMillis() - startTime
            Log.d("MediaStoreScanner", "Fast scan complete: ${songs.size} songs in ${elapsed}ms")

            songs
        } finally {
            isScanning.set(false)
        }
    }

    private fun buildSelection(lastModifiedAfter: Long): String {
        val clauses = mutableListOf<String>()
        clauses += "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        if (lastModifiedAfter > 0) {
            clauses += "${MediaStore.Audio.Media.DATE_MODIFIED} > $lastModifiedAfter"
        }
        return clauses.joinToString(" AND ")
    }

    private fun shouldSkipRecordingCandidate(
        normalizedPath: String,
        fileName: String,
        title: String?,
        artist: String?,
        album: String?
    ): Boolean {
        if (RECORDING_PATH_KEYWORDS.any(normalizedPath::contains)) return true

        if (RECORDING_FILE_PATTERNS.any { it.containsMatchIn(fileName) }) return true

        val metadataMissing = title.isNullOrBlank() && artist.isNullOrBlank() && album.isNullOrBlank()
        if (metadataMissing && NUMERIC_FILENAME_PATTERN.matches(fileName)) return true

        return false
    }

    /**
     * BACKGROUND METADATA FIX: Runs MediaMetadataRetriever for songs
     * with suspicious metadata. Called AFTER initial scan and UI display.
     * Returns list of songs that were updated.
     */
    suspend fun fixSuspiciousMetadata(songs: List<Song>): List<Song> = withContext(Dispatchers.IO) {
        val updatedSongs = mutableListOf<Song>()

        for (song in songs) {
            if (!isSuspicious(song.artist) && !isSuspicious(song.album) && !isSuspicious(song.title)) {
                continue
            }

            yield() // Allow cancellation between songs

            val mmr = MediaMetadataRetriever()
            try {
                mmr.setDataSource(song.path)

                var newTitle = song.title
                var newArtist = song.artist
                var newAlbum = song.album
                var newDuration = song.duration
                var changed = false

                if (isSuspicious(song.title)) {
                    val mmrTitle = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                    if (mmrTitle != null && !isSuspicious(mmrTitle)) {
                        newTitle = MetadataUtils.sanitize(MetadataUtils.cleanTitle(mmrTitle), MetadataType.TITLE)
                        changed = true
                    }
                }
                if (isSuspicious(song.artist)) {
                    val mmrArtist = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                    if (mmrArtist != null && !isSuspicious(mmrArtist)) {
                        newArtist = MetadataUtils.sanitize(MetadataUtils.cleanArtist(mmrArtist), MetadataType.ARTIST)
                        changed = true
                    }
                }
                if (isSuspicious(song.album)) {
                    val mmrAlbum = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
                    if (mmrAlbum != null && !isSuspicious(mmrAlbum)) {
                        newAlbum = MetadataUtils.sanitize(MetadataUtils.cleanTitle(mmrAlbum), MetadataType.ALBUM)
                        changed = true
                    }
                }
                if (song.duration <= 0) {
                    val mmrDuration = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
                    if (mmrDuration != null && mmrDuration > 0) {
                        newDuration = mmrDuration
                        changed = true
                    }
                }

                if (changed) {
                    updatedSongs.add(song.copy(title = newTitle, artist = newArtist, album = newAlbum, duration = newDuration))
                }
            } catch (_: Exception) {
                // Skip songs that can't be read
            } finally {
                mmr.release()
            }
        }

        Log.d("MediaStoreScanner", "Background metadata fix: ${updatedSongs.size} songs updated")
        updatedSongs
    }

    suspend fun scanAlbums(): List<Album> = withContext(Dispatchers.IO) {
        val albums = mutableListOf<Album>()

        val albumProjection = arrayOf(
            MediaStore.Audio.Albums._ID,
            MediaStore.Audio.Albums.ALBUM,
            MediaStore.Audio.Albums.ARTIST,
            MediaStore.Audio.Albums.NUMBER_OF_SONGS,
            MediaStore.Audio.Albums.FIRST_YEAR
        )

        context.contentResolver.query(
            MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
            albumProjection,
            null,
            null,
            "${MediaStore.Audio.Albums.ALBUM} ASC"
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums._ID)
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.ALBUM)
            val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.ARTIST)
            val songCountColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.NUMBER_OF_SONGS)
            val yearColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.FIRST_YEAR)

            while (cursor.moveToNext()) {
                val name = MetadataUtils.sanitize(cursor.getString(nameColumn), MetadataType.ALBUM)
                val artist = MetadataUtils.sanitize(cursor.getString(artistColumn), MetadataType.ARTIST)
                albums.add(
                    Album(
                        id = cursor.getLong(idColumn),
                        name = name,
                        artist = artist,
                        songCount = cursor.getInt(songCountColumn),
                        year = cursor.getInt(yearColumn)
                    )
                )
            }
        }

        albums
    }

    suspend fun scanArtists(): List<Artist> = withContext(Dispatchers.IO) {
        val artists = mutableListOf<Artist>()

        val artistProjection = arrayOf(
            MediaStore.Audio.Artists._ID,
            MediaStore.Audio.Artists.ARTIST,
            MediaStore.Audio.Artists.NUMBER_OF_TRACKS,
            MediaStore.Audio.Artists.NUMBER_OF_ALBUMS
        )

        context.contentResolver.query(
            MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI,
            artistProjection,
            null,
            null,
            "${MediaStore.Audio.Artists.ARTIST} ASC"
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Artists._ID)
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Artists.ARTIST)
            val songCountColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Artists.NUMBER_OF_TRACKS)
            val albumCountColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Artists.NUMBER_OF_ALBUMS)

            while (cursor.moveToNext()) {
                val name = MetadataUtils.sanitize(cursor.getString(nameColumn), MetadataType.ARTIST)
                artists.add(
                    Artist(
                        id = cursor.getLong(idColumn),
                        name = name,
                        songCount = cursor.getInt(songCountColumn),
                        albumCount = cursor.getInt(albumCountColumn)
                    )
                )
            }
        }

        artists
    }

    suspend fun scanFolders(songs: List<Song>? = null): List<Folder> = withContext(Dispatchers.IO) {
        val allSongs = songs ?: scanAllSongs()
        allSongs
            .groupBy { it.folderPath }
            .map { (path, folderSongs) ->
                Folder(
                    path = path,
                    name = File(path).name,
                    songCount = folderSongs.size
                )
            }
            .sortedBy { it.name.lowercase() }
    }

    suspend fun getSongsInFolder(folderPath: String): List<Song> = withContext(Dispatchers.IO) {
        scanAllSongs().filter { it.folderPath == folderPath }
    }

    suspend fun getSongsForAlbum(albumId: Long): List<Song> = withContext(Dispatchers.IO) {
        scanAllSongs().filter { it.albumId == albumId }.sortedBy { it.trackNumber }
    }

    suspend fun getSongsForArtist(artistName: String): List<Song> = withContext(Dispatchers.IO) {
        scanAllSongs().filter { it.artist.equals(artistName, ignoreCase = true) }
    }

    private fun isSuspicious(text: String?): Boolean {
        if (text == null || text.isBlank()) return true
        val lower = text.lowercase()
        return lower == "0" || lower == "<unknown>" || lower == "unknown" || lower == "null" || MetadataUtils.containsJunk(text)
    }
}
