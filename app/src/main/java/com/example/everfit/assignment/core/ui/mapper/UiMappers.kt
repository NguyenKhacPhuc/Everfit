package com.example.everfit.assignment.ui.mapper

import com.example.everfit.assignment.core.model.DayPosition
import com.example.everfit.assignment.core.model.WorkoutAssignment
import com.example.everfit.assignment.core.domain.resolveDisplay
import com.example.everfit.assignment.ui.calendar.DayUiModel
import com.example.everfit.assignment.ui.calendar.WorkoutUiModel
import java.time.LocalDate

/**
 * Domain -> UI.
 *
 * DisplayStatus is computed here rather than stored on the domain model, because
 * it depends on *today*: the same assignment means different things on different
 * days, so baking it in would make the model mean different things over time.
 */
fun List<WorkoutAssignment>.toDayUiModels(
    weekDates: List<LocalDate>,
    today: LocalDate,
): List<DayUiModel> {
    val byDay = groupBy { it.dayIndex }
    return weekDates.mapIndexed { dayIndex, date ->
        val position = positionOf(date, today)
        DayUiModel(
            date = date,
            isToday = position == DayPosition.TODAY,
            workouts = byDay[dayIndex].orEmpty().map { assignment ->
                WorkoutUiModel(
                    id = assignment.id,
                    title = assignment.title,
                    totalExercises = assignment.totalExercises,
                    displayStatus = resolveDisplay(position, assignment.isCompleted),
                )
            },
        )
    }
}

fun positionOf(date: LocalDate, today: LocalDate): DayPosition = when {
    date.isBefore(today) -> DayPosition.PAST
    date.isAfter(today) -> DayPosition.FUTURE
    else -> DayPosition.TODAY
}
