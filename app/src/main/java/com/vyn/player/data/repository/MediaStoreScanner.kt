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
            val selection = if (lastModifiedAfter > 0) "${MediaStore.Audio.Media.DATE_MODIFIED} > $lastModifiedAfter" else null
            val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

            context.contentResolver.query(
                audioUri,
                fastProjection,
                selection,
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
                val isMusicColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.IS_MUSIC)

                Log.d("MediaStoreScanner", "Cursor has ${cursor.count} rows")

                while (cursor.moveToNext()) {
                    val path = cursor.getString(dataColumn) ?: continue

                    // ─── Deduplicate by File Path ───
                    if (!seenPaths.add(path)) {
                        continue // Skip duplicate entry
                    }

                    // ─── Folder Blacklist ───
                    val upperPath = path.uppercase()
                    val isJunkFolder = upperPath.contains("WHATSAPP VOICE NOTES") ||
                                     upperPath.contains("WHATSAPP AUDIO") ||
                                     upperPath.contains("CALLRECORDINGS") ||
                                     upperPath.contains("RECORDER") ||
                                     upperPath.contains("TELEGRAM AUDIO") ||
                                     upperPath.contains("ANDROID/MEDIA/COM.WHATSAPP")

                    if (isJunkFolder) continue

                    // ─── Confidence scoring (fast — no I/O) ───
                    val duration = cursor.getLong(durationColumn)
                    val size = cursor.getLong(sizeColumn)
                    val isMusicFlag = cursor.getInt(isMusicColumn) != 0

                    val isMusicFolder = upperPath.contains("/MUSIC/") ||
                                        upperPath.contains("/SONGS/") ||
                                        upperPath.contains("/DOWNLOADS/") ||
                                        upperPath.contains("/DOWNLOAD/") ||
                                        upperPath.contains("/ALBUMS/") ||
                                        upperPath.contains("/ARTIST/")

                    var confidenceScore = 0
                    if (duration > 30000) confidenceScore++
                    if (size > 1048576) confidenceScore++
                    if (isMusicFolder) confidenceScore++
                    if (isMusicFlag) confidenceScore++

                    if (confidenceScore < 2) continue

                    // ─── Extract and clean metadata (CPU-only, no file I/O) ───
                    val id = cursor.getLong(idColumn)
                    val file = File(path)
                    val folderPath = file.parent ?: ""
                    val folderName = File(folderPath).name

                    var rawTitle = cursor.getString(titleColumn)
                    var rawArtist = cursor.getString(artistColumn)
                    var rawAlbum = cursor.getString(albumColumn)
                    val albumId = cursor.getLong(albumIdColumn)

                    // ─── Fast filename fallback (no disk I/O) ───
                    if (isSuspicious(rawArtist) || isSuspicious(rawTitle)) {
                        val (extractedTitle, extractedArtist) = MetadataUtils.extractFromFileName(file.name)
                        if (isSuspicious(rawTitle) && extractedTitle != null) rawTitle = extractedTitle
                        if (isSuspicious(rawArtist) && extractedArtist != null) rawArtist = extractedArtist
                    }

                    // Never copy TITLE into ARTIST
                    if (rawArtist != null && rawTitle != null && rawArtist.equals(rawTitle, ignoreCase = true)) {
                        rawArtist = null
                    }

                    // Fallback to Folder Name for Album if missing
                    if (isSuspicious(rawAlbum)) {
                        rawAlbum = folderName
                    }

                    // ─── Clean & sanitize ───
                    val cleanTitle = MetadataUtils.cleanTitle(rawTitle ?: file.nameWithoutExtension)
                    val cleanArtist = MetadataUtils.cleanArtist(rawArtist ?: "")
                    val cleanAlbum = MetadataUtils.cleanTitle(rawAlbum ?: "")

                    val finalTitle = MetadataUtils.sanitize(cleanTitle, MetadataType.TITLE)
                    val finalArtist = MetadataUtils.sanitize(cleanArtist, MetadataType.ARTIST)
                    val finalAlbum = MetadataUtils.sanitize(cleanAlbum, MetadataType.ALBUM)

                    val contentUri = ContentUris.withAppendedId(audioUri, id)

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
