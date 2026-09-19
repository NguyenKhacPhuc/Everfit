package com.example.everfit.assignment.data.remote

import com.example.everfit.assignment.data.DataError
import com.example.everfit.assignment.data.mapper.toDomain
import com.example.everfit.assignment.data.remote.dto.WorkoutsResponseDto
import com.example.everfit.assignment.domain.model.WorkoutAssignment
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.isSuccess
import io.ktor.serialization.ContentConvertException
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import java.io.IOException

/**
 * The only place that knows Ktor exists.
 *
 * Returns [Result] rather than throwing, with every failure mapped to a
 * [DataError]. Cancellation is deliberately re-thrown: swallowing it into a
 * Result would break structured concurrency and make a cancelled refresh
 * indistinguishable from a failed one.
 */
class WorkoutApi(
    private val client: HttpClient,
    private val endpoint: String,
) {
    suspend fun fetchWorkouts(): Result<List<WorkoutAssignment>> = try {
        val response = client.get(endpoint)

        // Checked explicitly rather than relying on `expectSuccess`, which is
        // client configuration this class does not own. Without it a 500 would
        // reach the deserializer and surface as a parsing failure.
        if (!response.status.isSuccess()) {
            Result.failure(DataError.Server(response.status.value))
        } else {
            val dto: WorkoutsResponseDto = response.body()
            Result.success(dto.toDomain())
        }
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (error: Throwable) {
        Result.failure(error.toDataError())
    }
}

private fun Throwable.toDataError(): DataError = when {
    // Ktor wraps deserialization failures in its own type, so matching on
    // SerializationException alone lets a malformed body escape as Unknown.
    this is ContentConvertException || this is SerializationException ||
        hasCauseOfType<SerializationException>() -> DataError.Parsing(this)

    this is IOException || hasCauseOfType<IOException>() -> DataError.Network(this)

    else -> DataError.Unknown(this)
}

private inline fun <reified T : Throwable> Throwable.hasCauseOfType(): Boolean {
    var current = cause
    while (current != null && current != this) {
        if (current is T) return true
        current = current.cause
    }
    return false
}
