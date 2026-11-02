package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.audio.OrderedShuffleQueue
import com.example.data.model.LyricsParser
import com.example.data.model.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("PlayLish Lite", appName)
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
}
