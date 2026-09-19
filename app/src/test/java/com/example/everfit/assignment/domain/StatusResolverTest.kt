package com.example.everfit.assignment.domain

import com.example.everfit.assignment.core.domain.resolveCompletion
import com.example.everfit.assignment.core.domain.resolveDisplay
import com.example.everfit.assignment.core.model.DayPosition
import com.example.everfit.assignment.core.model.DisplayStatus
import com.example.everfit.assignment.core.model.StoredStatus
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * TIER 1. Two steps, tested separately, because collapsing them is the mistake
 * this file exists to prevent.
 */
class StatusResolverTest {

    // ── step 0: the raw mapping ───────────────────────────────────────────
    //
    // Confirmed against docs/design/training.png, NOT guessed. The fixture's
    // status=1 items render as "Missed" and its status=2 item as "Completed",
    // which is the reverse of the intuitive ordering.

    @Test
    fun `raw status values map as the design shows, not as intuition suggests`() {
        assertEquals(StoredStatus.ASSIGNED, StoredStatus.fromRaw(0))
        assertEquals(StoredStatus.MISSED, StoredStatus.fromRaw(1))
        assertEquals(StoredStatus.COMPLETED, StoredStatus.fromRaw(2))
    }

    @Test
    fun `an unrecognised status is UNKNOWN rather than a crash or a wrong guess`() {
        assertEquals(StoredStatus.UNKNOWN, StoredStatus.fromRaw(7))
        assertEquals(StoredStatus.UNKNOWN, StoredStatus.fromRaw(-1))
    }

    // ── step 1: completion resolution, override wins ──────────────────────

    @Test
    fun `a local override wins over any stored status`() {
        StoredStatus.entries.forEach { stored ->
            assertEquals(true, resolveCompletion(override = true, stored = stored))
            assertEquals(false, resolveCompletion(override = false, stored = stored))
        }
    }

    @Test
    fun `without an override completion comes from the stored status`() {
        assertEquals(true, resolveCompletion(null, StoredStatus.COMPLETED))
        assertEquals(false, resolveCompletion(null, StoredStatus.ASSIGNED))
        assertEquals(false, resolveCompletion(null, StoredStatus.MISSED))
        assertEquals(false, resolveCompletion(null, StoredStatus.UNKNOWN))
    }

    // ── step 2: display derivation ────────────────────────────────────────

    @Test
    fun `a past day is completed or missed`() {
        assertEquals(DisplayStatus.COMPLETED, resolveDisplay(DayPosition.PAST, true))
        assertEquals(DisplayStatus.MISSED, resolveDisplay(DayPosition.PAST, false))
    }

    @Test
    fun `today is completed or assigned, never missed`() {
        assertEquals(DisplayStatus.COMPLETED, resolveDisplay(DayPosition.TODAY, true))
        assertEquals(DisplayStatus.ASSIGNED, resolveDisplay(DayPosition.TODAY, false))
    }

    /**
     * The rows that matter. A future day greys out EVEN WHEN MARKED COMPLETE.
     * A single-step implementation passes every other case and fails here.
     */
    @Test
    fun `a future day is upcoming regardless of completion`() {
        assertEquals(DisplayStatus.UPCOMING, resolveDisplay(DayPosition.FUTURE, false))
        assertEquals(DisplayStatus.UPCOMING, resolveDisplay(DayPosition.FUTURE, true))
    }

    // ── the fixture, end to end, against what the design renders ──────────

    @Test
    fun `the fixture reproduces exactly what the design shows`() {
        // Design: today is Friday. Mon/Thu "Missed", Tue "Completed",
        // Fri a plain count, Sun greyed.
        fun display(raw: Int, position: DayPosition) =
            resolveDisplay(position, resolveCompletion(null, StoredStatus.fromRaw(raw)))

        assertEquals(DisplayStatus.MISSED, display(1, DayPosition.PAST))     // Mon
        assertEquals(DisplayStatus.COMPLETED, display(2, DayPosition.PAST))  // Tue
        assertEquals(DisplayStatus.MISSED, display(1, DayPosition.PAST))     // Thu
        assertEquals(DisplayStatus.ASSIGNED, display(0, DayPosition.TODAY))  // Fri
        assertEquals(DisplayStatus.UPCOMING, display(0, DayPosition.FUTURE)) // Sun
    }
}
