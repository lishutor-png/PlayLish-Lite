package com.example.data.model

data class UserSettings(
    val sleepTimerMinutes: Int = 30,
    val sleepModeFinishSong: Boolean = false,       // false: langsung mati; true: tunggu lagu selesai
    val sleepVolumeFade: Boolean = true,           // true: bertahap (fade-out); false: mendadak
    val sleepFadeDurationMinutes: Int = 2,
    val hearingProtectionEnabled: Boolean = true,  // Batas aman telinga
    val maxSafeVolumePercent: Int = 75,            // 75% max cap
    val targetSafeDb: Float = -12.0f,              // target output in dB
    val equalizerPreset: String = "Sport Dynamic",
    val bassBoostLevel: Int = 350,                 // 0 to 1000
    val hiResEngineEnabled: Boolean = true,
    val isShuffleEnabled: Boolean = false,
    val sortQueueByShuffleOrder: Boolean = false
)
