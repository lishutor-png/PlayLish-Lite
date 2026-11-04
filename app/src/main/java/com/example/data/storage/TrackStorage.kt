package com.example.data.storage

import android.content.Context
import com.example.data.model.Playlist
import com.example.data.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class TrackStorage(private val context: Context) {

    private val tracksFile = File(context.filesDir, "saved_tracks.json")
    private val playlistsFile = File(context.filesDir, "saved_playlists.json")

    suspend fun loadSavedTracks(): List<Track> = withContext(Dispatchers.IO) {
        if (!tracksFile.exists()) return@withContext emptyList()
        try {
            val jsonStr = tracksFile.readText()
            val jsonArray = JSONArray(jsonStr)
            val list = mutableListOf<Track>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    Track(
                        id = obj.getString("id"),
                        title = obj.optString("title", "Unknown Title"),
                        artist = obj.optString("artist", "Unknown Artist"),
                        album = obj.optString("album", "Unknown Album"),
                        durationMs = obj.optLong("durationMs", 0L),
                        uri = if (obj.has("uri") && !obj.isNull("uri")) obj.getString("uri") else null,
                        format = obj.optString("format", "MP3"),
                        sampleRateKHz = obj.optDouble("sampleRateKHz", 44.1).toFloat(),
                        bitDepth = obj.optInt("bitDepth", 16),
                        bitrateKbps = obj.optInt("bitrateKbps", 320),
                        isHiRes = obj.optBoolean("isHiRes", false),
                        rawLyrics = if (obj.has("rawLyrics") && !obj.isNull("rawLyrics")) obj.getString("rawLyrics") else null,
                        category = obj.optString("category", "Semua"),
                        accentColor = obj.optLong("accentColor", 0xFFFF6D00),
                        isDeviceFile = obj.optBoolean("isDeviceFile", true),
                        synthScaleType = obj.optInt("synthScaleType", 0),
                        replayGainDb = obj.optDouble("replayGainDb", 0.0).toFloat()
                    )
                )
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun saveTracks(tracks: List<Track>) = withContext(Dispatchers.IO) {
        try {
            val jsonArray = JSONArray()
            for (track in tracks) {
                val obj = JSONObject()
                obj.put("id", track.id)
                obj.put("title", track.title)
                obj.put("artist", track.artist)
                obj.put("album", track.album)
                obj.put("durationMs", track.durationMs)
                obj.put("uri", track.uri)
                obj.put("format", track.format)
                obj.put("sampleRateKHz", track.sampleRateKHz.toDouble())
                obj.put("bitDepth", track.bitDepth)
                obj.put("bitrateKbps", track.bitrateKbps)
                obj.put("isHiRes", track.isHiRes)
                obj.put("rawLyrics", track.rawLyrics)
                obj.put("category", track.category)
                obj.put("accentColor", track.accentColor)
                obj.put("isDeviceFile", track.isDeviceFile)
                obj.put("synthScaleType", track.synthScaleType)
                obj.put("replayGainDb", track.replayGainDb.toDouble())
                jsonArray.put(obj)
            }
            tracksFile.writeText(jsonArray.toString())
        } catch (_: Exception) {}
    }

    suspend fun loadPlaylists(): List<Playlist> = withContext(Dispatchers.IO) {
        if (!playlistsFile.exists()) {
            // Default starter playlist
            val defaultPlaylists = listOf(
                Playlist(
                    id = "pl_favorites",
                    name = "Favorit Saya",
                    description = "Kumpulan lagu yang paling disukai",
                    trackIds = emptyList()
                )
            )
            savePlaylists(defaultPlaylists)
            return@withContext defaultPlaylists
        }
        try {
            val jsonStr = playlistsFile.readText()
            val jsonArray = JSONArray(jsonStr)
            val list = mutableListOf<Playlist>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val trackIdsArray = obj.optJSONArray("trackIds") ?: JSONArray()
                val trackIds = mutableListOf<String>()
                for (j in 0 until trackIdsArray.length()) {
                    trackIds.add(trackIdsArray.getString(j))
                }
                list.add(
                    Playlist(
                        id = obj.getString("id"),
                        name = obj.getString("name"),
                        description = obj.optString("description", ""),
                        trackIds = trackIds,
                        iconName = obj.optString("iconName", "playlist"),
                        createdAtEpoch = obj.optLong("createdAtEpoch", System.currentTimeMillis())
                    )
                )
            }
            if (list.isEmpty()) {
                listOf(
                    Playlist(
                        id = "pl_favorites",
                        name = "Favorit Saya",
                        description = "Kumpulan lagu yang paling disukai",
                        trackIds = emptyList()
                    )
                )
            } else {
                list
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun savePlaylists(playlists: List<Playlist>) = withContext(Dispatchers.IO) {
        try {
            val jsonArray = JSONArray()
            for (playlist in playlists) {
                val obj = JSONObject()
                obj.put("id", playlist.id)
                obj.put("name", playlist.name)
                obj.put("description", playlist.description)
                val trackIdsArray = JSONArray()
                for (tid in playlist.trackIds) {
                    trackIdsArray.put(tid)
                }
                obj.put("trackIds", trackIdsArray)
                obj.put("iconName", playlist.iconName)
                obj.put("createdAtEpoch", playlist.createdAtEpoch)
                jsonArray.put(obj)
            }
            playlistsFile.writeText(jsonArray.toString())
        } catch (_: Exception) {}
    }
}
