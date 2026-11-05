package com.example.audio

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.ContextCompat
import com.example.data.model.Track
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Singleton coordinator bridging the UI/ViewModel and MediaPlaybackService.
 * Ensures MediaSession events (lock screen, notification shade, bluetooth, headset)
 * directly invoke the active player without creating duplicate instances.
 */
object MusicPlaybackCoordinator {

    data class PlaybackInfo(
        val track: Track? = null,
        val isPlaying: Boolean = false,
        val positionMs: Long = 0L,
        val durationMs: Long = 0L
    )

    private val _playbackInfo = MutableStateFlow(PlaybackInfo())
    val playbackInfo: StateFlow<PlaybackInfo> = _playbackInfo.asStateFlow()

    // Callbacks hooked directly to ViewModel methods
    var onPlayAction: (() -> Unit)? = null
    var onPauseAction: (() -> Unit)? = null
    var onNextAction: (() -> Unit)? = null
    var onPrevAction: (() -> Unit)? = null
    var onSeekAction: ((Long) -> Unit)? = null
    var onStopAction: (() -> Unit)? = null

    /**
     * Called by MusicPlayerViewModel whenever track or playback state changes.
     */
    fun updatePlaybackState(
        context: Context,
        track: Track?,
        isPlaying: Boolean,
        positionMs: Long,
        durationMs: Long
    ) {
        _playbackInfo.value = PlaybackInfo(
            track = track,
            isPlaying = isPlaying,
            positionMs = positionMs,
            durationMs = durationMs
        )

        val intent = Intent(context, MediaPlaybackService::class.java).apply {
            action = MediaPlaybackService.ACTION_SYNC_STATE
        }

        try {
            if (isPlaying) {
                ContextCompat.startForegroundService(context, intent)
            } else if (track != null) {
                context.startService(intent)
            }
        } catch (_: Exception) {
            // Guard against background start restrictions if app is already backgrounded
        }
    }

    /**
     * Stop and detach the foreground media service.
     */
    fun stopService(context: Context) {
        val intent = Intent(context, MediaPlaybackService::class.java).apply {
            action = MediaPlaybackService.ACTION_STOP
        }
        try {
            context.startService(intent)
        } catch (_: Exception) {}
    }
}
