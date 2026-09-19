package com.example.everfit.assignment.data.network.service

import com.example.everfit.assignment.data.network.BaseResponse
import com.example.everfit.assignment.data.network.mapToApiError
import io.ktor.client.HttpClient
import io.ktor.client.call.NoTransformationFoundException
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Wraps an API call so the call site reads as one line and every failure leaves
 * as an exception for `asResult()` to type.
 *
 * Two behaviours inherited from the shared wrapper, both deliberate and both
 * sharp enough to be worth stating:
 *
 * 1. A [NoTransformationFoundException] is swallowed, because a backend can
 *    return a shape we cannot parse and that must not crash the app. The flow
 *    then completes **without emitting**, so a collector must tolerate zero
 *    items — see WorkoutRepositoryImpl, which treats "no emission" as an error
 *    rather than letting `first()` throw.
 * 2. `data` is cast to `T` unchecked. A 2xx response carrying a null `data`
 *    would emit null. This endpoint always returns the array, and the parsing
 *    tests pin that.
 */
fun <T : Any?> safeApiCall(call: suspend () -> BaseResponse<T>): Flow<T> = flow {
    try {
        val result = call()
        if (result.isSuccessful) {
            @Suppress("UNCHECKED_CAST")
            emit(result.data as T)
        } else {
            throw result.mapToApiError()
        }
    } catch (e: Exception) {
        // BE might return an invalid JSON response shape; do not re-throw that one.
        if (e !is NoTransformationFoundException) throw e
    }
}

/**
 * GET returning the envelope.
 *
 * Named rather than overloading `get`, which would clash with Ktor's own
 * single-argument `get(urlString)`.
 */
suspend inline fun <reified Response> HttpClient.getBaseResponse(
    url: String,
): BaseResponse<Response> = get(url).body()
