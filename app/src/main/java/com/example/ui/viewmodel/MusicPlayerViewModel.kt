package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.AudioPlayerEngine
import com.example.audio.OrderedShuffleQueue
import com.example.data.model.LyricLine
import com.example.data.model.LyricsParser
import com.example.data.model.Track
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
    val repeatMode: PlaybackRepeatMode = PlaybackRepeatMode.ALL,
    val shuffleOrderMap: Map<String, Int> = emptyMap(),
    val currentQueuePosition: Int = 1,
    val totalQueueCount: Int = 0,
    val currentLyrics: List<LyricLine> = emptyList(),
    val activeLyricIndex: Int = -1,
    val isSleepTimerActive: Boolean = false,
    val sleepTimerRemainingSeconds: Long = 0L,
    val currentDecibels: Float = -12.0f,
    val effectiveVolumePercent: Int = 75,
    val selectedCategory: String = "Semua",
    val searchQuery: String = "",
    val sortQueueByShuffleOrder: Boolean = false,
    val isScanningDevice: Boolean = false,
    val isFullPlayerOpen: Boolean = false,
    val showSleepTimerDialog: Boolean = false,
    val showEqualizerDialog: Boolean = false,
    val showSettingsDialog: Boolean = false,
    val showLyricsFullScreen: Boolean = false
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
        loadInitialTracks()
        observeSettings()
        startProgressTracker()
    }

    private fun loadInitialTracks() {
        val samples = repository.getSampleTracks()
        _allTracks.value = samples
        syncQueueWithFilteredTracks()

        // Set initial track
        if (samples.isNotEmpty()) {
            val initialTrack = samples.first()
            _uiState.value = _uiState.value.copy(
                currentTrack = initialTrack,
                durationMs = initialTrack.durationMs,
                currentLyrics = LyricsParser.parse(initialTrack.rawLyrics),
                totalQueueCount = samples.size
            )
        }
    }

    private fun observeSettings() {
        viewModelScope.launch {
            userSettings.collect { settings ->
                audioEngine.setHearingSafety(
                    enabled = settings.hearingProtectionEnabled,
                    maxSafeCapPercent = settings.maxSafeVolumePercent
                )
                if (settings.isShuffleEnabled != _uiState.value.isShuffleEnabled) {
                    toggleShuffle(explicitShuffleState = settings.isShuffleEnabled)
                }
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
                        effectiveVolumePercent = (audioEngine.effectiveVolume * 100).toInt()
                    )
                }
                delay(100)
            }
        }
    }

    private fun syncQueueWithFilteredTracks() {
        val tracks = getFilteredTracks()
        val currentId = _uiState.value.currentTrack?.id
        shuffleQueue.setupQueue(tracks, _uiState.value.isShuffleEnabled, currentId)

        _uiState.value = _uiState.value.copy(
            shuffleOrderMap = shuffleQueue.orderIndexMap,
            totalQueueCount = tracks.size,
            currentQueuePosition = if (currentId != null) shuffleQueue.getOrderBadge(currentId) else 1
        )
    }

    fun getFilteredTracks(): List<Track> {
        val category = _uiState.value.selectedCategory
        val query = _uiState.value.searchQuery.trim().lowercase()

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

        if (query.isNotEmpty()) {
            filtered = filtered.filter {
                it.title.lowercase().contains(query) ||
                        it.artist.lowercase().contains(query) ||
                        it.album.lowercase().contains(query)
            }
        }

        if (_uiState.value.sortQueueByShuffleOrder && _uiState.value.isShuffleEnabled) {
            filtered = filtered.sortedBy { shuffleQueue.getOrderBadge(it.id) }
        }

        return filtered
    }

    // --- Audio Playback Controls ---

    fun playTrack(track: Track) {
        val isSameTrack = _uiState.value.currentTrack?.id == track.id
        if (isSameTrack && _uiState.value.isPlaying) {
            pause()
            return
        }

        waitingForTrackToEndToSleep = false
        audioEngine.playTrack(track)
        shuffleQueue.moveToTrack(track.id)

        _uiState.value = _uiState.value.copy(
            currentTrack = track,
            isPlaying = true,
            currentPositionMs = 0L,
            durationMs = track.durationMs,
            currentLyrics = LyricsParser.parse(track.rawLyrics),
            activeLyricIndex = 0,
            currentQueuePosition = shuffleQueue.getOrderBadge(track.id)
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
    }

    fun resume() {
        if (_uiState.value.currentTrack == null && _allTracks.value.isNotEmpty()) {
            playTrack(_allTracks.value.first())
            return
        }
        audioEngine.resume()
        _uiState.value = _uiState.value.copy(isPlaying = true)
    }

    fun seekTo(positionMs: Long) {
        audioEngine.seekTo(positionMs)
        _uiState.value = _uiState.value.copy(currentPositionMs = positionMs)
    }

    fun playNext() {
        val tracks = getFilteredTracks()
        val nextId = shuffleQueue.moveToNext(repeatAll = _uiState.value.repeatMode != PlaybackRepeatMode.OFF)
        val nextTrack = tracks.find { it.id == nextId } ?: _allTracks.value.find { it.id == nextId }

        if (nextTrack != null) {
            playTrack(nextTrack)
        }
    }

    fun playPrevious() {
        // If current position > 3 seconds, restart current track
        if (_uiState.value.currentPositionMs > 3000L) {
            seekTo(0L)
            return
        }

        val tracks = getFilteredTracks()
        val prevId = shuffleQueue.moveToPrevious()
        val prevTrack = tracks.find { it.id == prevId } ?: _allTracks.value.find { it.id == prevId }

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
        val tracks = getFilteredTracks()

        shuffleQueue.setupQueue(tracks, newShuffle, currentTrackId)

        _uiState.value = _uiState.value.copy(
            isShuffleEnabled = newShuffle,
            shuffleOrderMap = shuffleQueue.orderIndexMap,
            currentQueuePosition = if (currentTrackId != null) shuffleQueue.getOrderBadge(currentTrackId) else 1
        )

        viewModelScope.launch {
            settingsDataStore.updateShuffleState(newShuffle, _uiState.value.sortQueueByShuffleOrder)
        }
    }

    /**
     * Reshuffles the active tracks and visibly re-indexes order badges #1..#N.
     */
    fun reshuffle() {
        val tracks = getFilteredTracks()
        val currentTrackId = _uiState.value.currentTrack?.id
        shuffleQueue.reshuffle(tracks, currentTrackId)

        _uiState.value = _uiState.value.copy(
            isShuffleEnabled = true,
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
        fadeDurationMin: Int
    ) {
        sleepTimerJob?.cancel()
        waitingForTrackToEndToSleep = false
        audioEngine.setFadeMultiplier(1.0f)

        val totalSeconds = minutes.toLong() * 60L
        val fadeSeconds = (fadeDurationMin * 60L).coerceAtMost(totalSeconds)

        _uiState.value = _uiState.value.copy(
            isSleepTimerActive = true,
            sleepTimerRemainingSeconds = totalSeconds
        )

        viewModelScope.launch {
            settingsDataStore.updateSleepSettings(
                minutes = minutes,
                finishSong = finishCurrentSong,
                volumeFade = fadeVolume,
                fadeDurationMin = fadeDurationMin
            )
        }

        sleepTimerJob = viewModelScope.launch {
            var remaining = totalSeconds
            while (remaining > 0 && isActive) {
                delay(1000)
                remaining--
                _uiState.value = _uiState.value.copy(sleepTimerRemainingSeconds = remaining)

                // Gradual decibel/volume fade-out during the final minutes
                if (fadeVolume && remaining <= fadeSeconds && fadeSeconds > 0) {
                    val fadeFactor = (remaining.toFloat() / fadeSeconds.toFloat()).coerceIn(0.05f, 1.0f)
                    audioEngine.setFadeMultiplier(fadeFactor)
                }
            }

            // Timer reached 0
            if (finishCurrentSong && _uiState.value.isPlaying) {
                // Wait for the currently playing song to conclude before sleeping
                waitingForTrackToEndToSleep = true
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
            sleepTimerRemainingSeconds = 0L
        )
    }

    private fun stopAndSleep() {
        pause()
        audioEngine.setFadeMultiplier(1.0f)
        _uiState.value = _uiState.value.copy(
            isSleepTimerActive = false,
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
            val currentSamples = repository.getSampleTracks()
            _allTracks.value = currentSamples + deviceFiles
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

    override fun onCleared() {
        super.onCleared()
        audioEngine.stop()
        playbackProgressJob?.cancel()
        sleepTimerJob?.cancel()
    }
}
