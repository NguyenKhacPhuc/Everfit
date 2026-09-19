package com.example.everfit.assignment.feature.calendar

import androidx.compose.runtime.Immutable
import com.example.everfit.assignment.core.model.DisplayStatus
import java.time.LocalDate

/**
 * `days` and `load` are separate fields: a failed refresh over good cache is both
 * content-bearing and errored, which one collapsed hierarchy cannot express.
 */
@Immutable
data class CalendarState(
    val weekDates: List<LocalDate> = emptyList(),
    val today: LocalDate = LocalDate.MIN,
    val days: List<DayUiModel> = emptyList(),
    val load: Load = Load.Idle,
) {
    /** Derived, never stored. */
    val showsFullScreenError: Boolean
        get() = load is Load.Failed && days.all { it.workouts.isEmpty() }

    val isRefreshing: Boolean get() = load is Load.Refreshing

    /**
     * Cold start only. Derived, so a background refresh over content is false
     * (days populated) and a genuinely empty week is false (load settled).
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

    val showsCheckmark: Boolean get() = displayStatus == DisplayStatus.COMPLETED


    val showsExerciseCount: Boolean get() = displayStatus != DisplayStatus.COMPLETED
}

sealed interface CalendarIntent {
    data object Refresh : CalendarIntent
    data class ToggleCompletion(val workoutId: String) : CalendarIntent
}

sealed interface CalendarEffect {
    /** A failed refresh over usable content. A full-screen error is state. */
    data class ShowMessage(val message: String) : CalendarEffect
}
