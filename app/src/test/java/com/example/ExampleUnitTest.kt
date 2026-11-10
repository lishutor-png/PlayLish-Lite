package com.example

import com.example.audio.OrderedShuffleQueue
import com.example.data.model.LyricsParser
import com.example.data.model.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun `ordered shuffle assigns sequential order badges`() {
        val queue = OrderedShuffleQueue()
        val dummyTracks = listOf(
            Track(id = "1", title = "Track 1", artist = "Artist", album = "Album", durationMs = 100000L),
            Track(id = "2", title = "Track 2", artist = "Artist", album = "Album", durationMs = 100000L),
            Track(id = "3", title = "Track 3", artist = "Artist", album = "Album", durationMs = 100000L)
        )

        // Enable shuffle
        queue.setupQueue(dummyTracks, shuffle = true)
        assertTrue(queue.isShuffleActive)

        // Every track must have a badge between 1 and 3
        val badges = dummyTracks.map { queue.getOrderBadge(it.id) }.sorted()
        assertEquals(listOf(1, 2, 3), badges)
    }

    @Test
    fun `lyrics parser extracts timestamps and finds active index`() {
        val sampleLrc = """
            [00:05.00]Line 1
            [00:15.50]Line 2
            [00:30.00]Line 3
        """.trimIndent()

        val parsed = LyricsParser.parse(sampleLrc)
        assertEquals(3, parsed.size)
        assertEquals(5000L, parsed[0].timestampMs)
        assertEquals("Line 1", parsed[0].text)

        val activeIndex = LyricsParser.findActiveIndex(parsed, 16000L)
        assertEquals(1, activeIndex)
    }

    @Test
    fun `playlist sorting orders correctly by name and track count`() {
        val p1 = com.example.data.model.Playlist(id = "1", name = "Zebra", trackIds = listOf("t1", "t2"))
        val p2 = com.example.data.model.Playlist(id = "2", name = "Alpha", trackIds = listOf("t1", "t2", "t3"))
        val p3 = com.example.data.model.Playlist(id = "3", name = "Beta", trackIds = listOf("t1"))

        val list = listOf(p1, p2, p3)

        val sortedByNameAsc = list.sortedBy { it.name.lowercase() }
        assertEquals(listOf("Alpha", "Beta", "Zebra"), sortedByNameAsc.map { it.name })

        val sortedByCountDesc = list.sortedByDescending { it.trackIds.size }
        assertEquals(listOf(3, 2, 1), sortedByCountDesc.map { it.trackIds.size })
    }

    @Test
    fun `track sorting orders correctly by title and duration`() {
        val t1 = Track(id = "1", title = "Charlie", artist = "Artist", album = "Album", durationMs = 300000L)
        val t2 = Track(id = "2", title = "Alpha", artist = "Artist", album = "Album", durationMs = 120000L)
        val t3 = Track(id = "3", title = "Bravo", artist = "Artist", album = "Album", durationMs = 240000L)

        val list = listOf(t1, t2, t3)

        val sortedByTitle = list.sortedBy { it.title.lowercase() }
        assertEquals(listOf("Alpha", "Bravo", "Charlie"), sortedByTitle.map { it.title })

        val sortedByDurationDesc = list.sortedByDescending { it.durationMs }
        assertEquals(listOf(300000L, 240000L, 120000L), sortedByDurationDesc.map { it.durationMs })
    }

    @Test
    fun `natural queue maintains strict sequential order when shuffle is false`() {
        val queue = OrderedShuffleQueue()
        val dummyTracks = listOf(
            Track(id = "track_a", title = "Song A", artist = "Artist", album = "Album", durationMs = 100000L),
            Track(id = "track_b", title = "Song B", artist = "Artist", album = "Album", durationMs = 100000L),
            Track(id = "track_c", title = "Song C", artist = "Artist", album = "Album", durationMs = 100000L)
        )

        // Setup queue with shuffle = false
        queue.setupQueue(dummyTracks, shuffle = false)
        assertEquals(false, queue.isShuffleActive)

        // First track is 1, second is 2, third is 3
        assertEquals(1, queue.getOrderBadge("track_a"))
        assertEquals(2, queue.getOrderBadge("track_b"))
        assertEquals(3, queue.getOrderBadge("track_c"))

        // Sequential advancement
        assertEquals("track_b", queue.moveToNext(repeatAll = true))
        assertEquals("track_c", queue.moveToNext(repeatAll = true))
        assertEquals("track_a", queue.moveToNext(repeatAll = true)) // loops to first
    }

    @Test
    fun `reshuffle assigns order badges to all tracks and varies position 1`() {
        val queue = OrderedShuffleQueue()
        val dummyTracks = (1..10).map {
            Track(id = "track_$it", title = "Song $it", artist = "Artist", album = "Album", durationMs = 100000L)
        }

        // Setup with shuffle enabled, passing track_1 as initial
        queue.setupQueue(dummyTracks, shuffle = true, keepCurrentTrackId = "track_1")
        assertEquals(true, queue.isShuffleActive)

        // All 10 badges 1..10 must be uniquely assigned
        val allBadges = dummyTracks.map { queue.getOrderBadge(it.id) }.sorted()
        assertEquals((1..10).toList(), allBadges)

        // Record the first track in the sequence
        val firstTrackBefore = queue.playbackSequence.first()

        // Reshuffle with forceDifferentFromTrackId
        queue.reshuffle(dummyTracks, currentTrackId = "track_1", forceDifferentFromTrackId = firstTrackBefore)
        val firstTrackAfter = queue.playbackSequence.first()

        // Position #1 must have changed to a different track
        org.junit.Assert.assertNotEquals(firstTrackBefore, firstTrackAfter)

        // All 10 badges must still be uniquely assigned
        val allBadgesAfter = dummyTracks.map { queue.getOrderBadge(it.id) }.sorted()
        assertEquals((1..10).toList(), allBadgesAfter)
    }

    @Test
    fun `per-tab queues operate independently without cross contamination`() {
        val allTracks = (1..6).map {
            Track(id = "track_$it", title = "All Song $it", artist = "Artist", album = "Album", durationMs = 100000L)
        }
        val favoriteTracks = listOf(allTracks[0], allTracks[2]) // track_1 and track_3

        val allQueue = OrderedShuffleQueue()
        val favQueue = OrderedShuffleQueue()

        // 1. All Tracks in natural order (shuffle off)
        allQueue.setupQueue(allTracks, shuffle = false)
        assertEquals(false, allQueue.isShuffleActive)
        assertEquals(1, allQueue.getOrderBadge("track_1"))
        assertEquals(2, allQueue.getOrderBadge("track_2"))

        // 2. Favorites has shuffle enabled independently
        favQueue.setupQueue(favoriteTracks, shuffle = true)
        assertEquals(true, favQueue.isShuffleActive)
        assertEquals(2, favQueue.playbackSequence.size)
        // All tracks queue remains unmodified and shuffle off
        assertEquals(false, allQueue.isShuffleActive)
        assertEquals(6, allQueue.playbackSequence.size)

        // 3. Advancing favorites queue only cycles favorites
        val nextInFav = favQueue.moveToNext(repeatAll = true)
        assertTrue(nextInFav == "track_1" || nextInFav == "track_3")

        // 4. All tracks queue still sequential
        assertEquals("track_2", allQueue.moveToNext(repeatAll = true))
    }

    @Test
    fun `loudness normalization formula produces correct safe gain range`() {
        val targetLufs = -14.0f
        val trackGainDb = -2.0f
        val lufsDelta = (-14.0f) - targetLufs
        val totalGainDb = (trackGainDb + lufsDelta).coerceIn(-9.0f, 6.0f)
        val multiplier = Math.pow(10.0, totalGainDb / 20.0).toFloat().coerceIn(0.4f, 1.4f)

        // -2.0 dB gain -> multiplier should be ~0.794
        assertTrue(multiplier in 0.78f..0.82f)
    }
}

