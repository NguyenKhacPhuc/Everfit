package com.example.everfit.assignment.feature.calendar.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.everfit.assignment.core.ui.theme.EverfitTheme

/**
 * A placeholder where a workout card will land, with a shimmer sweeping across it.
 *
 * The design provides no loading treatment, so this is invented — kept restrained
 * by reusing the card's geometry and keeping the two band colours a few points
 * either side of the card fill. A high-contrast sweep would read as a different
 * component rather than a card that has not arrived yet.
 *
 * Height matches a single-workout card, so the row does not resize when real
 * content replaces it.
 */
@Composable
fun WorkoutSkeleton(modifier: Modifier = Modifier) {
    val base = EverfitTheme.colors.skeletonBase
    val highlight = EverfitTheme.colors.skeletonHighlight

    val transition = rememberInfiniteTransition(label = "shimmer")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            // Linear, so the sweep has no apparent acceleration; Restart rather
            // than Reverse, so it always travels the same way.
            animation = tween(durationMillis = SWEEP_MS, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "sweep",
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(SKELETON_HEIGHT)
            .clip(RoundedCornerShape(EverfitTheme.sizes.cardCorner))
            // drawBehind, NOT drawWithCache: the cache-building lambda does not
            // re-run per frame, so reading the animated value there renders a
            // gradient that never moves.
            .drawBehind {
                val bandWidth = size.width * BAND_FRACTION
                // Starts fully off the left edge and ends fully off the right, so
                // the band is never parked mid-view between cycles.
                val originX = -bandWidth + progress * (size.width + bandWidth)
                drawRect(
                    Brush.linearGradient(
                        colors = listOf(base, highlight, base),
                        start = Offset(originX, 0f),
                        end = Offset(originX + bandWidth, 0f),
                    )
                )
            }
            // Nothing here is readable; announcing an empty box would be noise.
            .clearAndSetSemantics { },
    )
}

private val SKELETON_HEIGHT = 74.dp
private const val SWEEP_MS = 1_100
private const val BAND_FRACTION = 0.55f

@Preview(showBackground = true, widthDp = 320)
@Composable
private fun WorkoutSkeletonPreview() {
    EverfitTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            WorkoutSkeleton()
        }
    }
}
