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
 * VALUES ARE PLACEHOLDERS until the design lands (Intent 04). Applying the real
 * design should mean editing this file and nothing else — which is only true if
 * composables never inline a literal.
 */
@Immutable
data class EverfitColors(
    val todayHighlight: Color,
    val onTodayHighlight: Color,
    val statusCompleted: Color,
    val statusMissed: Color,
    val statusAssigned: Color,
    val statusUpcoming: Color,
    val screenBackground: Color,
    val cellBackground: Color,
    val cellBorder: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textDisabled: Color,
)

internal val LightEverfitColors = EverfitColors(
    todayHighlight = Color(0xFF6C5CE7),
    onTodayHighlight = Color(0xFFFFFFFF),
    statusCompleted = Color(0xFF27AE60),
    statusMissed = Color(0xFFEB5757),
    statusAssigned = Color(0xFF2D9CDB),
    statusUpcoming = Color(0xFFBDBDBD),
    screenBackground = Color(0xFFFFFFFF),
    cellBackground = Color(0xFFF7F7F9),
    cellBorder = Color(0xFFE6E6EB),
    textPrimary = Color(0xFF1A1A1F),
    textSecondary = Color(0xFF6B6B76),
    textDisabled = Color(0xFFB0B0BA),
)

/**
 * No sensible default: a missing provider is a wiring bug, and failing loudly at the
 * call site beats silently rendering the wrong palette.
 */
val LocalEverfitColors = staticCompositionLocalOf<EverfitColors> {
    error("EverfitColors not provided — wrap the content in EverfitTheme { }.")
}
