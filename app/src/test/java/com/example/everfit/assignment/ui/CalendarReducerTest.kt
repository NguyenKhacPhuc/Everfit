package com.example.everfit.assignment.ui

import com.example.everfit.assignment.model.DisplayStatus
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

    // ── Intent 07: telling "still loading" apart from "nothing scheduled" ──

    @Test
    fun `placeholders show while loading with nothing yet loaded`() {
        val blank = week.map { day(it) }

        val after = reduceCalendar(state(days = blank), CalendarResult.RefreshStarted)

        assertTrue(after.showsLoadingPlaceholders)
    }

    /** Rung 7.4: a background refresh must not replace content with placeholders. */
    @Test
    fun `a refresh over existing content shows no placeholders`() {
        val content = listOf(day(monday, workout("a"))) + week.drop(1).map { day(it) }

        val after = reduceCalendar(state(days = content), CalendarResult.RefreshStarted)

        assertFalse(after.showsLoadingPlaceholders, "content is already on screen")
    }

    /** Rung 7.5: a week that genuinely has no workouts is not a loading week. */
    @Test
    fun `an empty week that finished loading shows no placeholders`() {
        val blank = week.map { day(it) }

        val after = reduceCalendar(
            state(days = blank, load = Load.Refreshing),
            CalendarResult.RefreshSucceeded,
        )

        assertFalse(after.showsLoadingPlaceholders)
    }

    @Test
    fun `a failure with nothing loaded shows the error, not placeholders`() {
        val blank = week.map { day(it) }

        val after = reduceCalendar(
            state(days = blank, load = Load.Refreshing),
            CalendarResult.RefreshFailed("offline"),
        )

        assertFalse(after.showsLoadingPlaceholders)
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
