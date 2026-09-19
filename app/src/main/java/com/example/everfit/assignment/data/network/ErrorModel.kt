package com.example.everfit.assignment.data.network

import kotlinx.serialization.Serializable

/**
 * Shape of an error body the API may return. A wire model, so it lives beside
 * BaseResponse in `data/network` rather than in `core` — nothing above the data
 * layer should need to know the server's error format.
 */
@Serializable
data class ErrorModel(
    val code: Int = -1,
    val message: String = "",
)
