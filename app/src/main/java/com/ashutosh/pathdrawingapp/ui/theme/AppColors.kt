package com.ashutosh.pathdrawingapp.ui.theme

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

// ─────────────────────────────────────────────────────────────
//  Color Token Data Class
// ─────────────────────────────────────────────────────────────

data class AppColors(
    val bg: Color,
    val surface: Color,
    val surfaceHigh: Color,
    val border: Color,
    val borderMid: Color,
    val ink: Color,
    val inkSub: Color,
    val inkLight: Color,
    val accent: Color,
    val accentSoft: Color,
    val green: Color,
    val greenSoft: Color,
    val red: Color,
    val redSoft: Color,
    val amber: Color,
    val amberSoft: Color,
    val mapBg: Color,
    val mapGrid: Color,
    val pathBlue: Color,
    val entrance: Color,
    val exit: Color,
    val isDark: Boolean,
)

// ─────────────────────────────────────────────────────────────
//  Light Theme
// ─────────────────────────────────────────────────────────────

val LightColors = AppColors(
    bg          = Color(0xFFF8F9FC),
    surface     = Color(0xFFFFFFFF),
    surfaceHigh = Color(0xFFF0F2F7),
    border      = Color(0xFFE4E8F0),
    borderMid   = Color(0xFFCDD3E0),
    ink         = Color(0xFF0F1923),
    inkSub      = Color(0xFF6B7A99),
    inkLight    = Color(0xFFA0ABBD),
    accent      = Color(0xFF1A6BFF),
    accentSoft  = Color(0xFFEBF1FF),
    green       = Color(0xFF00C37A),
    greenSoft   = Color(0xFFE6FAF3),
    red         = Color(0xFFF03E3E),
    redSoft     = Color(0xFFFFF0F0),
    amber       = Color(0xFFF59E0B),
    amberSoft   = Color(0xFFFFF8EC),
    mapBg       = Color(0xFFEEF1F7),
    mapGrid     = Color(0xFFDDE2EE),
    pathBlue    = Color(0xFF1A6BFF),
    entrance    = Color(0xFF00C37A),
    exit        = Color(0xFFF03E3E),
    isDark      = false,
)

// ─────────────────────────────────────────────────────────────
//  Dark Theme
// ─────────────────────────────────────────────────────────────

val DarkColors = AppColors(
    bg          = Color(0xFF0D1017),
    surface     = Color(0xFF161B26),
    surfaceHigh = Color(0xFF1C2235),
    border      = Color(0xFF252D42),
    borderMid   = Color(0xFF38435C),
    ink         = Color(0xFFE8EDF5),
    inkSub      = Color(0xFF8A95AB),
    inkLight    = Color(0xFF52617A),
    accent      = Color(0xFF4D8FFF),
    accentSoft  = Color(0xFF152040),
    green       = Color(0xFF00E676),
    greenSoft   = Color(0xFF062A18),
    red         = Color(0xFFFF5252),
    redSoft     = Color(0xFF2A0A0A),
    amber       = Color(0xFFFFB300),
    amberSoft   = Color(0xFF2A1A00),
    mapBg       = Color(0xFF111520),
    mapGrid     = Color(0xFF1A2030),
    pathBlue    = Color(0xFF4D8FFF),
    entrance    = Color(0xFF00E676),
    exit        = Color(0xFFFF5252),
    isDark      = true,
)

// ─────────────────────────────────────────────────────────────
//  CompositionLocal — access colors from anywhere
// ─────────────────────────────────────────────────────────────

val LocalAppColors = compositionLocalOf<AppColors> { LightColors }
