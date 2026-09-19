package com.example.everfit.assignment.data

import com.example.everfit.assignment.core.Result
import com.example.everfit.assignment.core.asResult
import com.example.everfit.assignment.data.network.ApiError
import com.example.everfit.assignment.data.remote.WorkoutApi
import com.example.everfit.assignment.core.model.StoredStatus
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import java.net.UnknownHostException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * TIER 1 via Ktor's MockEngine — no network, no live endpoint.
 *
 * `safeApiCall` lets failures leave as exceptions; `asResult()` types them at the
 * collecting end, which is where these tests assert.
 */
class WorkoutApiTest {

    private fun fixture(): String = javaClass.classLoader!!
        .getResourceAsStream("workouts.json")!!
        .bufferedReader().readText()

    private fun api(engine: MockEngine): WorkoutApi {
        val client = HttpClient(engine) {
            // Matches the production client: the wrapper relies on Ktor throwing
            // for a non-2xx so Exception.toResult() can type it.
            expectSuccess = true
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }
        return WorkoutApi(client, BASE_URL)
    }

    private fun apiRespondingWith(body: String) = api(
        MockEngine {
            respond(
                body,
                HttpStatusCode.OK,
                headersOf("Content-Type", ContentType.Application.Json.toString()),
            )
        }
    )

    private suspend fun WorkoutApi.failure(): Result.Error =
        fetchWorkouts().asResult().filterNot { it is Result.Loading }.first() as Result.Error

    @Test
    fun `a successful response becomes domain assignments`() = runTest {
        val assignments = apiRespondingWith(fixture()).fetchWorkouts().first()

        assertEquals(6, assignments.size)
        assertTrue(assignments.all { it.dayIndex in 0..6 })
    }

    @Test
    fun `raw status is mapped to the named status on the way through`() = runTest {
        val assignments = apiRespondingWith(fixture()).fetchWorkouts().first()
        val legsDayMonday = assignments.first { it.dayIndex == 0 }

        assertEquals("Legs day", legsDayMonday.title)
        assertEquals(StoredStatus.MISSED, legsDayMonday.storedStatus)
        assertEquals(5, legsDayMonday.totalExercises)
    }

    @Test
    fun `day index survives mapping, including a day holding two workouts`() = runTest {
        val assignments = apiRespondingWith(fixture()).fetchWorkouts().first()

        assertEquals(2, assignments.count { it.dayIndex == 4 })
        assertEquals(0, assignments.count { it.dayIndex == 2 })
    }

    @Test
    fun `a non-2xx response carries the HTTP status as the error code`() = runTest {
        val failure = api(MockEngine { respondError(HttpStatusCode.InternalServerError) }).failure()

        assertEquals(500, failure.code)
    }

    @Test
    fun `a transport failure maps to the NETWORK code`() = runTest {
        val failure = api(MockEngine { throw UnknownHostException("no connectivity") }).failure()

        assertEquals(ApiError.NETWORK, failure.code)
    }

    /**
     * NOTE: `Exception.toResult()` has no branch for deserialization, so a
     * malformed body lands on UNKNOWN. Recorded rather than silently patched —
     * adding an ApiError.PARSING code would change shared convention.
     */
    @Test
    fun `a malformed body is reported as an error rather than empty data`() = runTest {
        val failure = apiRespondingWith("""{"data":[{"day":"not a number"}]}""").failure()

        assertEquals(ApiError.UNKNOWN, failure.code)
    }

    private companion object {
        const val BASE_URL = "https://mock.internalef.com/workouts"
    }
}
