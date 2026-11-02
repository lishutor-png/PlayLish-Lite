package com.example.data.model

import java.util.regex.Pattern

data class LyricLine(
    val timestampMs: Long,
    val text: String
)

object LyricsParser {
    private val LRC_PATTERN = Pattern.compile("\\[(\\d{2}):(\\d{2})(?:\\.(\\d{1,3}))?\\](.*)")

    fun parse(rawLrc: String?): List<LyricLine> {
        if (rawLrc.isNullOrBlank()) return emptyList()

        val lines = rawLrc.lines()
        val parsedList = mutableListOf<LyricLine>()

        var hasTimestamps = false

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isEmpty()) continue

            val matcher = LRC_PATTERN.matcher(trimmed)
            if (matcher.matches()) {
                hasTimestamps = true
                val minutes = matcher.group(1)?.toLongOrNull() ?: 0L
                val seconds = matcher.group(2)?.toLongOrNull() ?: 0L
                val millisPart = matcher.group(3)
                val millis = when {
                    millisPart == null -> 0L
                    millisPart.length == 1 -> millisPart.toLong() * 100
                    millisPart.length == 2 -> millisPart.toLong() * 10
                    else -> millisPart.take(3).toLong()
                }

                val totalMs = (minutes * 60 + seconds) * 1000 + millis
                val lyricText = matcher.group(4)?.trim() ?: ""
                parsedList.add(LyricLine(totalMs, lyricText))
            }
        }

        // If no [mm:ss.xx] timestamps were detected, treat as plain text with estimated spaced intervals
        if (!hasTimestamps) {
            var estimatedMs = 3000L
            for (line in lines) {
                val trimmed = line.trim()
                if (trimmed.isNotEmpty()) {
                    parsedList.add(LyricLine(estimatedMs, trimmed))
                    estimatedMs += 5000L
                }
            }
        }

        return parsedList.sortedBy { it.timestampMs }
    }

    fun findActiveIndex(lyrics: List<LyricLine>, currentMs: Long): Int {
        if (lyrics.isEmpty()) return -1
        var activeIndex = -1
        for (i in lyrics.indices) {
            if (lyrics[i].timestampMs <= currentMs) {
                activeIndex = i
            } else {
                break
            }
        }
        return activeIndex
    }
}
