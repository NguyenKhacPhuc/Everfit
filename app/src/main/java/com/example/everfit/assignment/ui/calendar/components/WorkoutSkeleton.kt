package com.example.everfit.assignment.ui.calendar.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.everfit.assignment.ui.theme.EverfitTheme

/**
 * A placeholder where a workout card will land.
 *
 * Deliberately flat rather than shimmering. The design provides no loading
 * treatment at all, so anything here is invented — and the most restrained
 * invention is to reuse the card's own geometry and background token.
 *
 * Height matches a single-workout card (title plus one subtitle line), so the
 * row does not resize when real content replaces it.
 */
@Composable
fun WorkoutSkeleton(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(SKELETON_HEIGHT)
            .clip(RoundedCornerShape(EverfitTheme.sizes.cardCorner))
            .background(EverfitTheme.colors.cardBackground)
            // Nothing here is readable; announcing an empty box would be noise.
            .clearAndSetSemantics { },
    )
}

private val SKELETON_HEIGHT = 74.dp

@Preview(showBackground = true, widthDp = 320)
@Composable
private fun WorkoutSkeletonPreview() {
    EverfitTheme { WorkoutSkeleton(Modifier.padding(16.dp)) }
}
