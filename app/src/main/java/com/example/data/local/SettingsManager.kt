package com.example.data.local

import android.content.Context
import android.graphics.Color
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class SettingsManager(context: Context) {
    private val prefs = context.getSharedPreferences("mirai_settings", Context.MODE_PRIVATE)

    // Flow emitters to notify Compose immediately when settings change!
    private val _themeColor = MutableStateFlow(prefs.getInt("theme_color", Color.parseColor("#4A6572"))) // Standard sophisticated slate primary
    val themeColor: StateFlow<Int> = _themeColor

    private val _komikCastEnabled = MutableStateFlow(prefs.getBoolean("komikcast_enabled", true))
    val komikCastEnabled: StateFlow<Boolean> = _komikCastEnabled

    private val _shinigamiEnabled = MutableStateFlow(prefs.getBoolean("shinigami_enabled", true))
    val shinigamiEnabled: StateFlow<Boolean> = _shinigamiEnabled

    private val _readerMode = MutableStateFlow(prefs.getString("reader_mode", "vertical") ?: "vertical")
    val readerMode: StateFlow<String> = _readerMode

    private val _tapToZoom = MutableStateFlow(prefs.getBoolean("tap_to_zoom", true))
    val tapToZoom: StateFlow<Boolean> = _tapToZoom

    private val _cacheBytesUsed = MutableStateFlow(prefs.getLong("cache_bytes", 15200000L)) // Simulated Cache Bytes
    val cacheBytesUsed: StateFlow<Long> = _cacheBytesUsed

    fun setThemeColor(colorArgb: Int) {
        prefs.edit().putInt("theme_color", colorArgb).apply()
        _themeColor.value = colorArgb
    }

    fun setKomikCastEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("komikcast_enabled", enabled).apply()
        _komikCastEnabled.value = enabled
    }

    fun setShinigamiEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("shinigami_enabled", enabled).apply()
        _shinigamiEnabled.value = enabled
    }

    fun setReaderMode(mode: String) {
        prefs.edit().putString("reader_mode", mode).apply()
        _readerMode.value = mode
    }

    fun setTapToZoom(enabled: Boolean) {
        prefs.edit().putBoolean("tap_to_zoom", enabled).apply()
        _tapToZoom.value = enabled
    }

    fun clearCache() {
        prefs.edit().putLong("cache_bytes", 0L).apply()
        _cacheBytesUsed.value = 0L
    }

    fun simulateCacheIncrease() {
        val current = _cacheBytesUsed.value
        val next = current + (1024 * 1024 * (1..4).random()).toLong()
        prefs.edit().putLong("cache_bytes", next).apply()
        _cacheBytesUsed.value = next
    }
}
