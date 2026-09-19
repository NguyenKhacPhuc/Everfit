package com.example.everfit.assignment.data.network

import kotlinx.serialization.Serializable

/**
 * The envelope every endpoint returns.
 *
 * The workouts endpoint responds with `{"data": [...]}`, so the payload maps onto
 * this directly rather than needing a bespoke wrapper.
 */
@Serializable
data class BaseResponse<T>(
    val data: T? = null,
)
