package com.example.data.repository

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.example.data.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class MusicRepository(private val context: Context) {

    // Built-in high-fidelity demo tracks with complete LRC synced lyrics
    private val sampleTracks = listOf(
        Track(
            id = "demo_1",
            title = "Midnight Velocity (Hi-Res Master)",
            artist = "Aether Pulse",
            album = "Cybernetic Horizon Vol. 1",
            durationMs = 184000L, // 3:04
            format = "FLAC",
            sampleRateKHz = 96.0f,
            bitDepth = 24,
            bitrateKbps = 2304,
            isHiRes = true,
            category = "Hi-Res",
            accentColor = 0xFFFF6D00,
            synthScaleType = 1, // Sporty energetic synth
            rawLyrics = """
                [00:00.00]-- Intro: Aether Pulse - Midnight Velocity --
                [00:08.50]Menembus batas malam yang hening
                [00:15.20]Langkah berdetak seirama gelombang suara
                [00:23.80]Resolusi murni mengalir tanpa jeda
                [00:31.40]Rasakan bass membelah sunyi angkasa
                [00:39.00]PlayLish Lite memandu irama nafasmu
                [00:47.30]Kilau neon membakar sisa keraguan
                [00:55.10]Setiap detik berputar dalam frekuensi sejati
                [01:03.40]Tetap fokus melaju hingga garis akhir
                [01:12.80]-- Solo Synthesizer Arpeggio --
                [01:28.00]Angin malam meredam rasa lelah
                [01:36.20]Harmoni akustik menjaga ketenangan jiwa
                [01:45.00]Melangkah pasti bersama ritme presisi
                [01:54.60]Takkan berhenti sebelum fajar menyapa
                [02:05.10]-- Instrumental Outro --
                [02:25.00]Harmoni memudar dalam kedamaian
            """.trimIndent()
        ),
        Track(
            id = "demo_2",
            title = "Deep Sleep Lullaby (432Hz Soundscape)",
            artist = "Soma Resonance",
            album = "Nightfall Rest & Recovery",
            durationMs = 240000L, // 4:00
            format = "WAV",
            sampleRateKHz = 192.0f,
            bitDepth = 32,
            bitrateKbps = 4608,
            isHiRes = true,
            category = "Pengantar Tidur",
            accentColor = 0xFF00E5FF,
            synthScaleType = 2, // Deep soothing drone
            rawLyrics = """
                [00:00.00]-- Gelombang Relaksasi 432Hz Dimulai --
                [00:12.00]Pejamkan mata perlahan
                [00:25.00]Lepaskan semua beban pikiran hari ini
                [00:40.00]Hembuskan nafas panjang yang menenangkan
                [00:58.00]Timer tidur menjaga istirahatmu dengan aman
                [01:15.00]Volume melembut bertahap tanpa mengejutkan
                [01:35.00]Alunan nada menjaga kehangatan tidurmu
                [01:58.00]Rasa damai menyelimuti seluruh raga
                [02:20.00]Biarkan mimpi indah membawamu terbang
                [02:45.00]Istirahatlah dalam ketenangan yang abadi
                [03:15.00]Selamat tidur, esok hari menanti dengan semangat baru
            """.trimIndent()
        ),
        Track(
            id = "demo_3",
            title = "Sprint Elevation (Cardio Drive)",
            artist = "Titanium Beats",
            album = "Ultra Performance Soundtrack",
            durationMs = 198000L, // 3:18
            format = "ALAC",
            sampleRateKHz = 96.0f,
            bitDepth = 24,
            bitrateKbps = 1411,
            isHiRes = true,
            category = "Enerjik",
            accentColor = 0xFFFF3D00,
            synthScaleType = 1,
            rawLyrics = """
                [00:00.00]Ready... Set... Go!
                [00:06.00]Detak jantung meningkat seiring ketukan drum
                [00:14.20]Dorong batas kemampuan tanpa kompromi
                [00:22.50]Keringat menetes jadi saksi perjuangan
                [00:30.80]Irama acak berurutan menjaga kejutan adrenalin
                [00:40.00]Lompat lebih tinggi, lari lebih kencang!
                [00:49.00]Kejernihan audio membuat setiap langkah berenergi
                [00:58.50]Kita takkan mundur sebelum tuntas
                [01:08.00]Power up! Energi maksimum!
                [01:25.00]Tembus garis finis dengan kepala tegak!
            """.trimIndent()
        ),
        Track(
            id = "demo_4",
            title = "Acoustic Raindrops on Bamboo",
            artist = "Kyoto Strings",
            album = "Organic Sound Journey",
            durationMs = 210000L, // 3:30
            format = "FLAC",
            sampleRateKHz = 96.0f,
            bitDepth = 24,
            bitrateKbps = 1820,
            isHiRes = true,
            category = "Hi-Res",
            accentColor = 0xFF00E676,
            synthScaleType = 3, // Acoustic piano/pluck
            rawLyrics = """
                [00:00.00]-- Petikan Senar & Gemericik Hujan --
                [00:10.50]Tetesan embun jatuh di daun bambu
                [00:22.00]Gemericik lembut menyegarkan suasana
                [00:35.40]Petikan akustik menyatu dengan angin
                [00:48.00]Kejernihan nada 24-bit tanpa kompresi
                [01:02.00]Ketenangan batin meresap ke dalam sukma
                [01:20.00]Dengarkan setiap getaran frekuensi
                [01:40.00]Waktu seolah melambat di sini
                [02:00.00]Kedamaian alam di telingamu
            """.trimIndent()
        ),
        Track(
            id = "demo_5",
            title = "Neon Boulevard (Synthwave Odyssey)",
            artist = "Retro Grid",
            album = "Analog Dreams 1986",
            durationMs = 225000L, // 3:45
            format = "DSD",
            sampleRateKHz = 192.0f,
            bitDepth = 32,
            bitrateKbps = 5644,
            isHiRes = true,
            category = "Enerjik",
            accentColor = 0xFFFF007F,
            synthScaleType = 0,
            rawLyrics = """
                [00:00.00]-- Cruising down the retro highway --
                [00:14.00]Lampu kota berkelebat di kaca spion
                [00:26.50]Synthesizer 80-an mengiringi perjalanan malam
                [00:38.20]Bass analog yang tebal dan hangat
                [00:51.00]Tak ada arah tujuan, hanya melaju bebas
                [01:05.40]PlayLish Lite menghadirkan nuansa studio analog
                [01:20.00]Bintang-bintang menjadi saksi petualangan ini
                [01:45.00]Melintasi batas waktu dan dimensi
            """.trimIndent()
        ),
        Track(
            id = "demo_6",
            title = "Serenade of the Forest Mist",
            artist = "Ambient Realm",
            album = "Peaceful Slumber Series",
            durationMs = 260000L, // 4:20
            format = "FLAC",
            sampleRateKHz = 96.0f,
            bitDepth = 24,
            bitrateKbps = 2100,
            isHiRes = true,
            category = "Pengantar Tidur",
            accentColor = 0xFF80D8FF,
            synthScaleType = 2,
            rawLyrics = """
                [00:00.00]-- Kabut Pagi di Hutan Pinus --
                [00:18.00]Kicau burung sayup terdengar di kejauhan
                [00:36.00]Udara sejuk mengalir perlahan
                [00:55.00]Pikiran menjadi tenang dan jernih
                [01:20.00]Pengatur desibel telinga menjaga kenyamanan pendengaran
                [01:45.00]Tidur pulas dalam pelukan alam
                [02:10.00]Relaksasi total untuk pemulihan tubuh
            """.trimIndent()
        )
    )

    fun getSampleTracks(): List<Track> = sampleTracks

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

                        // Look for a companion .lrc file in the same directory!
                        var lrcContent: String? = null
                        if (filePath.isNotEmpty()) {
                            try {
                                val audioFile = File(filePath)
                                val baseName = audioFile.nameWithoutExtension
                                val lrcFile = File(audioFile.parentFile, "$baseName.lrc")
                                if (lrcFile.exists() && lrcFile.canRead()) {
                                    lrcContent = lrcFile.readText()
                                }
                            } catch (_: Exception) {
                                // Gracefully ignore file access limitations
                            }
                        }

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
                                rawLyrics = lrcContent,
                                category = "Perangkat",
                                accentColor = 0xFF00E5FF,
                                isDeviceFile = true
                            )
                        )
                    }
                }
            }
        } catch (_: Exception) {
            // Return collected so far or empty
        }

        deviceTracks
    }
}
