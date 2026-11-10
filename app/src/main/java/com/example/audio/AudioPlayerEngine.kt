package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.MediaPlayer
import android.net.Uri
import com.example.data.model.Track
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.log10
import kotlin.math.sin

class AudioPlayerEngine(
    private val context: Context,
    private val onTrackCompleted: () -> Unit
) {
    private var mediaPlayer: MediaPlayer? = null
    private var synthTrack: AudioTrack? = null
    private var synthJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default)

    private var currentPlayingTrack: Track? = null
    private var isCurrentlyPlaying = false
    private var currentPositionMillis: Long = 0L

    // Volume Factors
    private var userVolume: Float = 0.85f           // 0.0 to 1.0
    private var hearingSafetyCap: Float = 0.75f     // Max ceiling if hearing safety is enabled
    private var isHearingSafetyActive: Boolean = true
    private var fadeMultiplier: Float = 1.0f        // For sleep timer gradual fade-out

    // Automatic Volume Leveling / Loudness Normalization (ReplayGain / EBU R128)
    private var isLoudnessNormalizationActive: Boolean = true
    private var targetLoudnessLufs: Float = -14.0f
    var appliedNormalizationGainDb: Float = 0.0f
        private set

    val normalizationMultiplier: Float
        get() {
            if (!isLoudnessNormalizationActive) {
                appliedNormalizationGainDb = 0.0f
                return 1.0f
            }
            val trackGain = currentPlayingTrack?.replayGainDb ?: 0.0f
            // Standard EBU R128 (-14 LUFS target compensation)
            val lufsDelta = (-14.0f) - targetLoudnessLufs
            val totalGainDb = (trackGain + lufsDelta).coerceIn(-9.0f, 6.0f)
            appliedNormalizationGainDb = totalGainDb
            return Math.pow(10.0, totalGainDb / 20.0).toFloat().coerceIn(0.4f, 1.4f)
        }

    val currentTrack: Track? get() = currentPlayingTrack
    val isPlaying: Boolean get() = isCurrentlyPlaying
    val currentPosition: Long
        get() {
            mediaPlayer?.let {
                if (isCurrentlyPlaying) {
                    try {
                        return it.currentPosition.toLong()
                    } catch (_: Exception) {}
                }
            }
            return currentPositionMillis
        }

    val duration: Long
        get() {
            mediaPlayer?.let {
                try {
                    val d = it.duration.toLong()
                    if (d > 0) return d
                } catch (_: Exception) {}
            }
            return currentPlayingTrack?.durationMs ?: 0L
        }

    val effectiveVolume: Float
        get() {
            val base = if (isHearingSafetyActive) {
                userVolume.coerceAtMost(hearingSafetyCap)
            } else {
                userVolume
            }
            val norm = if (isLoudnessNormalizationActive) normalizationMultiplier else 1.0f
            return (base * norm * fadeMultiplier).coerceIn(0.0f, 1.0f)
        }

    val currentDecibels: Float
        get() {
            val vol = effectiveVolume
            if (vol <= 0.001f) return -60.0f
            return (20.0f * log10(vol)).coerceIn(-60.0f, 0.0f)
        }

    fun playTrack(track: Track, startPositionMs: Long = 0L) {
        stop()
        currentPlayingTrack = track
        currentPositionMillis = startPositionMs

        if (track.isDeviceFile && track.uri != null) {
            playDeviceMedia(track, startPositionMs)
        } else {
            playSynthesizedTrack(track, startPositionMs)
        }
    }

    private fun playDeviceMedia(track: Track, startPositionMs: Long) {
        val uriStr = track.uri
        if (uriStr.isNullOrBlank()) {
            playSynthesizedTrack(track, startPositionMs)
            return
        }

        try {
            val parsedUri = Uri.parse(uriStr)
            val player = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                // Use openFileDescriptor for content:// URIs for rock-solid permission/sandboxing compatibility
                var dataSourceSet = false
                if (parsedUri.scheme == "content") {
                    try {
                        context.contentResolver.openFileDescriptor(parsedUri, "r")?.use { pfd ->
                            setDataSource(pfd.fileDescriptor)
                            dataSourceSet = true
                        }
                    } catch (_: Exception) {}
                }
                if (!dataSourceSet) {
                    setDataSource(context, parsedUri)
                }

                setOnPreparedListener { mp ->
                    if (startPositionMs > 0) {
                        try {
                            mp.seekTo(startPositionMs.toInt())
                        } catch (_: Exception) {}
                    }
                    applyVolume()
                    mp.start()
                    isCurrentlyPlaying = true
                }
                setOnCompletionListener {
                    isCurrentlyPlaying = false
                    onTrackCompleted()
                }
                setOnErrorListener { mp, what, extra ->
                    isCurrentlyPlaying = false
                    try {
                        mp.setOnCompletionListener(null)
                        mp.setOnErrorListener(null)
                        mp.reset()
                        mp.release()
                    } catch (_: Exception) {}
                    mediaPlayer = null
                    // Fallback to internal synth instead of triggering rapid onTrackCompleted loop
                    playSynthesizedTrack(track, startPositionMs)
                    true // Returning true indicates the error is handled and suppresses OnCompletionListener
                }
                prepareAsync()
            }
            mediaPlayer = player
        } catch (_: Exception) {
            // Fallback to internal synth if media source fails
            playSynthesizedTrack(track, startPositionMs)
        }
    }

    private fun playSynthesizedTrack(track: Track, startPositionMs: Long) {
        val sampleRate = 44100
        val bufferSize = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_STEREO,
            AudioFormat.ENCODING_PCM_16BIT
        ) * 2

        synthTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                    .build()
            )
            .setBufferSizeInBytes(bufferSize)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        synthTrack?.play()
        isCurrentlyPlaying = true

        val chordProgressions = when (track.synthScaleType) {
            1 -> listOf(220.0, 261.63, 329.63, 392.0)   // Energetic A minor 7th
            2 -> listOf(144.0, 216.0, 288.0, 432.0)    // 432Hz sleep resonance
            3 -> listOf(261.63, 329.63, 392.0, 523.25) // Acoustic C Major
            else -> listOf(196.0, 246.94, 293.66, 369.99) // Chill G Major
        }

        synthJob = scope.launch {
            val chunkSamples = 2048
            val pcmBuffer = ShortArray(chunkSamples * 2) // Stereo
            var phase = 0.0
            val startTime = System.currentTimeMillis() - startPositionMs
            val rawDuration = track.durationMs
            val totalDuration = if (rawDuration > 1000L) rawDuration else 180000L

            while (isActive && isCurrentlyPlaying) {
                val elapsed = System.currentTimeMillis() - startTime
                currentPositionMillis = elapsed

                if (elapsed >= totalDuration) {
                    isCurrentlyPlaying = false
                    launch(Dispatchers.Main) { onTrackCompleted() }
                    break
                }

                val chordIdx = ((elapsed / 2500) % chordProgressions.size).toInt()
                val baseFreq = chordProgressions[chordIdx]
                val subFreq = baseFreq / 2.0

                val vol = effectiveVolume

                for (i in 0 until chunkSamples) {
                    val t = (phase + i) / sampleRate
                    // Harmonic blend of fundamental + fifth + soft pulse
                    val sample1 = sin(2.0 * Math.PI * baseFreq * t) * 0.4
                    val sample2 = sin(2.0 * Math.PI * (baseFreq * 1.5) * t) * 0.25
                    val sampleSub = sin(2.0 * Math.PI * subFreq * t) * 0.2

                    val lfo = (sin(2.0 * Math.PI * 0.5 * t) + 1.0) * 0.5
                    val combined = ((sample1 + sample2 + sampleSub) * (0.6 + 0.4 * lfo) * vol * 32767.0)
                        .coerceIn(-32768.0, 32767.0)

                    val shortVal = combined.toInt().toShort()
                    pcmBuffer[i * 2] = shortVal     // Left
                    pcmBuffer[i * 2 + 1] = shortVal // Right
                }

                phase += chunkSamples
                synthTrack?.write(pcmBuffer, 0, pcmBuffer.size)
                delay(15)
            }
        }
    }

    fun resume() {
        if (!isCurrentlyPlaying && currentPlayingTrack != null) {
            mediaPlayer?.let {
                try {
                    applyVolume()
                    it.start()
                    isCurrentlyPlaying = true
                    return
                } catch (_: Exception) {}
            }
            synthTrack?.let {
                isCurrentlyPlaying = true
                playSynthesizedTrack(currentPlayingTrack!!, currentPositionMillis)
            } ?: run {
                playTrack(currentPlayingTrack!!, currentPositionMillis)
            }
        }
    }

    fun pause() {
        isCurrentlyPlaying = false
        try {
            mediaPlayer?.pause()
        } catch (_: Exception) {}
        synthJob?.cancel()
        synthJob = null
        try {
            synthTrack?.pause()
        } catch (_: Exception) {}
    }

    fun stop() {
        isCurrentlyPlaying = false
        try {
            mediaPlayer?.setOnCompletionListener(null)
            mediaPlayer?.setOnErrorListener(null)
            mediaPlayer?.setOnPreparedListener(null)
            mediaPlayer?.reset()
            mediaPlayer?.release()
        } catch (_: Exception) {}
        mediaPlayer = null

        synthJob?.cancel()
        synthJob = null
        try {
            synthTrack?.stop()
            synthTrack?.release()
        } catch (_: Exception) {}
        synthTrack = null
    }

    fun seekTo(positionMs: Long) {
        currentPositionMillis = positionMs
        mediaPlayer?.let {
            try {
                it.seekTo(positionMs.toInt())
            } catch (_: Exception) {}
        }
        if (synthJob != null && currentPlayingTrack != null) {
            // Restart synth at new position
            synthJob?.cancel()
            playSynthesizedTrack(currentPlayingTrack!!, positionMs)
        }
    }

    fun setUserVolume(volume: Float) {
        userVolume = volume.coerceIn(0.0f, 1.0f)
        applyVolume()
    }

    fun setHearingSafety(enabled: Boolean, maxSafeCapPercent: Int) {
        isHearingSafetyActive = enabled
        hearingSafetyCap = (maxSafeCapPercent / 100.0f).coerceIn(0.3f, 1.0f)
        applyVolume()
    }

    fun setFadeMultiplier(multiplier: Float) {
        fadeMultiplier = multiplier.coerceIn(0.0f, 1.0f)
        applyVolume()
    }

    fun setLoudnessNormalization(enabled: Boolean, targetLufs: Float) {
        isLoudnessNormalizationActive = enabled
        targetLoudnessLufs = targetLufs
        applyVolume()
    }

    private fun applyVolume() {
        val vol = effectiveVolume
        try {
            mediaPlayer?.setVolume(vol, vol)
        } catch (_: Exception) {}
    }
}
