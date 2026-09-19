package com.example.everfit.assignment.ui.calendar

/**
 * Facts produced by work that already happened — never requests.
 *
 * Note there is no ToggleCompletion result. Room is the source of truth, so a
 * toggle writes and returns through the cache pipeline as an ordinary
 * [CalendarResult.CachedLoaded]. Adding one would mean also adding a failure
 * result to roll back, and a reducer able to tell an optimistic value from a
 * confirmed one — cost with no benefit against a local database.
 */
sealed interface CalendarResult {
    data class CachedLoaded(val days: List<DayUiModel>) : CalendarResult
    data object RefreshStarted : CalendarResult
    data object RefreshSucceeded : CalendarResult
    data class RefreshFailed(val message: String) : CalendarResult
}

/**
 * THE SPEC — the whole screen in one screenful.
 *
 * A top-level function, so it has no `this`: it cannot read the repository or a
 * `var` even by accident. Purity is enforced by scope rather than discipline,
 * which holds whether or not anyone writes a test.
 *
 * Note that no branch clears `days`. Rung 3.4 — a failed refresh must not destroy
 * cached content — therefore holds by construction rather than by remembering.
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
