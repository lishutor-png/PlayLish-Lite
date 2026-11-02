package com.example.data.model

data class Track(
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val uri: String? = null,
    val format: String = "FLAC",       // FLAC, ALAC, WAV, MP3, OPUS, DSD
    val sampleRateKHz: Float = 96.0f,  // 44.1, 48.0, 96.0, 192.0
    val bitDepth: Int = 24,            // 16, 24, 32
    val bitrateKbps: Int = 920,        // 320, 920, 1411, 4608
    val isHiRes: Boolean = true,
    val rawLyrics: String? = null,
    val category: String = "Hi-Res",
    val accentColor: Long = 0xFFFF6D00,
    val isDeviceFile: Boolean = false,
    val synthScaleType: Int = 0,       // 0: Chill Ambient, 1: Sporty Energetic, 2: Sleep Deep Drone, 3: Acoustic Piano
    val replayGainDb: Float = 0.0f     // Track loudness offset in dB for ReplayGain/EBU R128
) {
    val durationFormatted: String
        get() {
            val totalSeconds = durationMs / 1000
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            return "%d:%02d".format(minutes, seconds)
        }

    val audioSpecsBadge: String
        get() = "$format $bitDepth-BIT / ${sampleRateKHz.toInt()}kHz"
}
