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
 * Failures leave as exceptions for `asResult()` to type.
 *
 * Note [NoTransformationFoundException] is swallowed, so the flow can complete
 * **without emitting** — collectors must tolerate zero items.
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

/** Named rather than overloading Ktor's own single-argument `get(urlString)`. */
suspend inline fun <reified Response> HttpClient.getBaseResponse(
    url: String,
): BaseResponse<Response> = get(url).body()
