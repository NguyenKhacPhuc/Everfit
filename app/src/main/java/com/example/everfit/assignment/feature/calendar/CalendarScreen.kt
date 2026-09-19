package com.example.everfit.assignment.feature.calendar

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.example.everfit.assignment.R
import com.example.everfit.assignment.core.model.DisplayStatus
import com.example.everfit.assignment.core.ui.component.DayCell
import com.example.everfit.assignment.core.ui.component.WorkoutCard
import com.example.everfit.assignment.core.ui.calendar.components.WorkoutSkeleton
import kotlinx.coroutines.delay
import com.example.everfit.assignment.core.ui.theme.EverfitTheme
import java.time.LocalDate

/**
 * Stateless: state in, intents out. It never sees a ViewModel, which is what
 * makes every state below previewable with no database, network or coroutine.
 */
@Composable
fun CalendarScreen(
    state: CalendarState,
    onIntent: (CalendarIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(EverfitTheme.colors.screenBackground),
    ) {
        if (state.showsFullScreenError) {
            FullScreenError(onRetry = { onIntent(CalendarIntent.Refresh) })
        } else {
            // Gated by a short delay: a warm start answers from cache almost
            // immediately, and flashing placeholders at it would trade one
            // flicker for another.
            val showPlaceholders = delayed(state.showsLoadingPlaceholders)

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(state.days, key = { it.date }) { day ->
                    DayCell(date = day.date, isToday = day.isToday) {
                        Crossfade(
                            targetState = showPlaceholders,
                            animationSpec = tween(CONTENT_FADE_MS),
                            label = "workouts",
                        ) { loading ->
                            Column(
                                verticalArrangement = Arrangement.spacedBy(
                                    EverfitTheme.spacing.sm
                                ),
                            ) {
                                if (loading) {
                                    WorkoutSkeleton()
                                } else {
                                    day.workouts.forEach { workout ->
                                        WorkoutCard(
                                            workout = workout,
                                            onClick = {
                                                onIntent(
                                                    CalendarIntent.ToggleCompletion(workout.id)
                                                )
                                            },
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * True only once [value] has held for [delayMillis]. Falls back to false the
 * instant [value] does, so content is never held back once it arrives.
 */
@Composable
private fun delayed(value: Boolean, delayMillis: Long = PLACEHOLDER_DELAY_MS): Boolean {
    var settled by remember { mutableStateOf(false) }
    LaunchedEffect(value) {
        if (value) {
            delay(delayMillis)
            settled = true
        } else {
            settled = false
        }
    }
    return value && settled
}

private const val PLACEHOLDER_DELAY_MS = 150L
private const val CONTENT_FADE_MS = 220

@Composable
private fun FullScreenError(onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.error_generic),
            style = MaterialTheme.typography.bodyMedium,
            color = EverfitTheme.colors.textSecondary,
        )
        TextButton(onClick = onRetry) {
            Text(stringResource(R.string.action_retry), color = EverfitTheme.colors.accent)
        }
    }
}

// ── previews: the finite list of states the contract allows ───────────────

private val MONDAY = LocalDate.parse("2026-09-14")
private val WEEK = List(7) { MONDAY.plusDays(it.toLong()) }
private val TODAY = WEEK[4]

private fun emptyDays() = WEEK.map { DayUiModel(it, it == TODAY, emptyList()) }

/** The design's populated state, reproduced from the fixture. */
private fun populatedDays(): List<DayUiModel> {
    fun w(id: String, title: String, count: Int, status: DisplayStatus) =
        WorkoutUiModel(id, title, count, status)

    val byIndex = mapOf(
        0 to listOf(w("1", "Legs day", 5, DisplayStatus.MISSED)),
        1 to listOf(w("2", "Full warm up workout", 6, DisplayStatus.COMPLETED)),
        3 to listOf(w("3", "Chest and shoulder workout", 9, DisplayStatus.MISSED)),
        4 to listOf(
            w("4", "Legs day", 7, DisplayStatus.ASSIGNED),
            w("5", "HIIT Tabata 20:10 8x8", 15, DisplayStatus.ASSIGNED),
        ),
        6 to listOf(w("6", "Squat, press, power clean", 6, DisplayStatus.UPCOMING)),
    )
    return WEEK.mapIndexed { i, date -> DayUiModel(date, date == TODAY, byIndex[i].orEmpty()) }
}

@Preview(showBackground = true, widthDp = 375, heightDp = 900, name = "Content")
@Composable
private fun CalendarContentPreview() {
    EverfitTheme {
        CalendarScreen(
            state = CalendarState(WEEK, TODAY, populatedDays(), Load.Idle),
            onIntent = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 375, heightDp = 900, name = "Loading / empty")
@Composable
private fun CalendarLoadingPreview() {
    EverfitTheme {
        CalendarScreen(
            state = CalendarState(WEEK, TODAY, emptyDays(), Load.Refreshing),
            onIntent = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 375, heightDp = 400, name = "Full-screen error")
@Composable
private fun CalendarErrorPreview() {
    EverfitTheme {
        CalendarScreen(
            state = CalendarState(WEEK, TODAY, emptyDays(), Load.Failed("offline")),
            onIntent = {},
        )
    }
}
