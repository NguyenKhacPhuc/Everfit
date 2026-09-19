package com.example.everfit.assignment.data.network

import kotlinx.serialization.Serializable

/**
 * The envelope every endpoint returns.
 *
 * The workouts endpoint responds with `{"data": [...]}` and carries **no**
 * status/success flag, so [isSuccessful] treats their absence as success.
 * Requiring `status == true` — as a backend that always sends one could — would
 * reject every response this API produces.
 */
@Serializable
data class BaseResponse<T>(
    val data: T? = null,
    val status: Boolean? = null,
    val success: Boolean? = null,
    val code: Int? = null,
    val message: String? = null,
) {
    val isSuccessful: Boolean get() = status ?: success ?: true
}

fun BaseResponse<*>.mapToApiError(): ResponseApiError = ResponseApiError(
    code = code ?: ApiError.UNKNOWN,
    message = message,
    data = data,
)
