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

/** Sizes that are neither spacing nor typography. */
@Immutable
data class EverfitSizes(
    val cellCorner: Dp = 8.dp,
    val cellBorder: Dp = 1.dp,
    val checkmark: Dp = 16.dp,
    val statusDot: Dp = 6.dp,
)

val LocalEverfitSpacing = staticCompositionLocalOf { EverfitSpacing() }
val LocalEverfitSizes = staticCompositionLocalOf { EverfitSizes() }
