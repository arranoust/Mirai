package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Generates cohesive Material 3 colors around a dynamic seed primary color
fun buildCustomColorScheme(seedColor: Color, isDark: Boolean): androidx.compose.material3.ColorScheme {
    return if (isDark) {
        darkColorScheme(
            primary = seedColor,
            onPrimary = Color.White,
            primaryContainer = seedColor.copy(alpha = 0.25f),
            onPrimaryContainer = seedColor,
            secondary = seedColor.copy(alpha = 0.8f),
            onSecondary = Color.Black,
            secondaryContainer = Color(0xFF22222B),
            onSecondaryContainer = Color.White,
            background = Color(0xFF0F0F12), // Elegant, deep midnight black
            surface = Color(0xFF18181E), // Slightly elevated card background
            onBackground = Color(0xFFEEEEEE),
            onSurface = Color(0xFFEEEEEE),
            surfaceVariant = Color(0xFF22222B),
            onSurfaceVariant = Color(0xFFCCCCCC),
            outline = seedColor.copy(alpha = 0.5f)
        )
    } else {
        lightColorScheme(
            primary = seedColor,
            onPrimary = Color.White,
            primaryContainer = Color(0xFFE8DEF8), // M3 lavender active container
            onPrimaryContainer = Color(0xFF1D192B), // M3 dark purple text
            secondary = seedColor,
            onSecondary = Color.White,
            secondaryContainer = Color(0xFFE8DEF8), // matching text input/nav active pill background
            onSecondaryContainer = Color(0xFF1D192B),
            background = Color(0xFFFEF7FF), // Precise `#fef7ff` background
            onBackground = Color(0xFF1D1B20), // Precise `#1d1b20` text
            surface = Color(0xFFFEF7FF), // Consistent surface hue
            onSurface = Color(0xFF1D1B20),
            surfaceVariant = Color(0xFFE7E0EC), // Precise `#e7e0ec` elevated surfaces
            onSurfaceVariant = Color(0xFF49454F), // Precise `#49454f` text
            outline = Color(0xFFCAC4D0) // Elegant M3 outline border
        )
    }
}

@Composable
fun MyApplicationTheme(
    seedColorVal: Int = 0xFF4A6572.toInt(), // Default slate primary
    darkTheme: Boolean = true, // By default manga apps look outstanding in dark mode
    content: @Composable () -> Unit
) {
    val seedColor = Color(seedColorVal)
    val colorScheme = buildCustomColorScheme(seedColor, isDark = darkTheme)

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
