package com.example.audio

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.graphics.drawable.BitmapDrawable
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationCompat
import androidx.media.app.NotificationCompat.MediaStyle
import coil.Coil
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.example.MainActivity
import com.example.R
import com.example.data.model.Track
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Foreground Service hosting the system-recognized MediaSessionCompat and MediaStyle notification.
 * Enables Lock Screen media player, Notification Shade wave progress bar, and background playback.
 */
class MediaPlaybackService : Service() {

    companion object {
        const val CHANNEL_ID = "playlish_media_playback_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_PLAY = "com.example.playlish.ACTION_PLAY"
        const val ACTION_PAUSE = "com.example.playlish.ACTION_PAUSE"
        const val ACTION_NEXT = "com.example.playlish.ACTION_NEXT"
        const val ACTION_PREV = "com.example.playlish.ACTION_PREV"
        const val ACTION_STOP = "com.example.playlish.ACTION_STOP"
        const val ACTION_SEEK_TO = "com.example.playlish.ACTION_SEEK_TO"
        const val ACTION_SYNC_STATE = "com.example.playlish.ACTION_SYNC_STATE"

        const val EXTRA_SEEK_POSITION = "extra_seek_position"
    }

    private lateinit var mediaSession: MediaSessionCompat
    private lateinit var audioManager: AudioManager
    private lateinit var notificationManager: NotificationManager

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var isForegroundActive = false
    private var lastRenderedTrackId: String? = null
    private var cachedAlbumArt: Bitmap? = null

    // Audio Focus
    private var audioFocusRequest: AudioFocusRequest? = null
    private val audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS -> {
                MusicPlaybackCoordinator.onPauseAction?.invoke()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                MusicPlaybackCoordinator.onPauseAction?.invoke()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                // Background ducking handled if needed
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                // Audio focus recovered
            }
        }
    }

    // Unplugged headphones / Bluetooth disconnected receiver
    private var isNoisyReceiverRegistered = false
    private val becomingNoisyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (AudioManager.ACTION_AUDIO_BECOMING_NOISY == intent?.action) {
                MusicPlaybackCoordinator.onPauseAction?.invoke()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        createNotificationChannel()
        setupMediaSession()

        // Observe state changes from MusicPlaybackCoordinator
        serviceScope.launch {
            MusicPlaybackCoordinator.playbackInfo.collectLatest { info ->
                updateNotificationAndSession(info)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY -> {
                requestAudioFocus()
                MusicPlaybackCoordinator.onPlayAction?.invoke()
            }
            ACTION_PAUSE -> {
                MusicPlaybackCoordinator.onPauseAction?.invoke()
            }
            ACTION_NEXT -> {
                MusicPlaybackCoordinator.onNextAction?.invoke()
            }
            ACTION_PREV -> {
                MusicPlaybackCoordinator.onPrevAction?.invoke()
            }
            ACTION_STOP -> {
                abandonAudioFocus()
                MusicPlaybackCoordinator.onStopAction?.invoke()
                stopForegroundService()
            }
            ACTION_SEEK_TO -> {
                val pos = intent.getLongExtra(EXTRA_SEEK_POSITION, 0L)
                MusicPlaybackCoordinator.onSeekAction?.invoke(pos)
            }
            ACTION_SYNC_STATE -> {
                val currentInfo = MusicPlaybackCoordinator.playbackInfo.value
                updateNotificationAndSession(currentInfo)
            }
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        unregisterNoisyReceiver()
        abandonAudioFocus()
        mediaSession.isActive = false
        mediaSession.release()
    }

    private fun setupMediaSession() {
        mediaSession = MediaSessionCompat(this, "PlayLishLiteMediaSession").apply {
            setFlags(
                MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or
                        MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS
            )

            setCallback(object : MediaSessionCompat.Callback() {
                override fun onPlay() {
                    requestAudioFocus()
                    MusicPlaybackCoordinator.onPlayAction?.invoke()
                }

                override fun onPause() {
                    MusicPlaybackCoordinator.onPauseAction?.invoke()
                }

                override fun onSkipToNext() {
                    MusicPlaybackCoordinator.onNextAction?.invoke()
                }

                override fun onSkipToPrevious() {
                    MusicPlaybackCoordinator.onPrevAction?.invoke()
                }

                override fun onSeekTo(pos: Long) {
                    MusicPlaybackCoordinator.onSeekAction?.invoke(pos)
                }

                override fun onStop() {
                    abandonAudioFocus()
                    MusicPlaybackCoordinator.onStopAction?.invoke()
                    stopForegroundService()
                }

                override fun onCustomAction(action: String?, extras: android.os.Bundle?) {
                    super.onCustomAction(action, extras)
                }
            })

            isActive = true
        }
    }

    private fun updateNotificationAndSession(info: MusicPlaybackCoordinator.PlaybackInfo) {
        val track = info.track
        if (track == null) {
            if (!info.isPlaying) {
                stopForegroundService()
            }
            return
        }

        // Manage Becoming Noisy receiver based on playback
        if (info.isPlaying) {
            registerNoisyReceiver()
            requestAudioFocus()
        } else {
            unregisterNoisyReceiver()
        }

        // 1. Update PlaybackState in MediaSession (Used by Lock Screen & System Wave Progress Bar)
        val state = if (info.isPlaying) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED
        val playbackSpeed = if (info.isPlaying) 1.0f else 0.0f
        val playbackState = PlaybackStateCompat.Builder()
            .setActions(
                PlaybackStateCompat.ACTION_PLAY or
                        PlaybackStateCompat.ACTION_PAUSE or
                        PlaybackStateCompat.ACTION_PLAY_PAUSE or
                        PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                        PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                        PlaybackStateCompat.ACTION_SEEK_TO or
                        PlaybackStateCompat.ACTION_STOP
            )
            .setState(state, info.positionMs, playbackSpeed)
            .build()
        mediaSession.setPlaybackState(playbackState)

        // 2. Load Artwork Asynchronously if track changed
        serviceScope.launch {
            val artworkBitmap = if (lastRenderedTrackId == track.id && cachedAlbumArt != null) {
                cachedAlbumArt!!
            } else {
                val loaded = loadTrackArtwork(track)
                cachedAlbumArt = loaded
                lastRenderedTrackId = track.id
                loaded
            }

            // Update Metadata in MediaSession (Displayed on Lock Screen, Smartwatches & Media Controls)
            val metadata = MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, track.title)
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, track.artist)
                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, track.album)
                .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, info.durationMs.coerceAtLeast(track.durationMs))
                .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, artworkBitmap)
                .putBitmap(MediaMetadataCompat.METADATA_KEY_ART, artworkBitmap)
                .putBitmap(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON, artworkBitmap)
                .build()
            mediaSession.setMetadata(metadata)

            // 3. Build the MediaStyle Notification
            val notification = buildMediaNotification(track, info.isPlaying, artworkBitmap)

            // 4. Start foreground or update existing notification
            if (info.isPlaying) {
                if (!isForegroundActive) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        startForeground(
                            NOTIFICATION_ID,
                            notification,
                            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                        )
                    } else {
                        startForeground(NOTIFICATION_ID, notification)
                    }
                    isForegroundActive = true
                } else {
                    notificationManager.notify(NOTIFICATION_ID, notification)
                }
            } else {
                // When paused, detach foreground so user can dismiss it if they want, but keep notification visible
                if (isForegroundActive) {
                    stopForeground(STOP_FOREGROUND_DETACH)
                    isForegroundActive = false
                }
                notificationManager.notify(NOTIFICATION_ID, notification)
            }
        }
    }

    private fun buildMediaNotification(
        track: Track,
        isPlaying: Boolean,
        artwork: Bitmap
    ): Notification {
        // PendingIntent to launch MainActivity when clicking notification body
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Notification Action Intents
        val prevPendingIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, MediaPlaybackService::class.java).apply { action = ACTION_PREV },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val playPauseAction = if (isPlaying) ACTION_PAUSE else ACTION_PLAY
        val playPausePendingIntent = PendingIntent.getService(
            this,
            2,
            Intent(this, MediaPlaybackService::class.java).apply { action = playPauseAction },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val nextPendingIntent = PendingIntent.getService(
            this,
            3,
            Intent(this, MediaPlaybackService::class.java).apply { action = ACTION_NEXT },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopPendingIntent = PendingIntent.getService(
            this,
            4,
            Intent(this, MediaPlaybackService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Actions for Previous, Play/Pause, Next
        val prevAction = NotificationCompat.Action(
            R.drawable.ic_notif_prev,
            "Sebelumnya",
            prevPendingIntent
        )
        val playPauseIcon = if (isPlaying) R.drawable.ic_notif_pause else R.drawable.ic_notif_play
        val playPauseTitle = if (isPlaying) "Jeda" else "Putar"
        val playAction = NotificationCompat.Action(
            playPauseIcon,
            playPauseTitle,
            playPausePendingIntent
        )
        val nextAction = NotificationCompat.Action(
            R.drawable.ic_notif_next,
            "Selanjutnya",
            nextPendingIntent
        )

        // MediaStyle linking directly with the MediaSession token
        val mediaStyle = MediaStyle()
            .setMediaSession(mediaSession.sessionToken)
            .setShowActionsInCompactView(0, 1, 2)
            .setShowCancelButton(true)
            .setCancelButtonIntent(stopPendingIntent)

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_playlish_notif_small)
            .setContentTitle(track.title)
            .setContentText(track.artist)
            .setSubText(track.album.ifBlank { "PlayLish Lite" })
            .setLargeIcon(artwork)
            .setContentIntent(openAppPendingIntent)
            .setDeleteIntent(stopPendingIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOnlyAlertOnce(true)
            .setOngoing(isPlaying)
            .setShowWhen(false)
            .addAction(prevAction)
            .addAction(playAction)
            .addAction(nextAction)
            .setStyle(mediaStyle)

        return builder.build()
    }

    private suspend fun loadTrackArtwork(track: Track): Bitmap {
        return withContext(Dispatchers.IO) {
            if (!track.albumArtUri.isNullOrBlank()) {
                try {
                    val imageLoader = Coil.imageLoader(this@MediaPlaybackService)
                    val request = ImageRequest.Builder(this@MediaPlaybackService)
                        .data(Uri.parse(track.albumArtUri))
                        .allowHardware(false) // Software bitmap required for Notification & MediaSession
                        .size(512, 512)
                        .build()
                    val result = (imageLoader.execute(request) as? SuccessResult)?.drawable
                    if (result is BitmapDrawable && result.bitmap != null) {
                        return@withContext result.bitmap
                    }
                } catch (_: Exception) {}
            }
            createFallbackVinylBitmap(track.title)
        }
    }

    /**
     * Generates a sleek, sporty vinyl disc bitmap as album art fallback.
     */
    private fun createFallbackVinylBitmap(title: String): Bitmap {
        val size = 400
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Background vinyl groove gradient
        val center = size / 2f
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(
                center, center, center,
                intArrayOf(Color.rgb(38, 42, 50), Color.rgb(18, 20, 24), Color.rgb(8, 9, 11)),
                floatArrayOf(0.0f, 0.7f, 1.0f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawCircle(center, center, center - 8, paint)

        // Groove rings
        val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = Color.argb(45, 255, 255, 255)
            strokeWidth = 2f
        }
        canvas.drawCircle(center, center, center * 0.85f, ringPaint)
        canvas.drawCircle(center, center, center * 0.70f, ringPaint)
        canvas.drawCircle(center, center, center * 0.55f, ringPaint)

        // Sport Center Label (Sport Orange disc)
        val centerDiscPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(255, 122, 0) // SportOrange
        }
        canvas.drawCircle(center, center, center * 0.35f, centerDiscPaint)

        // Spindle hole
        val holePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(12, 13, 16)
        }
        canvas.drawCircle(center, center, center * 0.10f, holePaint)

        // Initial Letter
        val letter = title.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "P"
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 54f
            textAlign = Paint.Align.CENTER
            isFakeBoldText = true
        }
        val yOffset = (textPaint.descent() + textPaint.ascent()) / 2
        canvas.drawText(letter, center, center - yOffset, textPaint)

        return bitmap
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Pemutar Musik PlayLish",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Kontrol pemutar musik di bilah notifikasi dan layar kunci"
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun requestAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val playbackAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()
            audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(playbackAttributes)
                .setAcceptsDelayedFocusGain(true)
                .setOnAudioFocusChangeListener(audioFocusChangeListener)
                .build()
            audioFocusRequest?.let { audioManager.requestAudioFocus(it) }
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(
                audioFocusChangeListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
            )
        }
    }

    private fun abandonAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(audioFocusChangeListener)
        }
    }

    private fun registerNoisyReceiver() {
        if (!isNoisyReceiverRegistered) {
            val filter = IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
            registerReceiver(becomingNoisyReceiver, filter)
            isNoisyReceiverRegistered = true
        }
    }

    private fun unregisterNoisyReceiver() {
        if (isNoisyReceiverRegistered) {
            try {
                unregisterReceiver(becomingNoisyReceiver)
            } catch (_: Exception) {}
            isNoisyReceiverRegistered = false
        }
    }

    private fun stopForegroundService() {
        isForegroundActive = false
        stopForeground(STOP_FOREGROUND_REMOVE)
        notificationManager.cancel(NOTIFICATION_ID)
        stopSelf()
    }
}
