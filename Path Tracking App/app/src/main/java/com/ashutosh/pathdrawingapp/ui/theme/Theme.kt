package com.ashutosh.pathdrawingapp.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

// ─────────────────────────────────────────────────────────────
//  Light colour scheme
// ─────────────────────────────────────────────────────────────

private val LightColorScheme = lightColorScheme(
    primary          = Color(0xFF1A6BFF),
    onPrimary        = Color.White,
    primaryContainer = Color(0xFFEBF1FF),
    onPrimaryContainer = Color(0xFF1A6BFF),

    secondary        = Color(0xFF00C37A),
    onSecondary      = Color.White,

    background       = Color(0xFFF8F9FC),
    onBackground     = Color(0xFF0F1923),

    surface          = Color(0xFFFFFFFF),
    onSurface        = Color(0xFF0F1923),

    outline          = Color(0xFFE4E8F0),
    error            = Color(0xFFF03E3E)
)

// ─────────────────────────────────────────────────────────────
//  Dark colour scheme — deep navy palette
// ─────────────────────────────────────────────────────────────

private val DarkColorScheme = darkColorScheme(
    primary          = Color(0xFF00D4FF), // cyan accent
    onPrimary        = Color(0xFF0A0E1A),
    primaryContainer = Color(0xFF00D4FF).copy(alpha = 0.15f),

    secondary        = Color(0xFF00FF9C), // green accent
    onSecondary      = Color(0xFF0A0E1A),

    tertiary         = Color(0xFFFFB547), // amber accent
    onTertiary       = Color(0xFF0A0E1A),

    background       = Color(0xFF0A0E1A), // deep navy
    onBackground     = Color(0xFFE8EFF7),

    surface          = Color(0xFF131929),
    onSurface        = Color(0xFFE8EFF7),

    outline          = Color(0xFF1E2D42),
    error            = Color(0xFFFF5370)
)

/**
 * IndoorMapperTheme — wraps the app in Material3 styling.
 *
 * Modified to use Light theme by default to support dark font colors.
 */
@Composable
fun IndoorMapperTheme(
    darkTheme: Boolean = false, // Changed default to false for "dark fonts"
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColors else LightColors
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    CompositionLocalProvider(LocalAppColors provides colors) {
        MaterialTheme(
            colorScheme = colorScheme,
            content     = content
        )
    }
}
