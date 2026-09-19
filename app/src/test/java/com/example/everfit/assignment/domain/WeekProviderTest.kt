package com.example.everfit.assignment.domain

import java.time.Clock
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * TIER 1 — pure. No dispatcher, no fake, no runTest.
 *
 * The API carries no dates at all, only `day: 0..6`, so this mapping is the only
 * thing tying the payload to a real calendar. Cases transcribed from
 * docs/sdlc/testing.md.
 */
class WeekProviderTest {

    private fun providerAt(date: String, zone: String = "Asia/Ho_Chi_Minh"): WeekProvider {
        val zoneId = ZoneId.of(zone)
        val instant = LocalDate.parse(date).atTime(12, 0).atZone(zoneId).toInstant()
        return WeekProvider(Clock.fixed(instant, zoneId))
    }

    @Test
    fun `mid-week date yields the Monday to Sunday week containing it`() {
        val week = providerAt("2026-09-19").currentWeek() // a Saturday

        assertEquals(LocalDate.parse("2026-09-14"), week.first())
        assertEquals(LocalDate.parse("2026-09-20"), week.last())
    }

    @Test
    fun `a Monday is index 0 of its own week`() {
        val week = providerAt("2026-09-21").currentWeek()

        assertEquals(LocalDate.parse("2026-09-21"), week[0])
        assertEquals(DayOfWeek.MONDAY, week[0].dayOfWeek)
    }

    /**
     * The case that matters. Many week calculations treat Sunday as the FIRST day,
     * which silently shows the wrong week one day in seven.
     */
    @Test
    fun `a Sunday belongs to the week that precedes it, not the one that follows`() {
        val week = providerAt("2026-09-20").currentWeek()

        assertEquals(LocalDate.parse("2026-09-14"), week.first())
        assertEquals(LocalDate.parse("2026-09-20"), week.last())
    }

    @Test
    fun `a week spanning a month boundary stays consecutive`() {
        val week = providerAt("2026-09-28").currentWeek()

        assertEquals(LocalDate.parse("2026-09-28"), week.first())
        assertEquals(LocalDate.parse("2026-10-04"), week.last())
    }

    @Test
    fun `a week spanning a year boundary stays consecutive`() {
        val week = providerAt("2025-12-29").currentWeek()

        assertEquals(LocalDate.parse("2025-12-29"), week.first())
        assertEquals(LocalDate.parse("2026-01-04"), week.last())
    }

    @Test
    fun `a week containing a DST transition still has seven days`() {
        // Europe/London springs forward on Sunday 2026-03-29.
        val week = providerAt("2026-03-25", zone = "Europe/London").currentWeek()

        assertEquals(7, week.size)
        assertEquals(LocalDate.parse("2026-03-23"), week.first())
        assertEquals(LocalDate.parse("2026-03-29"), week.last())
    }

    /**
     * "Today" is a local-calendar concept. The same instant can fall in different
     * local weeks, so deriving it from a UTC instant puts the highlight on the
     * wrong cell near midnight.
     */
    @Test
    fun `the same instant can fall in different weeks in different zones`() {
        // 2026-09-21 00:30 in Asia/Ho_Chi_Minh is still 2026-09-20 in UTC.
        val instant = Instant.parse("2026-09-20T17:30:00Z")

        val saigon = WeekProvider(Clock.fixed(instant, ZoneId.of("Asia/Ho_Chi_Minh")))
        val london = WeekProvider(Clock.fixed(instant, ZoneId.of("Europe/London")))

        assertEquals(LocalDate.parse("2026-09-21"), saigon.today())
        assertEquals(LocalDate.parse("2026-09-20"), london.today())
        assertEquals(LocalDate.parse("2026-09-21"), saigon.currentWeek().first())
        assertEquals(LocalDate.parse("2026-09-14"), london.currentWeek().first())
    }

    @Test
    fun `a week is always seven strictly consecutive days starting on Monday`() {
        val week = providerAt("2026-09-19").currentWeek()

        assertEquals(7, week.size)
        assertEquals(DayOfWeek.MONDAY, week.first().dayOfWeek)
        assertEquals(DayOfWeek.SUNDAY, week.last().dayOfWeek)
        week.zipWithNext { a, b -> assertEquals(b, a.plusDays(1)) }
    }

    // ── rung 1.2: day index -> date ────────────────────────────────────────

    @Test
    fun `api day index 0 is Monday and 6 is Sunday`() {
        val provider = providerAt("2026-09-19")

        assertEquals(LocalDate.parse("2026-09-14"), provider.dateForDayIndex(0))
        assertEquals(LocalDate.parse("2026-09-20"), provider.dateForDayIndex(6))
    }

    @Test
    fun `today reports the local date, not the instant`() {
        assertEquals(LocalDate.parse("2026-09-19"), providerAt("2026-09-19").today())
    }
}
