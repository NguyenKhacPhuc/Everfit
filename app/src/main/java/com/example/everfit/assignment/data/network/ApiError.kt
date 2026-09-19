package com.example.everfit.assignment.data.network

/** Error codes for failures that never reached an HTTP status. */
object ApiError {
    const val UNKNOWN = -1
    const val NETWORK = -2
    const val SERVER = -3
}

/** An error the API reports inside a 2xx envelope. */
class ResponseApiError(
    val code: Int? = null,
    override val message: String? = null,
    val data: Any? = null,
) : Exception(message)
