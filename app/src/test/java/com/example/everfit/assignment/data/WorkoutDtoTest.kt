package com.example.everfit.assignment.data

import com.example.everfit.assignment.data.remote.dto.WorkoutsResponseDto
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * TIER 1. Asserted against the committed capture of the real endpoint, never the
 * live one — a third party being down must not turn this suite red.
 */
class WorkoutDtoTest {

    private val json = Json { ignoreUnknownKeys = true }

    private fun fixture(): String = javaClass.classLoader!!
        .getResourceAsStream("workouts.json")!!
        .bufferedReader().readText()

    private fun parsed() = json.decodeFromString<WorkoutsResponseDto>(fixture())

    @Test
    fun `the fixture parses into seven days indexed zero through six`() {
        val days = parsed().data

        assertEquals(7, days.size)
        assertEquals((0..6).toList(), days.map { it.day })
    }

    @Test
    fun `wire names are mapped to Kotlin names`() {
        val first = parsed().data.first().assignments.single()

        assertEquals("68c0a1f45b9d4a0017c8e200", first.id)
        assertEquals("Legs day", first.title)
        assertEquals(5, first.totalExercise)
        assertEquals(1, first.status)
    }

    /** The fixture exercises both awkward shapes without inventing data. */
    @Test
    fun `days may hold zero or several assignments`() {
        val byDay = parsed().data.associateBy { it.day }

        assertTrue(byDay.getValue(2).assignments.isEmpty(), "day 2 should be empty")
        assertTrue(byDay.getValue(5).assignments.isEmpty(), "day 5 should be empty")
        assertEquals(2, byDay.getValue(4).assignments.size, "day 4 should hold two")
    }

    @Test
    fun `the fixture holds six assignments in total`() {
        assertEquals(6, parsed().data.sumOf { it.assignments.size })
    }

    /** A new server field must not be fatal. */
    @Test
    fun `unknown fields are ignored rather than fatal`() {
        val withExtra = """
            {"data":[{"_id":"a","day":0,"surprise":true,
              "assignments":[{"_id":"b","title":"T","status":0,
                              "total_exercise":3,"another":"x"}]}]}
        """.trimIndent()

        val day = json.decodeFromString<WorkoutsResponseDto>(withExtra).data.single()

        assertEquals("T", day.assignments.single().title)
    }

    @Test
    fun `a malformed body fails rather than producing empty data`() {
        assertFailsWith<Exception> {
            json.decodeFromString<WorkoutsResponseDto>("""{"data":[{"day":"not a number"}]}""")
        }
    }
}
