package com.example.everfit.assignment.core.domain

import java.time.Clock
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

/**
 * Maps the API's `day: 0..6` onto real dates — the payload carries none.
 *
 * [clock] is injected so "today" is testable, and it carries the zone, which is
 * what makes [today] a local-calendar answer rather than a UTC one.
 */
// Date math drafted with Claude. I chose previousOrSame(MONDAY) over
// WeekFields.of(locale) after it flagged that a locale-aware week start yields
// Sunday-first in some locales, and added the timezone and DST cases myself.
class WeekProvider(private val clock: Clock) {

    fun today(): LocalDate = LocalDate.now(clock)

    /**
     * Monday…Sunday of the week containing [today].
     *
     * `previousOrSame(MONDAY)`, never `WeekFields.of(locale)` — a locale-derived
     * week start is Sunday-first in some locales, which shows the wrong week one
     * day in seven.
     */
    fun currentWeek(): List<LocalDate> {
        val monday = today().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        return List(DAYS_IN_WEEK) { offset -> monday.plusDays(offset.toLong()) }
    }

    fun dateForDayIndex(dayIndex: Int): LocalDate {
        require(dayIndex in 0 until DAYS_IN_WEEK) {
            "day index must be 0..6 (Mon..Sun), was $dayIndex"
        }
        return currentWeek()[dayIndex]
    }

    private companion object {
        const val DAYS_IN_WEEK = 7
    }
}
