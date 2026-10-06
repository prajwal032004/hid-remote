package com.example.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** Semantic colour tokens. Components read these through [AppTheme.colors], never raw hex. */
@Immutable
data class AppColors(
    val background: Color,
    /** Cards and panels. */
    val surface: Color,
    /** Controls sitting on a card: keys, chips, list rows. */
    val surfaceHigh: Color,
    /** Emphasised controls: action keys, pressed states. */
    val surfaceHigher: Color,
    val outline: Color,
    val outlineStrong: Color,
    val text: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val accent: Color,
    val onAccent: Color,
    val success: Color = Color(0xFF34D399),
    val warning: Color = Color(0xFFFBBF24),
    val danger: Color = Color(0xFFF87171),
    val info: Color = Color(0xFF60A5FA),
) {
    val accentSoft: Color get() = accent.copy(alpha = 0.16f)
    val accentFaint: Color get() = accent.copy(alpha = 0.08f)
}

private val OnAccent = Color(0xFF071018)

fun darkAppColors(accent: Color) = AppColors(
    background = Color(0xFF0A0E17),
    surface = Color(0xFF121826),
    surfaceHigh = Color(0xFF1A2233),
    surfaceHigher = Color(0xFF243048),
    outline = Color(0xFF222C40),
    outlineStrong = Color(0xFF34405A),
    text = Color(0xFFF1F5F9),
    textSecondary = Color(0xFFA7B1C4),
    textMuted = Color(0xFF6E7A93),
    accent = accent,
    onAccent = OnAccent,
)

fun amoledAppColors(accent: Color) = AppColors(
    background = Color(0xFF000000),
    surface = Color(0xFF0B0D12),
    surfaceHigh = Color(0xFF14171F),
    surfaceHigher = Color(0xFF1E2330),
    outline = Color(0xFF1C212C),
    outlineStrong = Color(0xFF2C3342),
    text = Color(0xFFF1F5F9),
    textSecondary = Color(0xFFA7B1C4),
    textMuted = Color(0xFF6E7A93),
    accent = accent,
    onAccent = OnAccent,
)

val LocalAppColors = staticCompositionLocalOf { darkAppColors(Color(0xFF22D3EE)) }

/** Background used before Compose draws (window, splash) and by screenshot tests. */
val DarkBackground = Color(0xFF0A0E17)
