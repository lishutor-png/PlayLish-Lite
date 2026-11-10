package com.example.audio

import com.example.data.model.Track
import kotlin.random.Random

class OrderedShuffleQueue {

    // Maps trackId -> 1-based order position in current playback sequence
    private val _orderIndexMap = mutableMapOf<String, Int>()
    val orderIndexMap: Map<String, Int> get() = _orderIndexMap

    // Ordered list of trackIds to be played
    private val _playbackSequence = mutableListOf<String>()
    val playbackSequence: List<String> get() = _playbackSequence

    var isShuffleActive: Boolean = false
        private set

    var currentSequencePosition: Int = 0 // 0-based index into _playbackSequence
        private set

    /**
     * Initializes or re-synchronizes the queue with the provided playlist.
     * If shuffle is off, order matches original playlist index (1, 2, 3, ...).
     * If shuffle is on, order is randomized with guaranteed variation at position #1.
     */
    fun setupQueue(tracks: List<Track>, shuffle: Boolean, keepCurrentTrackId: String? = null) {
        val previousFirstId = _playbackSequence.firstOrNull() ?: tracks.firstOrNull()?.id
        isShuffleActive = shuffle
        _orderIndexMap.clear()
        _playbackSequence.clear()

        if (tracks.isEmpty()) {
            currentSequencePosition = 0
            return
        }

        if (!shuffle) {
            // Natural order
            tracks.forEachIndexed { index, track ->
                _orderIndexMap[track.id] = index + 1
                _playbackSequence.add(track.id)
            }
            currentSequencePosition = if (keepCurrentTrackId != null) {
                _playbackSequence.indexOf(keepCurrentTrackId).coerceAtLeast(0)
            } else 0
        } else {
            // Ordered Shuffle
            reshuffle(tracks, keepCurrentTrackId, forceDifferentFromTrackId = previousFirstId)
        }
    }

    /**
     * Re-shuffles all tracks completely and assigns new 1-based sequence numbers (#1, #2, #3, ...).
     * All tracks (including track #1 and any currently playing track) are randomized fairly.
     * Position #1 is guaranteed to change when there are >= 2 tracks.
     */
    fun reshuffle(
        tracks: List<Track>,
        currentTrackId: String? = null,
        forceDifferentFromTrackId: String? = null
    ) {
        if (tracks.isEmpty()) return

        isShuffleActive = true

        // Capture previous #1 track to ensure position #1 actually changes
        val previousFirstId = forceDifferentFromTrackId ?: _playbackSequence.firstOrNull()

        _orderIndexMap.clear()
        _playbackSequence.clear()

        val trackIds = tracks.map { it.id }.toMutableList()

        if (trackIds.size > 1) {
            val rng = Random(System.nanoTime())
            trackIds.shuffle(rng)

            // If by chance the track at index 0 is still the same as the previous #1 track,
            // swap it with a random position from 1 until size - 1 so #1 always changes!
            if (previousFirstId != null && trackIds.first() == previousFirstId) {
                val swapIndex = rng.nextInt(1, trackIds.size)
                val temp = trackIds[0]
                trackIds[0] = trackIds[swapIndex]
                trackIds[swapIndex] = temp
            }
        }

        _playbackSequence.addAll(trackIds)

        // Keep current playing track's position in sync with its new spot in the queue
        currentSequencePosition = if (currentTrackId != null) {
            val idx = _playbackSequence.indexOf(currentTrackId)
            if (idx != -1) idx else 0
        } else {
            0
        }

        // Assign 1-based order badge to every song
        _playbackSequence.forEachIndexed { index, trackId ->
            _orderIndexMap[trackId] = index + 1
        }
    }

    fun getOrderBadge(trackId: String): Int {
        return _orderIndexMap[trackId] ?: 1
    }

    fun getCurrentTrackId(): String? {
        if (_playbackSequence.isEmpty() || currentSequencePosition !in _playbackSequence.indices) {
            return null
        }
        return _playbackSequence[currentSequencePosition]
    }

    fun getNextTrackId(repeatAll: Boolean = true): String? {
        if (_playbackSequence.isEmpty()) return null
        val nextPos = currentSequencePosition + 1
        return if (nextPos < _playbackSequence.size) {
            _playbackSequence[nextPos]
        } else if (repeatAll) {
            _playbackSequence.firstOrNull()
        } else null
    }

    fun getPreviousTrackId(): String? {
        if (_playbackSequence.isEmpty()) return null
        val prevPos = currentSequencePosition - 1
        return if (prevPos >= 0) {
            _playbackSequence[prevPos]
        } else {
            _playbackSequence.lastOrNull()
        }
    }

    fun moveToNext(repeatAll: Boolean = true): String? {
        val nextId = getNextTrackId(repeatAll)
        if (nextId != null) {
            currentSequencePosition = _playbackSequence.indexOf(nextId)
        }
        return nextId
    }

    fun moveToPrevious(): String? {
        val prevId = getPreviousTrackId()
        if (prevId != null) {
            currentSequencePosition = _playbackSequence.indexOf(prevId)
        }
        return prevId
    }

    fun moveToTrack(trackId: String) {
        val idx = _playbackSequence.indexOf(trackId)
        if (idx != -1) {
            currentSequencePosition = idx
        }
    }
}
