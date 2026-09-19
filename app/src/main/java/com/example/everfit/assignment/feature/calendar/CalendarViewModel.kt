@file:OptIn(ExperimentalCoroutinesApi::class)

package com.example.everfit.assignment.feature.calendar

import com.example.everfit.assignment.core.model.Result
import com.example.everfit.assignment.data.network.ApiError
import com.example.everfit.assignment.core.domain.WeekProvider
import com.example.everfit.assignment.core.domain.WorkoutRepository
import com.example.everfit.assignment.core.mvi.MviViewModel
import com.example.everfit.assignment.core.mvi.flatMapFirst
import com.example.everfit.assignment.feature.mapper.toDayUiModels
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart

/**
 * Pipelines only. Note what is not in scope: any mutable state. This class
 * physically cannot write to the state — it can only send results into the queue.
 */
class CalendarViewModel(
    private val repository: WorkoutRepository,
    weekProvider: WeekProvider,
) : MviViewModel<CalendarIntent, CalendarResult, CalendarState, CalendarEffect>(
    initialState(weekProvider)
) {
    override val reducer: (CalendarState, CalendarResult) -> CalendarState = ::reduceCalendar

    init {
        wireCache()
        wireRefresh()
        wireToggle()
    }

    /**
     * A standing observation of the local store, not a triggered request — so it
     * needs no flattening operator. A toggle returns through here too, which is
     * why it produces no result of its own.
     */
    private fun wireCache() {
        repository.observeWeek()
            .map { assignments ->
                CalendarResult.CachedLoaded(
                    assignments.toDayUiModels(currentState.weekDates, currentState.today)
                )
            }
            .pipeToState()
    }

    /**
     * flatMapFirst: a double tap must not start two refreshes, and must not cancel
     * the first. flatMapLatest would cancel the in-flight one; flatMapConcat would
     * queue a redundant second.
     *
     * The initial load goes through the same operator, so a Retry arriving while
     * the first load is still in flight is dropped rather than duplicated.
     */
    private fun wireRefresh() {
        // Opening the screen is itself a refresh request, so it is emitted as
        // one rather than as a nameless trigger.
        //
        // onStart, not onIntent(Refresh) from init: onIntent does a tryEmit into
        // a SharedFlow that has no collectors yet at construction time, so the
        // initial load would be silently dropped and the app would open blank.
        // Emitting here makes it part of the very flow being collected.
        val refreshRequests = intents
            .filterIsInstance<CalendarIntent.Refresh>()
            .onStart { emit(CalendarIntent.Refresh) }

        refreshRequests
            .flatMapFirst {
                flow {
                    emit(CalendarResult.RefreshStarted)
                    when (val outcome = repository.refresh()) {
                        is Result.Success -> emit(CalendarResult.RefreshSucceeded)
                        is Result.Error -> emit(CalendarResult.RefreshFailed(outcome.userMessage()))
                        Result.Loading -> Unit
                    }
                }
            }
            .pipeToState()
    }

    /**
     * flatMapConcat: two toggles on different workouts are independent facts, and
     * neither may cancel the other. flatMapLatest would silently drop the first —
     * visible only under fast tapping, never in a demo.
     */
    private fun wireToggle() {
        intents.filterIsInstance<CalendarIntent.ToggleCompletion>()
            .flatMapConcat { intent ->
                flow<CalendarResult> { repository.toggleCompletion(intent.workoutId) }
            }
            .pipeToState()
    }

    /**
     * A full-screen error is state; a failed refresh over usable content is a
     * one-shot. Deciding this here, with `after` in hand, is why effectFor takes
     * the resulting state rather than only the result.
     */
    override fun effectFor(
        result: CalendarResult,
        before: CalendarState,
        after: CalendarState,
    ): CalendarEffect? = when {
        result is CalendarResult.RefreshFailed && !after.showsFullScreenError ->
            CalendarEffect.ShowMessage(result.message)

        else -> null
    }
}

/**
 * Seven day rows exist from the very first frame, each with no workouts. That is
 * the brief's loading requirement — correct dates, empty data — satisfied by the
 * initial state rather than by a branch in the screen.
 */
private fun initialState(weekProvider: WeekProvider): CalendarState {
    val week = weekProvider.currentWeek()
    val today = weekProvider.today()
    return CalendarState(
        weekDates = week,
        today = today,
        days = week.map { DayUiModel(date = it, isToday = it == today, workouts = emptyList()) },
        load = Load.Refreshing,
    )
}

private fun Result.Error.userMessage(): String = when {
    message.isNotBlank() -> message
    code == ApiError.NETWORK -> "No connection. Showing saved workouts."
    code == ApiError.SERVER -> "The server is unavailable. Showing saved workouts."
    else -> "Something went wrong. Showing saved workouts."
}
