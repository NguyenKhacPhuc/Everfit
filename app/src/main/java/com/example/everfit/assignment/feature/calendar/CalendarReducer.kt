package com.example.everfit.assignment.feature.calendar

/**
 * Facts produced by work that already happened — never requests.
 *
 * There is deliberately no ToggleCompletion result: Room is the source of truth,
 * so a toggle returns through the cache pipeline as an ordinary [CachedLoaded].
 */
sealed interface CalendarResult {
    data class CachedLoaded(val days: List<DayUiModel>) : CalendarResult
    data object RefreshStarted : CalendarResult
    data object RefreshSucceeded : CalendarResult
    data class RefreshFailed(val message: String) : CalendarResult
}

/**
 * Top-level, so it has no `this` and cannot reach a repository even by accident.
 *
 * No branch clears `days`, so a failed refresh cannot destroy cached content.
 */
// Shape follows my mvi-search sample: a top-level function so it has no `this`
// and cannot read a repository even by accident.
fun reduceCalendar(state: CalendarState, result: CalendarResult): CalendarState =
    when (result) {
        is CalendarResult.CachedLoaded -> state.copy(days = result.days)
        CalendarResult.RefreshStarted -> state.copy(load = Load.Refreshing)
        CalendarResult.RefreshSucceeded -> state.copy(load = Load.Idle)
        is CalendarResult.RefreshFailed -> state.copy(load = Load.Failed(result.message))
    }
