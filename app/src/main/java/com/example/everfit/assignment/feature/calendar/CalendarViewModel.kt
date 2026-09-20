@file:OptIn(ExperimentalCoroutinesApi::class)

package com.example.everfit.assignment.feature.calendar

import com.example.everfit.assignment.core.model.Result
import com.example.everfit.assignment.data.network.ApiError
import com.example.everfit.assignment.core.domain.WeekProvider
import com.example.everfit.assignment.core.domain.WorkoutRepository
import com.example.everfit.assignment.core.mvi.MviViewModel
import com.example.everfit.assignment.core.mvi.flatMapFirst
import com.example.everfit.assignment.feature.calendar.mapper.toDayUiModels
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart

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

    private fun wireToggle() {
        intents.filterIsInstance<CalendarIntent.ToggleCompletion>()
            .flatMapConcat { intent ->
                flow<CalendarResult> { repository.toggleCompletion(intent.workoutId) }
            }
            .pipeToState()
    }

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
