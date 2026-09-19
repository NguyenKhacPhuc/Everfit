package com.example.everfit.assignment.core.model

/**
 * Deliberately import-free: `core` is depended on by everything, and this type
 * appears in the `domain` contract. The Ktor-aware builders live in `data/base`.
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
