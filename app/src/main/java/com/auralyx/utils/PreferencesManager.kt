package com.auralyx.utils

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PreferencesManager @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    companion object {
        val KEY_DARK_THEME          = booleanPreferencesKey("dark_theme")
        val KEY_DEFAULT_VIDEO_MODE  = booleanPreferencesKey("default_video_mode")
        val KEY_SCAN_ON_LAUNCH      = booleanPreferencesKey("scan_on_launch")
    }

    val isDarkTheme: Flow<Boolean> = dataStore.data.map { it[KEY_DARK_THEME] ?: true }
    val isDefaultVideoMode: Flow<Boolean> = dataStore.data.map { it[KEY_DEFAULT_VIDEO_MODE] ?: false }
    val scanOnLaunch: Flow<Boolean> = dataStore.data.map { it[KEY_SCAN_ON_LAUNCH] ?: true }

    suspend fun setDarkTheme(enabled: Boolean) {
        dataStore.edit { it[KEY_DARK_THEME] = enabled }
    }

    suspend fun setDefaultVideoMode(enabled: Boolean) {
        dataStore.edit { it[KEY_DEFAULT_VIDEO_MODE] = enabled }
    }

    suspend fun setScanOnLaunch(enabled: Boolean) {
        dataStore.edit { it[KEY_SCAN_ON_LAUNCH] = enabled }
    }
}
