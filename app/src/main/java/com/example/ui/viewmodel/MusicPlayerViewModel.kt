package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.AudioPlayerEngine
import com.example.audio.MusicPlaybackCoordinator
import com.example.audio.OrderedShuffleQueue
import com.example.data.model.LyricLine
import com.example.data.model.LyricsParser
import com.example.data.model.Playlist
import com.example.data.model.PlaylistSortOption
import com.example.data.model.Track
import com.example.data.model.TrackSortOption
import com.example.data.model.UserSettings
import com.example.data.preferences.SettingsDataStore
import com.example.data.repository.MusicRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class PlaybackRepeatMode {
    OFF, ALL, ONE
}

data class PlayerUiState(
    val currentTrack: Track? = null,
    val isPlaying: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val isShuffleEnabled: Boolean = false,
    val shuffleVersion: Long = 0L,
    val repeatMode: PlaybackRepeatMode = PlaybackRepeatMode.ALL,
    val shuffleOrderMap: Map<String, Int> = emptyMap(),
    val currentQueuePosition: Int = 1,
    val totalQueueCount: Int = 0,
    val currentLyrics: List<LyricLine> = emptyList(),
    val activeLyricIndex: Int = -1,
    val isSleepTimerActive: Boolean = false,
    val sleepTimerRemainingSeconds: Long = 0L,
    val isWaitingForTrackEndToSleep: Boolean = false,
    val currentDecibels: Float = -12.0f,
    val effectiveVolumePercent: Int = 75,
    val selectedCategory: String = "Semua",
    val searchQuery: String = "",
    val sortQueueByShuffleOrder: Boolean = false,
    val trackSortOption: TrackSortOption = TrackSortOption.DEFAULT,
    val playlistSortOption: PlaylistSortOption = PlaylistSortOption.DATE_NEWEST,
    val isScanningDevice: Boolean = false,
    val isFullPlayerOpen: Boolean = false,
    val showSleepTimerDialog: Boolean = false,
    val showEqualizerDialog: Boolean = false,
    val showSettingsDialog: Boolean = false,
    val showLyricsFullScreen: Boolean = false,
    val showSortDialog: Boolean = false,
    val loudnessNormalizationActive: Boolean = true,
    val appliedNormalizationGainDb: Float = 0.0f,
    val trackToDelete: Track? = null,
    val playlistToDelete: Playlist? = null,
    val importStatusMessage: String? = null,
    val playlists: List<Playlist> = emptyList(),
    val selectedPlaylistId: String? = null,
    val showPlaylistModal: Boolean = false,
    val showCreatePlaylistDialog: Boolean = false,
    val trackToAddToPlaylist: Track? = null,
    val trackToImportLyrics: Track? = null,
    val showPasteLyricsDialog: Boolean = false
)

class MusicPlayerViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MusicRepository(application.applicationContext)
    private val settingsDataStore = SettingsDataStore(application.applicationContext)
    private val shuffleQueue = OrderedShuffleQueue()

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private val _allTracks = MutableStateFlow<List<Track>>(emptyList())
    val allTracks: StateFlow<List<Track>> = _allTracks.asStateFlow()

    val userSettings: StateFlow<UserSettings> = settingsDataStore.userSettingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = UserSettings()
    )

    private val audioEngine: AudioPlayerEngine = AudioPlayerEngine(
        context = application.applicationContext,
        onTrackCompleted = { handleTrackCompletion() }
    )

    private var playbackProgressJob: Job? = null
    private var sleepTimerJob: Job? = null
    private var waitingForTrackToEndToSleep: Boolean = false

    init {
        setupMediaCoordinator()
        loadInitialTracks()
        observeSettings()
        startProgressTracker()
    }

    private fun setupMediaCoordinator() {
        MusicPlaybackCoordinator.onPlayAction = { resume() }
        MusicPlaybackCoordinator.onPauseAction = { pause() }
        MusicPlaybackCoordinator.onNextAction = { playNext() }
        MusicPlaybackCoordinator.onPrevAction = { playPrevious() }
        MusicPlaybackCoordinator.onSeekAction = { pos -> seekTo(pos) }
        MusicPlaybackCoordinator.onStopAction = {
            pause()
            MusicPlaybackCoordinator.stopService(getApplication())
        }
    }

    private fun loadInitialTracks() {
        viewModelScope.launch {
            val savedTracks = repository.getSavedTracks()
            val savedPlaylists = repository.getPlaylists()
            _allTracks.value = savedTracks
            _uiState.value = _uiState.value.copy(
                playlists = savedPlaylists
            )
            syncQueueWithFilteredTracks()

            // Set initial track if user already has saved tracks
            if (savedTracks.isNotEmpty()) {
                val initialTrack = savedTracks.first()
                _uiState.value = _uiState.value.copy(
                    currentTrack = initialTrack,
                    durationMs = initialTrack.durationMs,
                    currentLyrics = LyricsParser.parse(initialTrack.rawLyrics),
                    totalQueueCount = savedTracks.size
                )
            }
        }
    }

    private fun observeSettings() {
        viewModelScope.launch {
            userSettings.collect { settings ->
                audioEngine.setHearingSafety(
                    enabled = settings.hearingProtectionEnabled,
                    maxSafeCapPercent = settings.maxSafeVolumePercent
                )
                audioEngine.setLoudnessNormalization(
                    enabled = settings.loudnessNormalizationEnabled,
                    targetLufs = settings.targetLoudnessLufs
                )
                if (settings.isShuffleEnabled != _uiState.value.isShuffleEnabled) {
                    toggleShuffle(explicitShuffleState = settings.isShuffleEnabled)
                }
                _uiState.value = _uiState.value.copy(
                    loudnessNormalizationActive = settings.loudnessNormalizationEnabled
                )
            }
        }
    }

    private fun startProgressTracker() {
        playbackProgressJob?.cancel()
        playbackProgressJob = viewModelScope.launch {
            while (isActive) {
                if (audioEngine.isPlaying) {
                    val pos = audioEngine.currentPosition
                    val dur = audioEngine.duration.coerceAtLeast(1L)
                    val lyrics = _uiState.value.currentLyrics
                    val activeLyric = LyricsParser.findActiveIndex(lyrics, pos)

                    _uiState.value = _uiState.value.copy(
                        currentPositionMs = pos,
                        durationMs = dur,
                        isPlaying = true,
                        activeLyricIndex = activeLyric,
                        currentDecibels = audioEngine.currentDecibels,
                        effectiveVolumePercent = (audioEngine.effectiveVolume * 100).toInt(),
                        appliedNormalizationGainDb = audioEngine.appliedNormalizationGainDb
                    )
                }
                delay(100)
            }
        }
    }

    private fun syncQueueWithFilteredTracks() {
        val tracks = getFilteredTracks()
        val currentId = _uiState.value.currentTrack?.id
        val isShuffle = _uiState.value.isShuffleEnabled
        shuffleQueue.setupQueue(tracks, isShuffle, currentId)

        val queuePos = if (isShuffle) {
            if (currentId != null) shuffleQueue.getOrderBadge(currentId) else 1
        } else {
            val idx = tracks.indexOfFirst { it.id == currentId }
            if (idx != -1) idx + 1 else 1
        }

        _uiState.value = _uiState.value.copy(
            shuffleOrderMap = shuffleQueue.orderIndexMap,
            totalQueueCount = tracks.size,
            currentQueuePosition = queuePos
        )
    }

    fun getFilteredTracks(): List<Track> {
        val category = _uiState.value.selectedCategory
        val query = _uiState.value.searchQuery.trim().lowercase()
        val selectedPlId = _uiState.value.selectedPlaylistId

        var filtered = _allTracks.value.filter { track ->
            when (category) {
                "Semua" -> true
                "Hi-Res" -> track.isHiRes
                "Enerjik" -> track.category == "Enerjik"
                "Pengantar Tidur" -> track.category == "Pengantar Tidur"
                "Perangkat" -> track.isDeviceFile
                else -> true
            }
        }

        // Filter by selected playlist while preserving exact playlist track sequence
        if (selectedPlId != null) {
            val playlist = _uiState.value.playlists.find { it.id == selectedPlId }
            if (playlist != null) {
                val trackMap = _allTracks.value.associateBy { it.id }
                filtered = playlist.trackIds.mapNotNull { trackMap[it] }
            } else {
                filtered = emptyList()
            }
        }

        if (query.isNotEmpty()) {
            filtered = filtered.filter {
                it.title.lowercase().contains(query) ||
                        it.artist.lowercase().contains(query) ||
                        it.album.lowercase().contains(query)
            }
        }

        filtered = when (_uiState.value.trackSortOption) {
            TrackSortOption.DEFAULT -> {
                if (_uiState.value.isShuffleEnabled) {
                    filtered.sortedBy { shuffleQueue.getOrderBadge(it.id) }
                } else filtered
            }
            TrackSortOption.TITLE_ASC -> filtered.sortedBy { it.title.lowercase() }
            TrackSortOption.TITLE_DESC -> filtered.sortedByDescending { it.title.lowercase() }
            TrackSortOption.ARTIST_ASC -> filtered.sortedBy { it.artist.lowercase() }
            TrackSortOption.DURATION_DESC -> filtered.sortedByDescending { it.durationMs }
            TrackSortOption.DURATION_ASC -> filtered.sortedBy { it.durationMs }
            TrackSortOption.SHUFFLE_ORDER -> {
                filtered.sortedBy { shuffleQueue.getOrderBadge(it.id) }
            }
        }

        return filtered
    }

    // --- Audio Playback Controls ---

    fun playTrack(track: Track) {
        val isSameTrack = _uiState.value.currentTrack?.id == track.id
        if (isSameTrack && _uiState.value.isPlaying && audioEngine.isPlaying) {
            pause()
            return
        }

        waitingForTrackToEndToSleep = false
        audioEngine.playTrack(track)
        shuffleQueue.moveToTrack(track.id)

        val tracks = getFilteredTracks()
        val currentQueuePos = if (_uiState.value.isShuffleEnabled) {
            shuffleQueue.getOrderBadge(track.id)
        } else {
            val idx = tracks.indexOfFirst { it.id == track.id }
            if (idx != -1) idx + 1 else 1
        }

        _uiState.value = _uiState.value.copy(
            currentTrack = track,
            isPlaying = true,
            currentPositionMs = 0L,
            durationMs = track.durationMs,
            currentLyrics = LyricsParser.parse(track.rawLyrics),
            activeLyricIndex = 0,
            currentQueuePosition = currentQueuePos,
            totalQueueCount = tracks.size
        )

        MusicPlaybackCoordinator.updatePlaybackState(
            context = getApplication(),
            track = track,
            isPlaying = true,
            positionMs = 0L,
            durationMs = track.durationMs
        )
    }

    fun togglePlayPause() {
        if (_uiState.value.isPlaying) {
            pause()
        } else {
            resume()
        }
    }

    fun pause() {
        audioEngine.pause()
        _uiState.value = _uiState.value.copy(isPlaying = false)
        MusicPlaybackCoordinator.updatePlaybackState(
            context = getApplication(),
            track = _uiState.value.currentTrack,
            isPlaying = false,
            positionMs = _uiState.value.currentPositionMs,
            durationMs = _uiState.value.durationMs
        )
    }

    fun resume() {
        val track = _uiState.value.currentTrack
        if (track == null && _allTracks.value.isNotEmpty()) {
            playTrack(_allTracks.value.first())
            return
        }
        if (track != null) {
            if (!audioEngine.isPlaying) {
                if (audioEngine.currentTrack == null || audioEngine.currentTrack?.id != track.id) {
                    audioEngine.playTrack(track, _uiState.value.currentPositionMs)
                } else {
                    audioEngine.resume()
                }
            }
        }
        _uiState.value = _uiState.value.copy(isPlaying = true)
        MusicPlaybackCoordinator.updatePlaybackState(
            context = getApplication(),
            track = _uiState.value.currentTrack,
            isPlaying = true,
            positionMs = _uiState.value.currentPositionMs,
            durationMs = _uiState.value.durationMs
        )
    }

    fun seekTo(positionMs: Long) {
        audioEngine.seekTo(positionMs)
        _uiState.value = _uiState.value.copy(currentPositionMs = positionMs)
        MusicPlaybackCoordinator.updatePlaybackState(
            context = getApplication(),
            track = _uiState.value.currentTrack,
            isPlaying = _uiState.value.isPlaying,
            positionMs = positionMs,
            durationMs = _uiState.value.durationMs
        )
    }

    fun playNext() {
        val tracks = getFilteredTracks()
        if (tracks.isEmpty()) return

        val isShuffle = _uiState.value.isShuffleEnabled
        val currentTrackId = _uiState.value.currentTrack?.id
        val repeatMode = _uiState.value.repeatMode

        val nextTrack = if (isShuffle) {
            val nextId = shuffleQueue.moveToNext(repeatAll = repeatMode != PlaybackRepeatMode.OFF)
            tracks.find { it.id == nextId } ?: _allTracks.value.find { it.id == nextId }
        } else {
            // Natural playlist order: strictly sequential
            val currentIndex = tracks.indexOfFirst { it.id == currentTrackId }
            if (currentIndex != -1) {
                val nextIndex = currentIndex + 1
                if (nextIndex < tracks.size) {
                    tracks[nextIndex]
                } else if (repeatMode != PlaybackRepeatMode.OFF) {
                    tracks.firstOrNull()
                } else {
                    null
                }
            } else {
                tracks.firstOrNull()
            }
        }

        if (nextTrack != null) {
            playTrack(nextTrack)
        } else {
            // Reached the end of queue in repeat OFF mode
            pause()
            seekTo(0L)
        }
    }

    fun playPrevious() {
        // If current position > 3 seconds, restart current track
        if (_uiState.value.currentPositionMs > 3000L) {
            seekTo(0L)
            return
        }

        val tracks = getFilteredTracks()
        if (tracks.isEmpty()) return

        val isShuffle = _uiState.value.isShuffleEnabled
        val currentTrackId = _uiState.value.currentTrack?.id
        val repeatMode = _uiState.value.repeatMode

        val prevTrack = if (isShuffle) {
            val prevId = shuffleQueue.moveToPrevious()
            tracks.find { it.id == prevId } ?: _allTracks.value.find { it.id == prevId }
        } else {
            // Natural playlist order: strictly sequential
            val currentIndex = tracks.indexOfFirst { it.id == currentTrackId }
            if (currentIndex != -1) {
                val prevIndex = currentIndex - 1
                if (prevIndex >= 0) {
                    tracks[prevIndex]
                } else if (repeatMode != PlaybackRepeatMode.OFF) {
                    tracks.lastOrNull()
                } else {
                    tracks.firstOrNull()
                }
            } else {
                tracks.firstOrNull()
            }
        }

        if (prevTrack != null) {
            playTrack(prevTrack)
        }
    }

    private fun handleTrackCompletion() {
        if (_uiState.value.repeatMode == PlaybackRepeatMode.ONE) {
            _uiState.value.currentTrack?.let { playTrack(it) }
            return
        }

        if (waitingForTrackToEndToSleep) {
            // Sleep timer expired and waited for track to end!
            waitingForTrackToEndToSleep = false
            stopAndSleep()
            return
        }

        playNext()
    }

    // --- Ordered Shuffle Feature ---

    /**
     * Toggles shuffle mode.
     * When enabled, assigns explicit ordered numbers (#1, #2, #3, ...) to all songs.
     */
    fun toggleShuffle(explicitShuffleState: Boolean? = null) {
        val newShuffle = explicitShuffleState ?: !_uiState.value.isShuffleEnabled
        val currentTrackId = _uiState.value.currentTrack?.id
        val tracks = _allTracks.value
        val targetTracks = if (_uiState.value.selectedPlaylistId != null) {
            val pl = _uiState.value.playlists.find { it.id == _uiState.value.selectedPlaylistId }
            val map = tracks.associateBy { it.id }
            pl?.trackIds?.mapNotNull { map[it] } ?: tracks
        } else {
            tracks
        }

        shuffleQueue.setupQueue(targetTracks, newShuffle, currentTrackId)

        val queuePos = if (newShuffle) {
            if (currentTrackId != null) shuffleQueue.getOrderBadge(currentTrackId) else 1
        } else {
            val idx = targetTracks.indexOfFirst { it.id == currentTrackId }
            if (idx != -1) idx + 1 else 1
        }

        _uiState.value = _uiState.value.copy(
            isShuffleEnabled = newShuffle,
            shuffleVersion = _uiState.value.shuffleVersion + 1L,
            shuffleOrderMap = shuffleQueue.orderIndexMap,
            currentQueuePosition = queuePos
        )

        viewModelScope.launch {
            settingsDataStore.updateShuffleState(newShuffle, _uiState.value.sortQueueByShuffleOrder)
        }
    }

    /**
     * Reshuffles the active tracks and visibly re-indexes order badges #1..#N.
     */
    fun reshuffle() {
        val tracks = _allTracks.value
        val currentTrackId = _uiState.value.currentTrack?.id
        val targetTracks = if (_uiState.value.selectedPlaylistId != null) {
            val pl = _uiState.value.playlists.find { it.id == _uiState.value.selectedPlaylistId }
            val map = tracks.associateBy { it.id }
            pl?.trackIds?.mapNotNull { map[it] } ?: tracks
        } else {
            tracks
        }

        val previousFirstId = shuffleQueue.playbackSequence.firstOrNull()
        shuffleQueue.reshuffle(targetTracks, currentTrackId, forceDifferentFromTrackId = previousFirstId)

        _uiState.value = _uiState.value.copy(
            isShuffleEnabled = true,
            shuffleVersion = _uiState.value.shuffleVersion + 1L,
            shuffleOrderMap = shuffleQueue.orderIndexMap,
            currentQueuePosition = if (currentTrackId != null) shuffleQueue.getOrderBadge(currentTrackId) else 1
        )
    }

    fun toggleSortQueueByShuffle() {
        val newSort = !_uiState.value.sortQueueByShuffleOrder
        _uiState.value = _uiState.value.copy(sortQueueByShuffleOrder = newSort)
        viewModelScope.launch {
            settingsDataStore.updateShuffleState(_uiState.value.isShuffleEnabled, newSort)
        }
    }

    fun cycleRepeatMode() {
        val nextMode = when (_uiState.value.repeatMode) {
            PlaybackRepeatMode.OFF -> PlaybackRepeatMode.ALL
            PlaybackRepeatMode.ALL -> PlaybackRepeatMode.ONE
            PlaybackRepeatMode.ONE -> PlaybackRepeatMode.OFF
        }
        _uiState.value = _uiState.value.copy(repeatMode = nextMode)
    }

    // --- Sleep Timer & Ear Protection Feature ---

    fun startSleepTimer(
        minutes: Int,
        finishCurrentSong: Boolean,
        fadeVolume: Boolean,
        fadeDurationSec: Int = 30
    ) {
        sleepTimerJob?.cancel()
        waitingForTrackToEndToSleep = false
        audioEngine.setFadeMultiplier(1.0f)

        val totalSeconds = minutes.toLong() * 60L
        val fadeSeconds = fadeDurationSec.toLong().coerceAtMost(totalSeconds)

        _uiState.value = _uiState.value.copy(
            isSleepTimerActive = true,
            sleepTimerRemainingSeconds = totalSeconds
        )

        viewModelScope.launch {
            settingsDataStore.updateSleepSettings(
                minutes = minutes,
                finishSong = finishCurrentSong,
                volumeFade = fadeVolume,
                fadeDurationSec = fadeDurationSec
            )
        }

        sleepTimerJob = viewModelScope.launch {
            var remaining = totalSeconds
            while (remaining > 0 && isActive) {
                delay(1000)
                remaining--
                _uiState.value = _uiState.value.copy(sleepTimerRemainingSeconds = remaining)

                // Gradual decibel/volume fade-out during the final seconds (default 30 detik)
                if (fadeVolume && remaining <= fadeSeconds && fadeSeconds > 0) {
                    val fadeFactor = (remaining.toFloat() / fadeSeconds.toFloat()).coerceIn(0.05f, 1.0f)
                    audioEngine.setFadeMultiplier(fadeFactor)
                }
            }

            // Timer reached 0
            if (finishCurrentSong && _uiState.value.isPlaying) {
                // Wait for the currently playing song to conclude before sleeping
                waitingForTrackToEndToSleep = true
                _uiState.value = _uiState.value.copy(
                    isWaitingForTrackEndToSleep = true,
                    sleepTimerRemainingSeconds = 0L
                )
            } else {
                stopAndSleep()
            }
        }
    }

    fun cancelSleepTimer() {
        sleepTimerJob?.cancel()
        sleepTimerJob = null
        waitingForTrackToEndToSleep = false
        audioEngine.setFadeMultiplier(1.0f)
        _uiState.value = _uiState.value.copy(
            isSleepTimerActive = false,
            isWaitingForTrackEndToSleep = false,
            sleepTimerRemainingSeconds = 0L
        )
    }

    fun extendSleepTimer(extraMinutes: Int) {
        val currentSec = _uiState.value.sleepTimerRemainingSeconds
        val addedSec = extraMinutes.toLong() * 60L
        val newTotalMinutes = ((currentSec + addedSec) / 60L).toInt().coerceAtLeast(1)
        startSleepTimer(
            minutes = newTotalMinutes,
            finishCurrentSong = userSettings.value.sleepModeFinishSong,
            fadeVolume = userSettings.value.sleepVolumeFade,
            fadeDurationSec = userSettings.value.sleepFadeDurationSeconds
        )
    }

    private fun stopAndSleep() {
        pause()
        audioEngine.setFadeMultiplier(1.0f)
        waitingForTrackToEndToSleep = false
        _uiState.value = _uiState.value.copy(
            isSleepTimerActive = false,
            isWaitingForTrackEndToSleep = false,
            sleepTimerRemainingSeconds = 0L
        )
    }

    fun updateHearingProtection(enabled: Boolean, maxVolumePercent: Int, targetDb: Float) {
        audioEngine.setHearingSafety(enabled, maxVolumePercent)
        _uiState.value = _uiState.value.copy(
            currentDecibels = audioEngine.currentDecibels,
            effectiveVolumePercent = (audioEngine.effectiveVolume * 100).toInt()
        )
        viewModelScope.launch {
            settingsDataStore.updateHearingProtection(enabled, maxVolumePercent, targetDb)
        }
    }

    fun setUserVolume(volumePercent: Int) {
        audioEngine.setUserVolume(volumePercent / 100.0f)
        _uiState.value = _uiState.value.copy(
            effectiveVolumePercent = (audioEngine.effectiveVolume * 100).toInt(),
            currentDecibels = audioEngine.currentDecibels
        )
    }

    // --- Equalizer & Settings ---

    fun setEqualizerPreset(preset: String, bassBoost: Int) {
        viewModelScope.launch {
            settingsDataStore.updateEqualizer(preset, bassBoost)
        }
    }

    fun toggleHiResEngine(enabled: Boolean) {
        viewModelScope.launch {
            settingsDataStore.updateHiResEngine(enabled)
        }
    }

    // --- Categories & Filtering ---

    fun selectCategory(category: String) {
        _uiState.value = _uiState.value.copy(selectedCategory = category)
        syncQueueWithFilteredTracks()
    }

    fun setSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        syncQueueWithFilteredTracks()
    }

    fun scanDeviceAudio() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isScanningDevice = true)
            val deviceFiles = repository.queryDeviceAudio()
            if (deviceFiles.isNotEmpty()) {
                val existingIds = _allTracks.value.map { it.id }.toSet()
                val newTracks = deviceFiles.filter { it.id !in existingIds }
                val updated = _allTracks.value + newTracks
                _allTracks.value = updated
                repository.saveTracks(updated)

                if (_uiState.value.currentTrack == null && updated.isNotEmpty()) {
                    val first = updated.first()
                    _uiState.value = _uiState.value.copy(
                        currentTrack = first,
                        durationMs = first.durationMs,
                        currentLyrics = LyricsParser.parse(first.rawLyrics)
                    )
                }

                _uiState.value = _uiState.value.copy(
                    importStatusMessage = "Berhasil menambahkan ${newTracks.size} lagu dari perangkat!"
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    importStatusMessage = "Tidak ditemukan file audio baru di penyimpanan."
                )
            }
            _uiState.value = _uiState.value.copy(isScanningDevice = false)
            syncQueueWithFilteredTracks()
        }
    }

    // --- Modal Navigation State ---

    fun openFullPlayer() {
        _uiState.value = _uiState.value.copy(isFullPlayerOpen = true)
    }

    fun closeFullPlayer() {
        _uiState.value = _uiState.value.copy(isFullPlayerOpen = false)
    }

    fun setShowSleepTimerDialog(show: Boolean) {
        _uiState.value = _uiState.value.copy(showSleepTimerDialog = show)
    }

    fun setShowEqualizerDialog(show: Boolean) {
        _uiState.value = _uiState.value.copy(showEqualizerDialog = show)
    }

    fun setShowSettingsDialog(show: Boolean) {
        _uiState.value = _uiState.value.copy(showSettingsDialog = show)
    }

    fun setShowLyricsFullScreen(show: Boolean) {
        _uiState.value = _uiState.value.copy(showLyricsFullScreen = show)
    }

    // --- Playlist Management ---

    fun selectPlaylist(playlistId: String?) {
        _uiState.value = _uiState.value.copy(selectedPlaylistId = playlistId)
        syncQueueWithFilteredTracks()
    }

    fun createPlaylist(name: String, description: String = "") {
        if (name.isBlank()) return
        val newPl = Playlist(
            id = "pl_${System.currentTimeMillis()}",
            name = name.trim(),
            description = description.trim(),
            trackIds = emptyList()
        )
        val updated = _uiState.value.playlists + newPl
        _uiState.value = _uiState.value.copy(
            playlists = updated,
            showCreatePlaylistDialog = false,
            selectedPlaylistId = newPl.id
        )
        viewModelScope.launch {
            repository.savePlaylists(updated)
        }
        syncQueueWithFilteredTracks()
    }

    fun deletePlaylist(playlistId: String) {
        val updated = _uiState.value.playlists.filter { it.id != playlistId }
        val newSelected = if (_uiState.value.selectedPlaylistId == playlistId) null else _uiState.value.selectedPlaylistId
        _uiState.value = _uiState.value.copy(
            playlists = updated,
            selectedPlaylistId = newSelected
        )
        viewModelScope.launch {
            repository.savePlaylists(updated)
        }
        syncQueueWithFilteredTracks()
    }

    fun addTrackToPlaylist(trackId: String, playlistId: String) {
        val updated = _uiState.value.playlists.map { pl ->
            if (pl.id == playlistId) {
                if (!pl.trackIds.contains(trackId)) {
                    pl.copy(trackIds = pl.trackIds + trackId)
                } else pl
            } else pl
        }
        _uiState.value = _uiState.value.copy(
            playlists = updated,
            trackToAddToPlaylist = null
        )
        viewModelScope.launch {
            repository.savePlaylists(updated)
        }
        syncQueueWithFilteredTracks()
    }

    fun removeTrackFromPlaylist(trackId: String, playlistId: String) {
        val updated = _uiState.value.playlists.map { pl ->
            if (pl.id == playlistId) {
                pl.copy(trackIds = pl.trackIds.filter { it != trackId })
            } else pl
        }
        _uiState.value = _uiState.value.copy(playlists = updated)
        viewModelScope.launch {
            repository.savePlaylists(updated)
        }
        syncQueueWithFilteredTracks()
    }

    fun promptAddToPlaylist(track: Track) {
        _uiState.value = _uiState.value.copy(trackToAddToPlaylist = track)
    }

    fun cancelAddToPlaylist() {
        _uiState.value = _uiState.value.copy(trackToAddToPlaylist = null)
    }

    /**
     * Checks if a track is in the Favorites playlist.
     */
    fun isTrackFavorite(trackId: String): Boolean {
        val favPlaylist = _uiState.value.playlists.find {
            it.id == "pl_favorites" ||
            it.name.contains("Favorit", ignoreCase = true) ||
            it.name.contains("Favorite", ignoreCase = true)
        }
        return favPlaylist?.trackIds?.contains(trackId) == true
    }

    /**
     * Toggles a track in/out of the Favorites playlist.
     * Instant one-tap Love action.
     */
    fun toggleFavoriteTrack(trackId: String) {
        val existingFav = _uiState.value.playlists.find {
            it.id == "pl_favorites" ||
            it.name.contains("Favorit", ignoreCase = true) ||
            it.name.contains("Favorite", ignoreCase = true)
        }

        if (existingFav != null) {
            if (existingFav.trackIds.contains(trackId)) {
                removeTrackFromPlaylist(trackId, existingFav.id)
            } else {
                addTrackToPlaylist(trackId, existingFav.id)
            }
        } else {
            val newFav = Playlist(
                id = "pl_favorites",
                name = "Favorit Saya",
                description = "Kumpulan lagu yang paling disukai",
                trackIds = listOf(trackId)
            )
            val updated = listOf(newFav) + _uiState.value.playlists
            _uiState.value = _uiState.value.copy(playlists = updated)
            viewModelScope.launch {
                repository.savePlaylists(updated)
            }
            syncQueueWithFilteredTracks()
        }
    }

    fun setShowPlaylistModal(show: Boolean) {
        _uiState.value = _uiState.value.copy(showPlaylistModal = show)
    }

    fun setShowCreatePlaylistDialog(show: Boolean) {
        _uiState.value = _uiState.value.copy(showCreatePlaylistDialog = show)
    }

    // --- Playlist & Track Sorting & Management ---

    fun setTrackSortOption(option: TrackSortOption) {
        _uiState.value = _uiState.value.copy(trackSortOption = option)
        if (option == TrackSortOption.SHUFFLE_ORDER) {
            if (!_uiState.value.isShuffleEnabled) {
                toggleShuffle(explicitShuffleState = true)
            } else {
                reshuffle()
            }
        } else {
            syncQueueWithFilteredTracks()
        }
    }

    fun setPlaylistSortOption(option: PlaylistSortOption) {
        _uiState.value = _uiState.value.copy(playlistSortOption = option)
    }

    fun setShowSortDialog(show: Boolean) {
        _uiState.value = _uiState.value.copy(showSortDialog = show)
    }

    fun promptDeletePlaylist(playlist: Playlist) {
        _uiState.value = _uiState.value.copy(playlistToDelete = playlist)
    }

    fun cancelDeletePlaylist() {
        _uiState.value = _uiState.value.copy(playlistToDelete = null)
    }

    fun confirmDeletePlaylist() {
        val pl = _uiState.value.playlistToDelete ?: return
        deletePlaylist(pl.id)
        _uiState.value = _uiState.value.copy(playlistToDelete = null)
    }

    fun getSortedPlaylists(): List<Playlist> {
        val list = _uiState.value.playlists
        return when (_uiState.value.playlistSortOption) {
            PlaylistSortOption.NAME_ASC -> list.sortedBy { it.name.lowercase() }
            PlaylistSortOption.NAME_DESC -> list.sortedByDescending { it.name.lowercase() }
            PlaylistSortOption.TRACKS_COUNT_DESC -> list.sortedByDescending { it.trackIds.size }
            PlaylistSortOption.DATE_NEWEST -> list.sortedByDescending { it.createdAtEpoch }
            PlaylistSortOption.DATE_OLDEST -> list.sortedBy { it.createdAtEpoch }
        }
    }

    // --- Import Audio & Lyrics Files ---
    fun importAudioFiles(uris: List<android.net.Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            val batchResult = repository.importAudioAndLyricsBatch(uris)
            val newlyImported = batchResult.importedTracks
            val orphanLyrics = batchResult.orphanLyricsMap

            var currentTracksList = _allTracks.value

            // If orphan lyrics were picked (e.g. user selected .lrc files for songs already in library)
            if (orphanLyrics.isNotEmpty()) {
                currentTracksList = currentTracksList.map { track ->
                    val titleNorm = track.title.trim().lowercase()
                    val matchedLrc = orphanLyrics[titleNorm]
                        ?: orphanLyrics.entries.firstOrNull { titleNorm.contains(it.key) || it.key.contains(titleNorm) }?.value
                    if (matchedLrc != null && track.rawLyrics.isNullOrBlank()) {
                        track.copy(rawLyrics = matchedLrc)
                    } else track
                }
            }

            if (newlyImported.isNotEmpty() || orphanLyrics.isNotEmpty()) {
                val updated = (currentTracksList + newlyImported).distinctBy { it.id }
                _allTracks.value = updated
                repository.saveTracks(updated)

                // If a playlist is selected, add imported songs directly to it
                val currentPlId = _uiState.value.selectedPlaylistId
                if (currentPlId != null && newlyImported.isNotEmpty()) {
                    val updatedPlaylists = _uiState.value.playlists.map { pl ->
                        if (pl.id == currentPlId) {
                            pl.copy(trackIds = (pl.trackIds + newlyImported.map { it.id }).distinct())
                        } else pl
                    }
                    _uiState.value = _uiState.value.copy(playlists = updatedPlaylists)
                    repository.savePlaylists(updatedPlaylists)
                }

                syncQueueWithFilteredTracks()

                // Update current track lyrics if active song was matched
                val activeTrack = _uiState.value.currentTrack
                if (activeTrack != null) {
                    val updatedActive = updated.find { it.id == activeTrack.id }
                    if (updatedActive != null && updatedActive.rawLyrics != activeTrack.rawLyrics) {
                        _uiState.value = _uiState.value.copy(
                            currentTrack = updatedActive,
                            currentLyrics = LyricsParser.parse(updatedActive.rawLyrics)
                        )
                    }
                } else if (updated.isNotEmpty()) {
                    val first = updated.first()
                    _uiState.value = _uiState.value.copy(
                        currentTrack = first,
                        durationMs = first.durationMs,
                        currentLyrics = LyricsParser.parse(first.rawLyrics)
                    )
                }

                val msg = buildString {
                    if (newlyImported.isNotEmpty()) {
                        append("Berhasil mengimpor ${newlyImported.size} lagu")
                    }
                    val matchedCount = newlyImported.count { !it.rawLyrics.isNullOrBlank() }
                    if (matchedCount > 0) {
                        append(" dengan $matchedCount lirik otomatis")
                    }
                    if (orphanLyrics.isNotEmpty()) {
                        append(". ${orphanLyrics.size} file lirik terhubung!")
                    }
                }

                _uiState.value = _uiState.value.copy(
                    importStatusMessage = msg.ifEmpty { "Impor selesai!" }
                )
            }
        }
    }

    fun clearImportStatusMessage() {
        _uiState.value = _uiState.value.copy(importStatusMessage = null)
    }

    // --- Lyrics Management (Manual Import & Paste) ---

    fun promptImportLyrics(track: Track) {
        _uiState.value = _uiState.value.copy(trackToImportLyrics = track)
    }

    fun cancelImportLyrics() {
        _uiState.value = _uiState.value.copy(trackToImportLyrics = null)
    }

    fun setShowPasteLyricsDialog(show: Boolean) {
        _uiState.value = _uiState.value.copy(showPasteLyricsDialog = show)
    }

    fun importLyricsFromUri(trackId: String, uri: android.net.Uri) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val content = getApplication<Application>().contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } ?: ""
                if (content.isNotBlank()) {
                    updateTrackLyrics(trackId, content)
                    _uiState.value = _uiState.value.copy(
                        importStatusMessage = "Berhasil memuat lirik untuk lagu!",
                        trackToImportLyrics = null
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        importStatusMessage = "File lirik kosong atau tidak terbaca."
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    importStatusMessage = "Gagal membaca file lirik: ${e.localizedMessage}"
                )
            }
        }
    }

    fun updateTrackLyrics(trackId: String, rawLyrics: String) {
        val updatedTracks = _allTracks.value.map { track ->
            if (track.id == trackId) {
                track.copy(rawLyrics = rawLyrics)
            } else track
        }
        _allTracks.value = updatedTracks

        val current = _uiState.value.currentTrack
        if (current?.id == trackId) {
            val parsed = LyricsParser.parse(rawLyrics)
            _uiState.value = _uiState.value.copy(
                currentTrack = current.copy(rawLyrics = rawLyrics),
                currentLyrics = parsed,
                activeLyricIndex = LyricsParser.findActiveIndex(parsed, _uiState.value.currentPositionMs)
            )
        }

        viewModelScope.launch {
            repository.saveTracks(updatedTracks)
        }
    }

    // --- Delete / Remove Track ---
    fun promptDeleteTrack(track: Track) {
        _uiState.value = _uiState.value.copy(trackToDelete = track)
    }

    fun cancelDeleteTrack() {
        _uiState.value = _uiState.value.copy(trackToDelete = null)
    }

    fun confirmDeleteTrack() {
        val track = _uiState.value.trackToDelete ?: return
        val currentTrack = _uiState.value.currentTrack
        val isDeletingCurrent = currentTrack?.id == track.id

        val updatedList = _allTracks.value.filter { it.id != track.id }
        _allTracks.value = updatedList

        // Remove track ID from all playlists
        val updatedPlaylists = _uiState.value.playlists.map { pl ->
            pl.copy(trackIds = pl.trackIds.filter { it != track.id })
        }

        _uiState.value = _uiState.value.copy(
            trackToDelete = null,
            playlists = updatedPlaylists
        )

        viewModelScope.launch {
            repository.saveTracks(updatedList)
            repository.savePlaylists(updatedPlaylists)
        }

        if (isDeletingCurrent) {
            val remainingFiltered = getFilteredTracks()
            if (remainingFiltered.isNotEmpty()) {
                playNext()
            } else {
                audioEngine.stop()
                _uiState.value = _uiState.value.copy(
                    currentTrack = null,
                    isPlaying = false,
                    currentPositionMs = 0L,
                    durationMs = 0L,
                    currentLyrics = emptyList()
                )
            }
        }
        syncQueueWithFilteredTracks()
    }

    // --- Loudness Normalization / Automatic Volume Leveling ---
    fun toggleLoudnessNormalization(enabled: Boolean) {
        audioEngine.setLoudnessNormalization(enabled, userSettings.value.targetLoudnessLufs)
        _uiState.value = _uiState.value.copy(
            loudnessNormalizationActive = enabled,
            appliedNormalizationGainDb = audioEngine.appliedNormalizationGainDb
        )
        viewModelScope.launch {
            settingsDataStore.updateLoudnessNormalization(enabled, userSettings.value.targetLoudnessLufs)
        }
    }

    fun setTargetLoudnessLufs(targetLufs: Float) {
        audioEngine.setLoudnessNormalization(_uiState.value.loudnessNormalizationActive, targetLufs)
        viewModelScope.launch {
            settingsDataStore.updateLoudnessNormalization(_uiState.value.loudnessNormalizationActive, targetLufs)
        }
    }

    override fun onCleared() {
        super.onCleared()
        audioEngine.stop()
        playbackProgressJob?.cancel()
        sleepTimerJob?.cancel()
        MusicPlaybackCoordinator.stopService(getApplication())
    }
}
