package com.example.data.model

enum class TrackSortOption(val displayName: String) {
    DEFAULT("Bawaan"),
    TITLE_ASC("Judul A-Z"),
    TITLE_DESC("Judul Z-A"),
    ARTIST_ASC("Artis A-Z"),
    DURATION_DESC("Durasi Terlama"),
    DURATION_ASC("Durasi Tercepat"),
    SHUFFLE_ORDER("Urutan Acak #")
}

enum class PlaylistSortOption(val displayName: String) {
    NAME_ASC("Nama A-Z"),
    NAME_DESC("Nama Z-A"),
    TRACKS_COUNT_DESC("Lagu Terbanyak"),
    DATE_NEWEST("Terbaru Dibuat"),
    DATE_OLDEST("Terlama Dibuat")
}
