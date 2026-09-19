package com.example.everfit.assignment.data

import com.example.everfit.assignment.data.base.Result
import com.example.everfit.assignment.data.network.ApiError
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
import java.net.UnknownHostException
import kotlin.test.Test
import kotlin.test.assertEquals
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

    private suspend fun WorkoutApi.assignments() =
        (fetchWorkouts() as Result.Success).data

    private suspend fun WorkoutApi.error() = fetchWorkouts() as Result.Error

    @Test
    fun `a successful response becomes domain assignments`() = runTest {
        val assignments = apiRespondingWith(fixture()).assignments()
        assertEquals(6, assignments.size)
        assertTrue(assignments.all { it.dayIndex in 0..6 })
    }

    @Test
    fun `raw status is mapped to the named status on the way through`() = runTest {
        val assignments = apiRespondingWith(fixture()).assignments()
        val legsDayMonday = assignments.first { it.dayIndex == 0 }

        assertEquals("Legs day", legsDayMonday.title)
        assertEquals(StoredStatus.MISSED, legsDayMonday.storedStatus)
        assertEquals(5, legsDayMonday.totalExercises)
    }

    @Test
    fun `day index survives mapping, including a day holding two workouts`() = runTest {
        val assignments = apiRespondingWith(fixture()).assignments()

        assertEquals(2, assignments.count { it.dayIndex == 4 })
        assertEquals(0, assignments.count { it.dayIndex == 2 })
    }

    @Test
    fun `a non-2xx response carries the HTTP status as the error code`() = runTest {
        val api = apiReturning { MockEngine { respondError(HttpStatusCode.InternalServerError) } }

        assertEquals(500, api.error().code)
    }

    /**
     * NOTE: `Exception.toResult()` has no branch for deserialization, so a
     * malformed body lands on UNKNOWN. Recorded rather than silently patched —
     * adding an ApiError.PARSING code would be a change to shared convention.
     */
    @Test
    fun `a malformed body is reported as an error rather than empty data`() = runTest {
        val api = apiRespondingWith("""{"data":[{"day":"not a number"}]}""")

        assertEquals(ApiError.UNKNOWN, api.error().code)
    }

    @Test
    fun `a transport failure maps to the NETWORK code`() = runTest {
        val api = apiReturning { MockEngine { throw UnknownHostException("no connectivity") } }

        assertEquals(ApiError.NETWORK, api.error().code)
    }

    private companion object {
        const val BASE_URL = "https://mock.internalef.com/workouts"
    }
}
