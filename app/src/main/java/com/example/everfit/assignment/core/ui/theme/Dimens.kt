package com.example.everfit.assignment.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Spacing scale. Placeholder values; see [EverfitColors] for why they live here. */
@Immutable
data class EverfitSpacing(
    val xs: Dp = 4.dp,
    val sm: Dp = 8.dp,
    val md: Dp = 12.dp,
    val lg: Dp = 16.dp,
    val xl: Dp = 24.dp,
)

/**
 * Sizes measured from docs/design/training.png (750px wide at 2x, so a 375dp
 * screen). Refined further in Intent 04.
 */
@Immutable
data class EverfitSizes(
    val screenPadding: Dp = 20.dp,
    val dayLabelWidth: Dp = 52.dp,
    val cardCorner: Dp = 10.dp,
    val cardPadding: Dp = 16.dp,
    val checkmark: Dp = 24.dp,
    val dividerThickness: Dp = 1.dp,
)

val LocalEverfitSpacing = staticCompositionLocalOf { EverfitSpacing() }
val LocalEverfitSizes = staticCompositionLocalOf { EverfitSizes() }
