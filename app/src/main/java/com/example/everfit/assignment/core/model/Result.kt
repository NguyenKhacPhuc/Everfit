package com.example.everfit.assignment.core.model

/**
 * The outcome of an operation.
 *
 * Deliberately free of imports. `core` is depended on by every other layer, so
 * anything reachable from here is reachable from everywhere — and this type is
 * named in the `domain` repository contract, which must stay free of HTTP.
 *
 * The helpers that build a Result from an HTTP response or an exception know
 * about Ktor, so they live in `data/base` rather than here.
 */
sealed interface Result<out T> {
    data class Success<T>(val data: T) : Result<T>

    data class Error(
        val code: Int,
        val message: String,
        val exception: Exception? = null,
        val data: Any? = null,
    ) : Result<Nothing>

    data object Loading : Result<Nothing>
}
