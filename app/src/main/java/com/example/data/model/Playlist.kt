package com.example.data.model

data class Playlist(
    val id: String,
    val name: String,
    val description: String = "",
    val trackIds: List<String> = emptyList(),
    val iconName: String = "playlist",
    val createdAtEpoch: Long = System.currentTimeMillis()
)
