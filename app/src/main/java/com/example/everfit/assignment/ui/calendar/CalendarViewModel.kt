@file:OptIn(ExperimentalCoroutinesApi::class)

package com.example.everfit.assignment.ui.calendar

import com.example.everfit.assignment.data.base.Result
import com.example.everfit.assignment.data.network.ApiError
import com.example.everfit.assignment.domain.WeekProvider
import com.example.everfit.assignment.domain.WorkoutRepository
import com.example.everfit.assignment.mvi.MviViewModel
import com.example.everfit.assignment.mvi.flatMapFirst
import com.example.everfit.assignment.ui.mapper.toDayUiModels
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge

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
     * The initial load is merged in as a trigger rather than emitted as an intent,
     * so it cannot be lost to a race between construction and subscription.
     */
    private fun wireRefresh() {
        val triggers = merge(
            flowOf(Unit),
            intents.filterIsInstance<CalendarIntent.Refresh>().map { },
        )

        triggers
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
    )
}

private fun Result.Error.userMessage(): String = when {
    message.isNotBlank() -> message
    code == ApiError.NETWORK -> "No connection. Showing saved workouts."
    code == ApiError.SERVER -> "The server is unavailable. Showing saved workouts."
    else -> "Something went wrong. Showing saved workouts."
}
