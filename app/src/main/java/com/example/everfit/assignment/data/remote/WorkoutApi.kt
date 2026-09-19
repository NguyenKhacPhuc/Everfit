package com.example.everfit.assignment.data.remote

import com.example.everfit.assignment.data.base.Result
import com.example.everfit.assignment.data.base.logAsNonFatal
import com.example.everfit.assignment.data.base.toResult
import com.example.everfit.assignment.data.mapper.toDomain
import com.example.everfit.assignment.data.remote.dto.DayDto
import com.example.everfit.assignment.domain.WorkoutRemoteSource
import com.example.everfit.assignment.domain.model.WorkoutAssignment
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import kotlinx.coroutines.CancellationException

/**
 * The only place that knows Ktor exists.
 *
 * Cancellation is re-thrown rather than captured into a Result: swallowing it
 * would break structured concurrency and make a cancelled refresh
 * indistinguishable from a failed one.
 */
class WorkoutApi(
    private val client: HttpClient,
    private val endpoint: String,
) : WorkoutRemoteSource {

    override suspend fun fetchWorkouts(): Result<List<WorkoutAssignment>> = try {
        when (val response = client.get(endpoint).toResult<List<DayDto>>()) {
            is Result.Success -> Result.Success(response.data.toDomain())
            is Result.Error -> response
            Result.Loading -> Result.Loading
        }
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (error: Exception) {
        error.logAsNonFatal()
        error.toResult()
    }
}
