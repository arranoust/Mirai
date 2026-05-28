package org.mirai.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

/**
 * Membangun color scheme Material 3 dari satu seed color.
 *
 * PENYEBAB BUG SEBELUMNYA:
 * - Di dark mode, `onPrimary = Color.White` — tapi ketika seed color TERANG
 *   (misal slate biru-abu), `primary` di dark mode juga jadi terang,
 *   sehingga teks putih di atas primary container TIDAK TERLIHAT (transparan seolah).
 * - Solusi: hitung luminance seed, lalu pilih warna kontras yang tepat.
 * - Atau lebih baik: gunakan MaterialTheme.colorScheme yang sudah dihitung M3.
 *
 * CARA YANG BENAR: gunakan `dynamicColorScheme` (Android 12+) untuk Material You,
 * atau biarkan M3 menghitung kontras via lightColorScheme/darkColorScheme dengan
 * warna yang sudah dihitung dengan benar.
 */
fun buildCustomColorScheme(seedColor: Color, isDark: Boolean): ColorScheme {
    // Hitung apakah seed color cukup gelap untuk dijadikan teks di atas warna terang
    val luminance = (0.2126f * seedColor.red + 0.7152f * seedColor.green + 0.0722f * seedColor.blue)
    val onPrimaryColor = if (luminance > 0.4f) Color(0xFF1A1A2E) else Color.White

    return if (isDark) {
        // Dark scheme: primary harus LEBIH TERANG dari seed supaya kontras di background gelap
        val primaryDark = seedColor.copy(
            red = (seedColor.red + 0.25f).coerceAtMost(1f),
            green = (seedColor.green + 0.25f).coerceAtMost(1f),
            blue = (seedColor.blue + 0.25f).coerceAtMost(1f),
            alpha = 1f
        )
        darkColorScheme(
            primary = primaryDark,
            onPrimary = Color(0xFF1A1A2E),            // GELAP — kontras di atas primary terang
            primaryContainer = seedColor.copy(alpha = 1f).let {
                // Container lebih gelap dari primary
                Color(
                    red = (it.red * 0.4f).coerceAtMost(1f),
                    green = (it.green * 0.4f).coerceAtMost(1f),
                    blue = (it.blue * 0.4f).coerceAtMost(1f),
                    alpha = 1f
                )
            },
            onPrimaryContainer = primaryDark,         // TERANG — kontras di atas container gelap
            secondary = primaryDark.copy(
                red = (primaryDark.red * 0.85f),
                green = (primaryDark.green * 0.85f),
                blue = (primaryDark.blue * 0.85f),
                alpha = 1f
            ),
            onSecondary = Color(0xFF1A1A2E),
            secondaryContainer = Color(0xFF22222B),
            onSecondaryContainer = Color(0xFFEEEEEE),
            background = Color(0xFF0F0F12),
            surface = Color(0xFF18181E),
            onBackground = Color(0xFFEEEEEE),
            onSurface = Color(0xFFEEEEEE),
            surfaceVariant = Color(0xFF22222B),
            onSurfaceVariant = Color(0xFFCCCCCC),
            outline = primaryDark.copy(alpha = 0.5f),
            error = Color(0xFFCF6679),
            onError = Color(0xFF1A1A2E)
        )
    } else {
        lightColorScheme(
            primary = seedColor,
            onPrimary = onPrimaryColor,               // DIHITUNG dari luminance seed
            primaryContainer = Color(
                red = (seedColor.red * 0.85f + 0.15f).coerceAtMost(1f),
                green = (seedColor.green * 0.85f + 0.15f).coerceAtMost(1f),
                blue = (seedColor.blue * 0.85f + 0.15f).coerceAtMost(1f),
                alpha = 1f
            ),
            onPrimaryContainer = Color(0xFF1D192B),
            secondary = seedColor,
            onSecondary = onPrimaryColor,
            secondaryContainer = Color(0xFFE8DEF8),
            onSecondaryContainer = Color(0xFF1D192B),
            background = Color(0xFFFEF7FF),
            onBackground = Color(0xFF1D1B20),
            surface = Color(0xFFFEF7FF),
            onSurface = Color(0xFF1D1B20),
            surfaceVariant = Color(0xFFE7E0EC),
            onSurfaceVariant = Color(0xFF49454F),
            outline = Color(0xFFCAC4D0),
            error = Color(0xFFB3261E),
            onError = Color.White
        )
    }
}

@Composable
fun MyApplicationTheme(
    seedColorVal: Int = 0xFF4A6572.toInt(),
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,   // <-- PARAMETER BARU untuk Material You
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        // Material You: Android 12+ (API 31+) saja
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context)
            else dynamicLightColorScheme(context)
        }
        // Custom seed color scheme
        else -> {
            val seedColor = Color(seedColorVal)
            buildCustomColorScheme(seedColor, isDark = darkTheme)
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}