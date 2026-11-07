package com.example.data.model

data class UserSettings(
    val sleepTimerMinutes: Int = 30,
    val sleepModeFinishSong: Boolean = false,       // false: langsung mati; true: tunggu lagu selesai
    val sleepVolumeFade: Boolean = true,           // true: bertahap (fade-out); false: mendadak
    val sleepFadeDurationSeconds: Int = 30,        // Fade-out melembutkan suara 30 detik sebelum mati
    val hearingProtectionEnabled: Boolean = true,  // Batas aman telinga
    val maxSafeVolumePercent: Int = 75,            // 75% max cap
    val targetSafeDb: Float = -12.0f,              // target output in dB
    val equalizerPreset: String = "Sport Dynamic",
    val bassBoostLevel: Int = 350,                 // 0 to 1000
    val hiResEngineEnabled: Boolean = true,
    val isShuffleEnabled: Boolean = false,
    val sortQueueByShuffleOrder: Boolean = false,
    val loudnessNormalizationEnabled: Boolean = true,  // Automatic Volume Leveling / ReplayGain
    val targetLoudnessLufs: Float = -14.0f             // Standard EBU R128 target (-14 LUFS)
)
