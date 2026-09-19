package com.example.everfit.assignment.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Semantic colour tokens for this app.
 *
 * These are NOT Material [androidx.compose.material3.ColorScheme] slots on purpose:
 * Material has no slot meaning "missed" or "upcoming", and mapping them onto `error`
 * or `tertiary` produces names every later reader has to decode. See Drill 00 §5b.
 *
 * Values sampled from docs/design/training.png. Because composables never inline
 * a literal, applying the design meant editing this file and nothing else — which
 * is what the token layer was for.
 */
@Immutable
data class EverfitColors(
    /** Today's date label, and the background of a completed workout card. */
    val accent: Color,
    val onAccent: Color,
    val statusMissed: Color,
    val screenBackground: Color,
    val cardBackground: Color,
    /** Shimmer band endpoints. Slightly off the card fill so the sweep reads. */
    val skeletonBase: Color,
    val skeletonHighlight: Color,
    val divider: Color,
    val textPrimary: Color,
    val textSecondary: Color,
)

internal val LightEverfitColors = EverfitColors(
    accent = Color(0xFF7470EF),
    onAccent = Color(0xFFFFFFFF),
    statusMissed = Color(0xFFFF5E5E),
    screenBackground = Color(0xFFFFFFFF),
    cardBackground = Color(0xFFF7F8FC),
    skeletonBase = Color(0xFFE6E9F3),
    skeletonHighlight = Color(0xFFFFFFFF),
    divider = Color(0xFFF1F1F1),
    textPrimary = Color(0xFF1E0A3C),
    textSecondary = Color(0xFF7B7E91),
)

/**
 * No sensible default: a missing provider is a wiring bug, and failing loudly at the
 * call site beats silently rendering the wrong palette.
 */
val LocalEverfitColors = staticCompositionLocalOf<EverfitColors> {
    error("EverfitColors not provided — wrap the content in EverfitTheme { }.")
}
