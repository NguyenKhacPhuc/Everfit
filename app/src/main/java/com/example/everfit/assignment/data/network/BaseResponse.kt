package com.example.everfit.assignment.data.network

import kotlinx.serialization.Serializable

/**
 * This endpoint sends no status/success flag, so [isSuccessful] treats their
 * absence as success — requiring `status == true` would reject every response.
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
