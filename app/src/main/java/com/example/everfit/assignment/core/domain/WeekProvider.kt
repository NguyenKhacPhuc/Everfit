package com.example.everfit.assignment.core.domain

import java.time.Clock
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

/**
 * Maps the API's `day: 0..6` onto real calendar dates.
 *
 * The payload carries no dates whatsoever, so this class is the only thing tying a
 * workout to a day the user recognises.
 *
 * [clock] is injected rather than read from the system. Without that, "today" is
 * untestable and the highlight can only be checked by changing the device date —
 * and the clock carries the zone, which is what makes [today] a local-calendar
 * answer rather than a UTC one.
 */
// Date math drafted with Claude. I chose previousOrSame(MONDAY) over
// WeekFields.of(locale) after it flagged that a locale-aware week start yields
// Sunday-first in some locales, and added the timezone and DST cases myself.
class WeekProvider(private val clock: Clock) {

    /** The local date, in the clock's zone. */
    fun today(): LocalDate = LocalDate.now(clock)

    /**
     * Monday…Sunday of the week containing [today], always exactly seven
     * consecutive dates.
     *
     * Uses `previousOrSame(MONDAY)` deliberately, never `WeekFields.of(locale)`:
     * a locale-derived week start is correct general behaviour and wrong here,
     * because the brief mandates Monday–Sunday. In locales that start on Sunday
     * the locale-aware version shows the wrong week one day in seven.
     */
    fun currentWeek(): List<LocalDate> {
        val monday = today().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        return List(DAYS_IN_WEEK) { offset -> monday.plusDays(offset.toLong()) }
    }

    /** [dayIndex] is the API's `day` field: 0 = Monday … 6 = Sunday. */
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
