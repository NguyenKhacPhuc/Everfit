package com.example.everfit.assignment.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable

/**
 * Wraps MaterialTheme so Material components keep working, and layers this app's
 * semantic tokens on top via CompositionLocals.
 *
 * Light only, deliberately: the design is light, and a second token set would double
 * both the theme work and the screenshot checks. `values-night` was removed so a dark
 * theme cannot appear unreviewed.
 */
@Composable
fun EverfitTheme(content: @Composable () -> Unit) {
    val colors = LightEverfitColors
    CompositionLocalProvider(
        LocalEverfitColors provides colors,
        LocalEverfitSpacing provides EverfitSpacing(),
        LocalEverfitSizes provides EverfitSizes(),
    ) {
        MaterialTheme(
            colorScheme = lightColorScheme(
                primary = colors.accent,
                onPrimary = colors.onAccent,
                background = colors.screenBackground,
                surface = colors.cardBackground,
                onBackground = colors.textPrimary,
                onSurface = colors.textPrimary,
            ),
            typography = EverfitTypography,
            content = content,
        )
    }
}

/** Access point: `EverfitTheme.colors.statusMissed` rather than a raw CompositionLocal. */
object EverfitTheme {
    val colors: EverfitColors
        @Composable @ReadOnlyComposable get() = LocalEverfitColors.current

    val spacing: EverfitSpacing
        @Composable @ReadOnlyComposable get() = LocalEverfitSpacing.current

    val sizes: EverfitSizes
        @Composable @ReadOnlyComposable get() = LocalEverfitSizes.current
}
