package com.example.everfit.assignment.ui

import app.cash.turbine.test
import com.example.everfit.assignment.MainDispatcherRule
import com.example.everfit.assignment.core.Result
import com.example.everfit.assignment.data.network.ApiError
import com.example.everfit.assignment.core.domain.WeekProvider
import com.example.everfit.assignment.core.domain.WorkoutRepository
import com.example.everfit.assignment.core.model.StoredStatus
import com.example.everfit.assignment.core.model.WorkoutAssignment
import com.example.everfit.assignment.ui.calendar.CalendarEffect
import com.example.everfit.assignment.ui.calendar.CalendarIntent
import com.example.everfit.assignment.ui.calendar.CalendarViewModel
import com.example.everfit.assignment.ui.calendar.Load
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * TIER 2 — only what a pure reducer cannot reach: pipeline behaviour.
 * Everything about transitions lives in CalendarReducerTest.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CalendarViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private class FakeRepository : WorkoutRepository {
        val cache = MutableStateFlow<List<WorkoutAssignment>>(emptyList())
        var refreshResult: Result<Unit> = Result.Success(Unit)
        var refreshCalls = 0
        var toggles = mutableListOf<String>()
        var gate: CompletableDeferred<Unit>? = null

        override fun observeWeek(): Flow<List<WorkoutAssignment>> = cache

        override suspend fun refresh(): Result<Unit> {
            refreshCalls++
            gate?.await()
            return refreshResult
        }

        override suspend fun toggleCompletion(assignmentId: String) {
            toggles += assignmentId
        }
    }

    private val fixedClock = Clock.fixed(
        LocalDate.parse("2026-09-19").atStartOfDay(ZoneId.of("UTC")).toInstant(),
        ZoneId.of("UTC"),
    )

    private fun viewModel(repo: FakeRepository) =
        CalendarViewModel(repo, WeekProvider(fixedClock))

    @Test
    fun `week dates are present in the initial state, before anything loads`() = runTest {
        val vm = viewModel(FakeRepository())

        val initial = vm.state.value
        assertEquals(7, initial.weekDates.size)
        assertEquals(LocalDate.parse("2026-09-14"), initial.weekDates.first())
        assertEquals(LocalDate.parse("2026-09-19"), initial.today)
    }

    /**
     * Rung 7.1. The first frame previously said Load.Idle while a refresh was
     * about to start, so "never loaded" looked identical to "nothing scheduled".
     */
    @Test
    fun `the first frame reports loading, not idle`() = runTest {
        val vm = viewModel(FakeRepository().apply { gate = CompletableDeferred() })

        assertIs<Load.Refreshing>(vm.state.value.load)
        assertTrue(vm.state.value.showsLoadingPlaceholders)
    }

    /**
     * flatMapFirst: a double tap must not start two refreshes, and must not
     * cancel the first. flatMapLatest would cancel; flatMapConcat would queue.
     */
    @Test
    fun `a second refresh while one is in flight is dropped, not queued`() = runTest {
        val repo = FakeRepository().apply { gate = CompletableDeferred() }
        val vm = viewModel(repo)
        advanceUntilIdle()

        vm.onIntent(CalendarIntent.Refresh)
        vm.onIntent(CalendarIntent.Refresh)
        advanceUntilIdle()

        assertEquals(1, repo.refreshCalls, "only the in-flight refresh should exist")

        repo.gate!!.complete(Unit)
        advanceUntilIdle()
        assertEquals(1, repo.refreshCalls, "the dropped intent must not run afterwards")
    }

    /** flatMapConcat: two toggles are independent facts; neither may cancel the other. */
    @Test
    fun `two rapid toggles both reach the repository`() = runTest {
        val repo = FakeRepository()
        val vm = viewModel(repo)
        advanceUntilIdle()

        vm.onIntent(CalendarIntent.ToggleCompletion("first"))
        vm.onIntent(CalendarIntent.ToggleCompletion("second"))
        advanceUntilIdle()

        assertEquals(listOf("first", "second"), repo.toggles)
    }

    @Test
    fun `cached content reaches state without an explicit refresh`() = runTest {
        val repo = FakeRepository()
        repo.cache.value = listOf(
            WorkoutAssignment("a", dayIndex = 0, title = "Legs day", storedStatus = StoredStatus.MISSED, totalExercises = 5),
        )
        val vm = viewModel(repo)
        advanceUntilIdle()

        val monday = vm.state.value.days.first()
        assertEquals("Legs day", monday.workouts.single().title)
    }

    @Test
    fun `a failed refresh over existing content emits a message rather than a full-screen error`() = runTest {
        val repo = FakeRepository()
        repo.cache.value = listOf(
            WorkoutAssignment("a", 0, "Legs day", StoredStatus.MISSED, 5),
        )
        repo.refreshResult = Result.Error(ApiError.NETWORK, "offline")
        val vm = viewModel(repo)

        vm.effects.test {
            advanceUntilIdle()
            assertIs<CalendarEffect.ShowMessage>(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }

        assertIs<Load.Failed>(vm.state.value.load)
        assertTrue(vm.state.value.days.any { it.workouts.isNotEmpty() })
        assertEquals(false, vm.state.value.showsFullScreenError)
    }

    @Test
    fun `a failed refresh with nothing cached is a full-screen error and emits no message`() = runTest {
        val repo = FakeRepository().apply { refreshResult = Result.Error(ApiError.NETWORK, "offline") }
        val vm = viewModel(repo)
        advanceUntilIdle()

        assertTrue(vm.state.value.showsFullScreenError)
        vm.effects.test { expectNoEvents() }
    }
}
