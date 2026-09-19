package com.example.everfit.assignment.core.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.example.everfit.assignment.R
import com.example.everfit.assignment.core.model.DisplayStatus
import com.example.everfit.assignment.feature.calendar.WorkoutUiModel
import com.example.everfit.assignment.core.ui.theme.EverfitTheme

/**
 * One workout card.
 *
 * Every state is driven by [WorkoutUiModel.displayStatus], so there is no branch
 * here that the state type does not force. Values read off
 * docs/design/training.png:
 *
 *   Completed → accent fill, white text, check on the right, NO exercise count
 *   Missed    → card fill, red status word, then the count
 *   Assigned  → card fill, count only, no status word
 *   Upcoming  → card fill, everything greyed
 */
@Composable
fun WorkoutCard(
    workout: WorkoutUiModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isCompleted = workout.displayStatus == DisplayStatus.COMPLETED
    val isUpcoming = workout.displayStatus == DisplayStatus.UPCOMING

    val background =
        if (isCompleted) EverfitTheme.colors.accent else EverfitTheme.colors.cardBackground
    val titleColour = when {
        isCompleted -> EverfitTheme.colors.onAccent
        isUpcoming -> EverfitTheme.colors.textSecondary
        else -> EverfitTheme.colors.textPrimary
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(EverfitTheme.sizes.cardCorner))
            .background(background)
            .clickable(onClick = onClick)
            .padding(EverfitTheme.sizes.cardPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(EverfitTheme.spacing.xs),
        ) {
            Text(
                text = workout.title,
                // Weight comes from the style; overriding it here would give the
                // theme two sources of truth for the same property.
                style = MaterialTheme.typography.titleMedium,
                color = titleColour,
                // The brief requires truncation with an ellipsis; the design keeps
                // a title to one line.
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            WorkoutSubtitle(workout = workout)
        }

        if (workout.showsCheckmark) {
            CompletedCheck()
        }
    }
}

@Composable
private fun WorkoutSubtitle(workout: WorkoutUiModel) {
    val count = pluralStringResource(
        R.plurals.exercise_count,
        workout.totalExercises,
        workout.totalExercises,
    )

    Row {
        when (workout.displayStatus) {
            DisplayStatus.COMPLETED -> Text(
                text = stringResource(R.string.status_completed),
                style = MaterialTheme.typography.bodyMedium,
                color = EverfitTheme.colors.onAccent,
            )

            DisplayStatus.MISSED -> {
                Text(
                    text = stringResource(R.string.status_missed),
                    style = MaterialTheme.typography.bodyMedium,
                    color = EverfitTheme.colors.statusMissed,
                )
                Text(
                    text = stringResource(R.string.separator_dot) + count,
                    style = MaterialTheme.typography.bodyMedium,
                    color = EverfitTheme.colors.textPrimary,
                )
            }

            // Today shows no status word, only the count.
            DisplayStatus.ASSIGNED -> Text(
                text = count,
                style = MaterialTheme.typography.bodyMedium,
                color = EverfitTheme.colors.textPrimary,
            )

            // A future day is greyed whatever its stored status.
            DisplayStatus.UPCOMING -> Text(
                text = count,
                style = MaterialTheme.typography.bodyMedium,
                color = EverfitTheme.colors.textSecondary,
            )
        }
    }
}

@Composable
private fun CompletedCheck() {
    Box(
        modifier = Modifier
            .size(EverfitTheme.sizes.checkmark)
            .clip(CircleShape)
            .background(EverfitTheme.colors.onAccent),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.Check,
            contentDescription = stringResource(R.string.cd_completed),
            tint = EverfitTheme.colors.accent,
            modifier = Modifier.size(EverfitTheme.sizes.checkmark * CHECK_GLYPH_RATIO),
        )
    }
}

private const val CHECK_GLYPH_RATIO = 0.7f

/** Every state the contract allows, which is what makes this list finite. */
@Preview(showBackground = true, widthDp = 320)
@Composable
private fun WorkoutCardStatesPreview() {
    EverfitTheme {
        Column(
            modifier = Modifier
                .background(Color.White)
                .padding(EverfitTheme.spacing.lg),
            verticalArrangement = Arrangement.spacedBy(EverfitTheme.spacing.sm),
        ) {
            WorkoutCard(WorkoutUiModel("1", "Legs day", 5, DisplayStatus.MISSED), {})
            WorkoutCard(WorkoutUiModel("2", "Full warm up workout", 6, DisplayStatus.COMPLETED), {})
            WorkoutCard(WorkoutUiModel("3", "Legs day", 7, DisplayStatus.ASSIGNED), {})
            WorkoutCard(WorkoutUiModel("4", "Squat, press, power clean", 6, DisplayStatus.UPCOMING), {})
            WorkoutCard(
                WorkoutUiModel("5", "A deliberately long workout title that must truncate", 12, DisplayStatus.ASSIGNED),
                {},
            )
        }
    }
}
