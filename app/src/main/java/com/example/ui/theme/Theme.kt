package com.example.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.model.AccentColor

object AppTheme {
    val colors: AppColors
        @Composable @ReadOnlyComposable get() = LocalAppColors.current
}

/** Material 3 "emphasized" easing: quick start, long gentle settle. Used for every screen move. */
val EmphasizedEasing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
val EmphasizedDecelerate = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)

@Composable
fun MyApplicationTheme(
    accent: AccentColor = AccentColor.CYAN,
    amoled: Boolean = false,
    content: @Composable () -> Unit
) {
    val colors = remember(accent, amoled) {
        val a = Color(accent.argb)
        if (amoled) amoledAppColors(a) else darkAppColors(a)
    }
    val scheme = remember(colors) {
        darkColorScheme(
            primary = colors.accent,
            onPrimary = colors.onAccent,
            primaryContainer = colors.accentSoft,
            onPrimaryContainer = colors.accent,
            secondary = colors.accent,
            onSecondary = colors.onAccent,
            secondaryContainer = colors.surfaceHigher,
            onSecondaryContainer = colors.text,
            tertiary = colors.success,
            background = colors.background,
            onBackground = colors.text,
            surface = colors.surface,
            onSurface = colors.text,
            surfaceVariant = colors.surfaceHigh,
            onSurfaceVariant = colors.textSecondary,
            surfaceContainerHighest = colors.surfaceHigher,
            inverseSurface = colors.text,
            inverseOnSurface = colors.background,
            outline = colors.outlineStrong,
            outlineVariant = colors.outline,
            error = colors.danger,
            onError = Color.White
        )
    }
    CompositionLocalProvider(LocalAppColors provides colors) {
        MaterialTheme(
            colorScheme = scheme,
            typography = Typography,
            shapes = Shapes(
                extraSmall = RoundedCornerShape(8.dp),
                small = RoundedCornerShape(12.dp),
                medium = RoundedCornerShape(16.dp),
                large = RoundedCornerShape(24.dp),
                extraLarge = RoundedCornerShape(32.dp)
            ),
            content = content
        )
    }
}
