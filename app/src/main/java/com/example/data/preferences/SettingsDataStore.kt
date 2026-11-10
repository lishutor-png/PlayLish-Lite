package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.UserSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "playlish_settings")

class SettingsDataStore(private val context: Context) {

    private object PreferencesKeys {
        val SLEEP_TIMER_MINUTES = intPreferencesKey("sleep_timer_minutes")
        val SLEEP_FINISH_SONG = booleanPreferencesKey("sleep_finish_song")
        val SLEEP_VOLUME_FADE = booleanPreferencesKey("sleep_volume_fade")
        val SLEEP_FADE_DURATION = intPreferencesKey("sleep_fade_duration")
        val HEARING_PROTECTION = booleanPreferencesKey("hearing_protection")
        val MAX_SAFE_VOLUME = intPreferencesKey("max_safe_volume")
        val TARGET_SAFE_DB = floatPreferencesKey("target_safe_db")
        val EQUALIZER_PRESET = stringPreferencesKey("equalizer_preset")
        val BASS_BOOST = intPreferencesKey("bass_boost")
        val HI_RES_ENGINE = booleanPreferencesKey("hi_res_engine")
        val IS_SHUFFLE = booleanPreferencesKey("is_shuffle")
        val SORT_BY_SHUFFLE = booleanPreferencesKey("sort_by_shuffle")
        val TAB_SHUFFLE_STATES = stringPreferencesKey("tab_shuffle_states")
        val LOUDNESS_NORMALIZATION = booleanPreferencesKey("loudness_normalization")
        val TARGET_LOUDNESS_LUFS = floatPreferencesKey("target_loudness_lufs")
    }

    val userSettingsFlow: Flow<UserSettings> = context.dataStore.data.map { preferences ->
        UserSettings(
            sleepTimerMinutes = preferences[PreferencesKeys.SLEEP_TIMER_MINUTES] ?: 30,
            sleepModeFinishSong = preferences[PreferencesKeys.SLEEP_FINISH_SONG] ?: false,
            sleepVolumeFade = preferences[PreferencesKeys.SLEEP_VOLUME_FADE] ?: true,
            sleepFadeDurationSeconds = preferences[PreferencesKeys.SLEEP_FADE_DURATION] ?: 30,
            hearingProtectionEnabled = preferences[PreferencesKeys.HEARING_PROTECTION] ?: true,
            maxSafeVolumePercent = preferences[PreferencesKeys.MAX_SAFE_VOLUME] ?: 75,
            targetSafeDb = preferences[PreferencesKeys.TARGET_SAFE_DB] ?: -12.0f,
            equalizerPreset = preferences[PreferencesKeys.EQUALIZER_PRESET] ?: "Sport Dynamic",
            bassBoostLevel = preferences[PreferencesKeys.BASS_BOOST] ?: 350,
            hiResEngineEnabled = preferences[PreferencesKeys.HI_RES_ENGINE] ?: true,
            isShuffleEnabled = preferences[PreferencesKeys.IS_SHUFFLE] ?: false,
            sortQueueByShuffleOrder = preferences[PreferencesKeys.SORT_BY_SHUFFLE] ?: false,
            loudnessNormalizationEnabled = preferences[PreferencesKeys.LOUDNESS_NORMALIZATION] ?: true,
            targetLoudnessLufs = preferences[PreferencesKeys.TARGET_LOUDNESS_LUFS] ?: -14.0f
        )
    }

    suspend fun updateSettings(settings: UserSettings) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SLEEP_TIMER_MINUTES] = settings.sleepTimerMinutes
            preferences[PreferencesKeys.SLEEP_FINISH_SONG] = settings.sleepModeFinishSong
            preferences[PreferencesKeys.SLEEP_VOLUME_FADE] = settings.sleepVolumeFade
            preferences[PreferencesKeys.SLEEP_FADE_DURATION] = settings.sleepFadeDurationSeconds
            preferences[PreferencesKeys.HEARING_PROTECTION] = settings.hearingProtectionEnabled
            preferences[PreferencesKeys.MAX_SAFE_VOLUME] = settings.maxSafeVolumePercent
            preferences[PreferencesKeys.TARGET_SAFE_DB] = settings.targetSafeDb
            preferences[PreferencesKeys.EQUALIZER_PRESET] = settings.equalizerPreset
            preferences[PreferencesKeys.BASS_BOOST] = settings.bassBoostLevel
            preferences[PreferencesKeys.HI_RES_ENGINE] = settings.hiResEngineEnabled
            preferences[PreferencesKeys.IS_SHUFFLE] = settings.isShuffleEnabled
            preferences[PreferencesKeys.SORT_BY_SHUFFLE] = settings.sortQueueByShuffleOrder
            preferences[PreferencesKeys.LOUDNESS_NORMALIZATION] = settings.loudnessNormalizationEnabled
            preferences[PreferencesKeys.TARGET_LOUDNESS_LUFS] = settings.targetLoudnessLufs
        }
    }

    suspend fun updateSleepSettings(
        minutes: Int,
        finishSong: Boolean,
        volumeFade: Boolean,
        fadeDurationSec: Int
    ) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SLEEP_TIMER_MINUTES] = minutes
            preferences[PreferencesKeys.SLEEP_FINISH_SONG] = finishSong
            preferences[PreferencesKeys.SLEEP_VOLUME_FADE] = volumeFade
            preferences[PreferencesKeys.SLEEP_FADE_DURATION] = fadeDurationSec
        }
    }

    suspend fun updateHearingProtection(enabled: Boolean, maxVolume: Int, targetDb: Float) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.HEARING_PROTECTION] = enabled
            preferences[PreferencesKeys.MAX_SAFE_VOLUME] = maxVolume
            preferences[PreferencesKeys.TARGET_SAFE_DB] = targetDb
        }
    }

    val tabShuffleStatesFlow: Flow<Map<String, Boolean>> = context.dataStore.data.map { preferences ->
        val raw = preferences[PreferencesKeys.TAB_SHUFFLE_STATES] ?: ""
        if (raw.isBlank()) {
            emptyMap()
        } else {
            try {
                raw.split(";").mapNotNull {
                    val parts = it.split("=")
                    if (parts.size == 2) parts[0] to parts[1].toBoolean() else null
                }.toMap()
            } catch (_: Exception) {
                emptyMap()
            }
        }
    }

    suspend fun updateShuffleState(isShuffle: Boolean, sortByShuffle: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.IS_SHUFFLE] = isShuffle
            preferences[PreferencesKeys.SORT_BY_SHUFFLE] = sortByShuffle
        }
    }

    suspend fun updateTabShuffleState(tabKey: String, isShuffle: Boolean) {
        context.dataStore.edit { preferences ->
            val raw = preferences[PreferencesKeys.TAB_SHUFFLE_STATES] ?: ""
            val currentMap = if (raw.isBlank()) mutableMapOf() else {
                raw.split(";").mapNotNull {
                    val parts = it.split("=")
                    if (parts.size == 2) parts[0] to parts[1].toBoolean() else null
                }.toMap().toMutableMap()
            }
            currentMap[tabKey] = isShuffle
            val serialized = currentMap.entries.joinToString(";") { "${it.key}=${it.value}" }
            preferences[PreferencesKeys.TAB_SHUFFLE_STATES] = serialized
        }
    }

    suspend fun updateEqualizer(preset: String, bassBoost: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.EQUALIZER_PRESET] = preset
            preferences[PreferencesKeys.BASS_BOOST] = bassBoost
        }
    }

    suspend fun updateHiResEngine(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.HI_RES_ENGINE] = enabled
        }
    }

    suspend fun updateLoudnessNormalization(enabled: Boolean, targetLufs: Float) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.LOUDNESS_NORMALIZATION] = enabled
            preferences[PreferencesKeys.TARGET_LOUDNESS_LUFS] = targetLufs
        }
    }
}
