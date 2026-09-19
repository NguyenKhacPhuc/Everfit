package com.example.everfit.assignment.feature.calendar

import androidx.compose.runtime.Immutable
import com.example.everfit.assignment.core.model.DisplayStatus
import java.time.LocalDate

/**
 * What the screen renders and what it can ask for.
 *
 * `days` (content) and `load` (status) are separate fields. A failed refresh over
 * good cached data is simultaneously content-bearing and errored, which one
 * collapsed hierarchy cannot express — while `load` being sealed still makes
 * "refreshing and failed at once" unrepresentable, which two booleans would allow.
 */
@Immutable
data class CalendarState(
    val weekDates: List<LocalDate> = emptyList(),
    val today: LocalDate = LocalDate.MIN,
    val days: List<DayUiModel> = emptyList(),
    val load: Load = Load.Idle,
) {
    /** Derived, never stored — a stored copy is one more thing to keep in step. */
    val showsFullScreenError: Boolean
        get() = load is Load.Failed && days.all { it.workouts.isEmpty() }

    val isRefreshing: Boolean get() = load is Load.Refreshing

    /**
     * True only on a cold start: refreshing with nothing yet on screen.
     *
     * Derived rather than stored, which is what makes the three cases fall out
     * without extra state — a background refresh over content is false because
     * `days` is populated, and a week that genuinely has no workouts is false
     * because `load` has settled.
     */
    val showsLoadingPlaceholders: Boolean
        get() = isRefreshing && days.all { it.workouts.isEmpty() }
}

@Immutable
sealed interface Load {
    data object Idle : Load
    data object Refreshing : Load
    data class Failed(val message: String) : Load
}

@Immutable
data class DayUiModel(
    val date: LocalDate,
    val isToday: Boolean,
    val workouts: List<WorkoutUiModel>,
)

@Immutable
data class WorkoutUiModel(
    val id: String,
    val title: String,
    val totalExercises: Int,
    val displayStatus: DisplayStatus,
) {
    /** The design shows a check only on a completed card. */
    val showsCheckmark: Boolean get() = displayStatus == DisplayStatus.COMPLETED

    /** The design omits the exercise count on a completed card. */
    val showsExerciseCount: Boolean get() = displayStatus != DisplayStatus.COMPLETED
}

sealed interface CalendarIntent {
    data object Refresh : CalendarIntent
    data class ToggleCompletion(val workoutId: String) : CalendarIntent
}

sealed interface CalendarEffect {
    /** A refresh that failed over usable content. A full-screen error is state, not this. */
    data class ShowMessage(val message: String) : CalendarEffect
}
