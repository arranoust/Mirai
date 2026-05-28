package org.mirai.app.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "mirai_settings")

class SettingsManager(context: Context) {

    private val dataStore = context.applicationContext.dataStore
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    companion object {
        val KEY_THEME_COLOR = intPreferencesKey("theme_color")
        val KEY_DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color") // <-- BARU
        val KEY_DARK_THEME = booleanPreferencesKey("dark_theme")       // <-- BARU
        val KEY_KOMIKCAST_ENABLED = booleanPreferencesKey("komikcast_enabled")
        val KEY_SHINIGAMI_ENABLED = booleanPreferencesKey("shinigami_enabled")
        val KEY_READER_MODE = stringPreferencesKey("reader_mode")
        val KEY_TAP_TO_ZOOM = booleanPreferencesKey("tap_to_zoom")
        val KEY_CACHE_BYTES = longPreferencesKey("cache_bytes")

        const val DEFAULT_THEME_COLOR = 0xFF4A6572.toInt()
        const val DEFAULT_CACHE_BYTES = 15_200_000L
    }

    val themeColor: StateFlow<Int> = dataStore.data
        .map { prefs -> prefs[KEY_THEME_COLOR] ?: DEFAULT_THEME_COLOR }
        .stateIn(scope, SharingStarted.Eagerly, DEFAULT_THEME_COLOR)

    // true = pakai Material You dynamic color (Android 12+)
    val isDynamicColor: StateFlow<Boolean> = dataStore.data
        .map { prefs -> prefs[KEY_DYNAMIC_COLOR] ?: false }
        .stateIn(scope, SharingStarted.Eagerly, false)

    // true = dark theme, false = light theme
    val isDarkTheme: StateFlow<Boolean> = dataStore.data
        .map { prefs -> prefs[KEY_DARK_THEME] ?: true }
        .stateIn(scope, SharingStarted.Eagerly, true)

    val komikCastEnabled: StateFlow<Boolean> = dataStore.data
        .map { prefs -> prefs[KEY_KOMIKCAST_ENABLED] ?: true }
        .stateIn(scope, SharingStarted.Eagerly, true)

    val shinigamiEnabled: StateFlow<Boolean> = dataStore.data
        .map { prefs -> prefs[KEY_SHINIGAMI_ENABLED] ?: true }
        .stateIn(scope, SharingStarted.Eagerly, true)

    val readerMode: StateFlow<String> = dataStore.data
        .map { prefs -> prefs[KEY_READER_MODE] ?: "vertical" }
        .stateIn(scope, SharingStarted.Eagerly, "vertical")

    val tapToZoom: StateFlow<Boolean> = dataStore.data
        .map { prefs -> prefs[KEY_TAP_TO_ZOOM] ?: true }
        .stateIn(scope, SharingStarted.Eagerly, true)

    val cacheBytesUsed: StateFlow<Long> = dataStore.data
        .map { prefs -> prefs[KEY_CACHE_BYTES] ?: DEFAULT_CACHE_BYTES }
        .stateIn(scope, SharingStarted.Eagerly, DEFAULT_CACHE_BYTES)

    fun setThemeColor(colorArgb: Int) {
        scope.launch { dataStore.edit { it[KEY_THEME_COLOR] = colorArgb } }
    }

    fun setDynamicColor(enabled: Boolean) {
        scope.launch { dataStore.edit { it[KEY_DYNAMIC_COLOR] = enabled } }
    }

    fun setDarkTheme(dark: Boolean) {
        scope.launch { dataStore.edit { it[KEY_DARK_THEME] = dark } }
    }

    fun setKomikCastEnabled(enabled: Boolean) {
        scope.launch { dataStore.edit { it[KEY_KOMIKCAST_ENABLED] = enabled } }
    }

    fun setShinigamiEnabled(enabled: Boolean) {
        scope.launch { dataStore.edit { it[KEY_SHINIGAMI_ENABLED] = enabled } }
    }

    fun setReaderMode(mode: String) {
        scope.launch { dataStore.edit { it[KEY_READER_MODE] = mode } }
    }

    fun setTapToZoom(enabled: Boolean) {
        scope.launch { dataStore.edit { it[KEY_TAP_TO_ZOOM] = enabled } }
    }

    fun clearCache() {
        scope.launch { dataStore.edit { it[KEY_CACHE_BYTES] = 0L } }
    }

    fun simulateCacheIncrease() {
        scope.launch {
            dataStore.edit { prefs ->
                val current = prefs[KEY_CACHE_BYTES] ?: DEFAULT_CACHE_BYTES
                prefs[KEY_CACHE_BYTES] = current + (1024 * 1024 * (1..4).random()).toLong()
            }
        }
    }
}