package com.example.everfit.assignment.feature.calendar

import androidx.compose.runtime.Immutable
import com.example.everfit.assignment.core.model.DisplayStatus
import java.time.LocalDate

@Immutable
data class CalendarState(
    val weekDates: List<LocalDate> = emptyList(),
    val today: LocalDate = LocalDate.MIN,
    val days: List<DayUiModel> = emptyList(),
    val load: Load = Load.Idle,
) {
    val showsFullScreenError: Boolean
        get() = load is Load.Failed && days.all { it.workouts.isEmpty() }

    val isRefreshing: Boolean get() = load is Load.Refreshing

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
}

sealed interface CalendarIntent {
    data object Refresh : CalendarIntent
    data class ToggleCompletion(val workoutId: String) : CalendarIntent
}

sealed interface CalendarEffect {
    data class ShowMessage(val message: String) : CalendarEffect
}
