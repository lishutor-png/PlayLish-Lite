package com.example.data.repository

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.example.data.model.Playlist
import com.example.data.model.Track
import com.example.data.storage.TrackStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MusicRepository(private val context: Context) {

    private val storage = TrackStorage(context)

    // No built-in default tracks: user imports their own audio or scans device
    fun getSampleTracks(): List<Track> = emptyList()

    suspend fun getSavedTracks(): List<Track> = storage.loadSavedTracks()

    suspend fun saveTracks(tracks: List<Track>) = storage.saveTracks(tracks)

    suspend fun getPlaylists(): List<Playlist> = storage.loadPlaylists()

    suspend fun savePlaylists(playlists: List<Playlist>) = storage.savePlaylists(playlists)

    // Scans device MediaStore for audio files
    suspend fun queryDeviceAudio(): List<Track> = withContext(Dispatchers.IO) {
        val deviceTracks = mutableListOf<Track>()
        val contentResolver: ContentResolver = context.contentResolver

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATA
        )

        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

        try {
            val cursor: Cursor? = contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                null,
                sortOrder
            )

            cursor?.use {
                val idCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val durationCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val dataCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)

                while (it.moveToNext()) {
                    val id = it.getLong(idCol)
                    val title = it.getString(titleCol) ?: "Unknown Track"
                    val artist = it.getString(artistCol) ?: "Unknown Artist"
                    val album = it.getString(albumCol) ?: "Unknown Album"
                    val duration = it.getLong(durationCol)
                    val filePath = it.getString(dataCol) ?: ""

                    if (duration > 5000) { // filter out short notifications
                        val uri = Uri.withAppendedPath(
                            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                            id.toString()
                        ).toString()

                        val extension = if (filePath.contains(".")) {
                            filePath.substringAfterLast(".").uppercase()
                        } else "MP3"

                        val isHiRes = extension in listOf("FLAC", "WAV", "ALAC", "DSD", "AIFF")

                        deviceTracks.add(
                            Track(
                                id = "device_$id",
                                title = title,
                                artist = artist,
                                album = album,
                                durationMs = duration,
                                uri = uri,
                                format = extension,
                                sampleRateKHz = if (isHiRes) 96.0f else 44.1f,
                                bitDepth = if (isHiRes) 24 else 16,
                                bitrateKbps = if (isHiRes) 1411 else 320,
                                isHiRes = isHiRes,
                                category = "Perangkat",
                                accentColor = if (isHiRes) 0xFFFF6D00 else 0xFF00E5FF,
                                isDeviceFile = true,
                                replayGainDb = ((-2..2).random()).toFloat()
                            )
                        )
                    }
                }
            }
        } catch (_: Exception) {}

        deviceTracks
    }

    // Imports audio from a user selected Document URI with persistable permission
    suspend fun importAudioFromUri(uri: Uri): Track? = withContext(Dispatchers.IO) {
        // Take persistable URI permission so it survives app restarts
        try {
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            context.contentResolver.takePersistableUriPermission(uri, flags)
        } catch (_: Exception) {}

        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)
            val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                ?: uri.lastPathSegment?.substringAfterLast("/") ?: "Lagu Impor"
            val artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST) ?: "Artis Impor"
            val album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM) ?: "Koleksi Impor"
            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            val duration = durationStr?.toLongOrNull() ?: 180000L
            val bitrateStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)
            val bitrateKbps = (bitrateStr?.toIntOrNull() ?: 320000) / 1000
            val mimeType = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE) ?: "audio/mpeg"

            val format = when {
                mimeType.contains("flac", ignoreCase = true) -> "FLAC"
                mimeType.contains("wav", ignoreCase = true) -> "WAV"
                mimeType.contains("alac", ignoreCase = true) -> "ALAC"
                mimeType.contains("ogg", ignoreCase = true) -> "OGG"
                mimeType.contains("aac", ignoreCase = true) -> "AAC"
                mimeType.contains("opus", ignoreCase = true) -> "OPUS"
                else -> "MP3"
            }

            val isHiRes = format in listOf("FLAC", "WAV", "ALAC", "DSD")

            Track(
                id = "imported_${System.currentTimeMillis()}_${(100..999).random()}",
                title = title,
                artist = artist,
                album = album,
                durationMs = duration,
                uri = uri.toString(),
                format = format,
                sampleRateKHz = if (isHiRes) 96.0f else 44.1f,
                bitDepth = if (isHiRes) 24 else 16,
                bitrateKbps = bitrateKbps.coerceAtLeast(128),
                isHiRes = isHiRes,
                category = "Perangkat",
                accentColor = 0xFF00E5FF,
                isDeviceFile = true,
                replayGainDb = ((-3..3).random()).toFloat()
            )
        } catch (_: Exception) {
            null
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {}
        }
    }
}
