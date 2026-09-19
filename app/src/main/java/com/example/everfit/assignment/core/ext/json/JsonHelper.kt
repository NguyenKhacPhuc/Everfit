package com.example.everfit.assignment.core.ext.json

import kotlinx.serialization.json.Json

object JsonHelper {
    val json: Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        explicitNulls = false
    }

    /** Null rather than throwing: a failure to parse an error body must not mask the error. */
    inline fun <reified T> toObject(raw: String): T? = try {
        json.decodeFromString<T>(raw)
    } catch (e: Exception) {
        null
    }
}
