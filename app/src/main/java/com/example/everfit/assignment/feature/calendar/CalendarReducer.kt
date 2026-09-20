package com.example.everfit.assignment.feature.calendar

sealed interface CalendarResult {
    data class CachedLoaded(val days: List<DayUiModel>) : CalendarResult
    data object RefreshStarted : CalendarResult
    data object RefreshSucceeded : CalendarResult
    data class RefreshFailed(val message: String) : CalendarResult
}

fun reduceCalendar(state: CalendarState, result: CalendarResult): CalendarState =
    when (result) {
        is CalendarResult.CachedLoaded -> state.copy(days = result.days)
        CalendarResult.RefreshStarted -> state.copy(load = Load.Refreshing)
        CalendarResult.RefreshSucceeded -> state.copy(load = Load.Idle)
        is CalendarResult.RefreshFailed -> state.copy(load = Load.Failed(result.message))
    }
