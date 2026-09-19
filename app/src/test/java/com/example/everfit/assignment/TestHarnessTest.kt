package com.example.everfit.assignment

import app.cash.turbine.test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Rung 0.4. Proves the harness itself works, so a later failure is a failure of the
 * code under test rather than of the machinery.
 *
 * Each test here exercises one piece that fails confusingly when missing.
 */
class TestHarnessTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `coroutines test machinery runs`() = runTest {
        val result = withContext(Dispatchers.Default) { 21 * 2 }
        assertEquals(42, result)
    }

    @Test
    fun `Dispatchers Main is installed by the rule`() = runTest {
        // Without MainDispatcherRule this throws IllegalStateException.
        val onMain = withContext(Dispatchers.Main) { "ok" }
        assertEquals("ok", onMain)
    }

    @Test
    fun `turbine observes a flow`() = runTest {
        flowOf(1, 2, 3).test {
            assertEquals(1, awaitItem())
            assertEquals(2, awaitItem())
            assertEquals(3, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `api fixture is on the test classpath`() {
        val json = javaClass.classLoader!!
            .getResourceAsStream("workouts.json")!!
            .bufferedReader().readText()

        assertTrue(json.contains("\"day\""), "fixture should contain day entries")
        assertTrue(json.contains("Legs day"), "fixture should contain known workout titles")
    }
}
