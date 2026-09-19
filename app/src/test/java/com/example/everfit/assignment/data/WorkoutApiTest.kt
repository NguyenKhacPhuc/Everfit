package com.example.everfit.assignment.data

import com.example.everfit.assignment.data.remote.WorkoutApi
import com.example.everfit.assignment.domain.model.StoredStatus
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * TIER 1 via Ktor's MockEngine — no network, no live endpoint.
 *
 * The point of these cases is that nothing above `data` ever sees a Ktor or
 * serialization exception: every failure arrives as a typed DataError.
 */
class WorkoutApiTest {

    private fun fixture(): String = javaClass.classLoader!!
        .getResourceAsStream("workouts.json")!!
        .bufferedReader().readText()

    private fun apiReturning(handler: MockEngine.Companion.() -> MockEngine): WorkoutApi {
        val client = HttpClient(MockEngine.handler()) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }
        return WorkoutApi(client, BASE_URL)
    }

    private fun apiRespondingWith(body: String) = apiReturning {
        MockEngine {
            respond(body, HttpStatusCode.OK, headersOf("Content-Type", ContentType.Application.Json.toString()))
        }
    }

    @Test
    fun `a successful response becomes domain assignments`() = runTest {
        val result = apiRespondingWith(fixture()).fetchWorkouts()

        val assignments = result.getOrThrow()
        assertEquals(6, assignments.size)
        assertTrue(assignments.all { it.dayIndex in 0..6 })
    }

    @Test
    fun `raw status is mapped to the named status on the way through`() = runTest {
        val assignments = apiRespondingWith(fixture()).fetchWorkouts().getOrThrow()
        val legsDayMonday = assignments.first { it.dayIndex == 0 }

        assertEquals("Legs day", legsDayMonday.title)
        assertEquals(StoredStatus.MISSED, legsDayMonday.storedStatus)
        assertEquals(5, legsDayMonday.totalExercises)
    }

    @Test
    fun `day index survives mapping, including a day holding two workouts`() = runTest {
        val assignments = apiRespondingWith(fixture()).fetchWorkouts().getOrThrow()

        assertEquals(2, assignments.count { it.dayIndex == 4 })
        assertEquals(0, assignments.count { it.dayIndex == 2 })
    }

    @Test
    fun `a non-2xx response becomes DataError Server`() = runTest {
        val api = apiReturning { MockEngine { respondError(HttpStatusCode.InternalServerError) } }

        assertIs<DataError.Server>(api.fetchWorkouts().exceptionOrNull().asDataError())
    }

    @Test
    fun `a malformed body becomes DataError Parsing, not a raw exception`() = runTest {
        val api = apiRespondingWith("""{"data":[{"day":"not a number"}]}""")

        assertIs<DataError.Parsing>(api.fetchWorkouts().exceptionOrNull().asDataError())
    }

    @Test
    fun `a transport failure becomes DataError Network`() = runTest {
        val api = apiReturning { MockEngine { throw IOException("no connectivity") } }

        assertIs<DataError.Network>(api.fetchWorkouts().exceptionOrNull().asDataError())
    }

    private fun Throwable?.asDataError(): DataError =
        this as? DataError ?: error("expected a DataError, got $this")

    private companion object {
        const val BASE_URL = "https://mock.internalef.com/workouts"
    }
}
