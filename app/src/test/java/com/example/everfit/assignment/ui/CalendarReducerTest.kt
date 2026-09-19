package com.example.everfit.assignment.ui

import com.example.everfit.assignment.domain.model.DisplayStatus
import com.example.everfit.assignment.ui.calendar.CalendarResult
import com.example.everfit.assignment.ui.calendar.CalendarState
import com.example.everfit.assignment.ui.calendar.DayUiModel
import com.example.everfit.assignment.ui.calendar.Load
import com.example.everfit.assignment.ui.calendar.WorkoutUiModel
import com.example.everfit.assignment.ui.calendar.reduceCalendar
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * TIER 1 — transitions. Plain function calls: no dispatcher, no fake, no runTest.
 * Most rules live here, which is the practical payoff of a pure reducer.
 */
class CalendarReducerTest {

    private val monday = LocalDate.parse("2026-09-14")
    private val week = List(7) { monday.plusDays(it.toLong()) }

    private fun workout(id: String, status: DisplayStatus = DisplayStatus.ASSIGNED) =
        WorkoutUiModel(id, "Workout $id", 5, status)

    private fun day(date: LocalDate, vararg workouts: WorkoutUiModel) =
        DayUiModel(date, isToday = false, workouts = workouts.toList())

    private fun state(
        days: List<DayUiModel> = emptyList(),
        load: Load = Load.Idle,
    ) = CalendarState(weekDates = week, today = week[5], days = days, load = load)

    @Test
    fun `cached content replaces days and leaves load alone`() {
        val after = reduceCalendar(
            state(load = Load.Refreshing),
            CalendarResult.CachedLoaded(listOf(day(monday, workout("a")))),
        )

        assertEquals(listOf("a"), after.days.flatMap { it.workouts }.map { it.id })
        assertEquals(Load.Refreshing, after.load)
    }

    @Test
    fun `refresh started moves load to refreshing`() {
        assertEquals(Load.Refreshing, reduceCalendar(state(), CalendarResult.RefreshStarted).load)
    }

    /** Rung 3.4 as a transition: no branch clears content on success or failure. */
    @Test
    fun `a successful refresh settles load and does not touch days`() {
        val content = listOf(day(monday, workout("a")))
        val after = reduceCalendar(
            state(days = content, load = Load.Refreshing),
            CalendarResult.RefreshSucceeded,
        )

        assertEquals(Load.Idle, after.load)
        assertEquals(content, after.days)
    }

    @Test
    fun `a failed refresh records the error and does not touch days`() {
        val content = listOf(day(monday, workout("a")))
        val after = reduceCalendar(
            state(days = content, load = Load.Refreshing),
            CalendarResult.RefreshFailed("offline"),
        )

        assertEquals(Load.Failed("offline"), after.load)
        assertEquals(content, after.days, "cached content must survive a failed refresh")
    }

    @Test
    fun `an error over existing content is not a full-screen error`() {
        val after = reduceCalendar(
            state(days = listOf(day(monday, workout("a"))), load = Load.Refreshing),
            CalendarResult.RefreshFailed("offline"),
        )

        assertFalse(after.showsFullScreenError)
    }

    @Test
    fun `an error with nothing cached is a full-screen error`() {
        val empty = week.map { day(it) }
        val after = reduceCalendar(
            state(days = empty, load = Load.Refreshing),
            CalendarResult.RefreshFailed("offline"),
        )

        assertTrue(after.showsFullScreenError)
    }

    @Test
    fun `week dates never change after construction`() {
        val results = listOf(
            CalendarResult.RefreshStarted,
            CalendarResult.RefreshSucceeded,
            CalendarResult.RefreshFailed("x"),
            CalendarResult.CachedLoaded(emptyList()),
        )

        results.fold(state()) { acc, result -> reduceCalendar(acc, result) }
            .let { assertEquals(week, it.weekDates) }
    }
}
