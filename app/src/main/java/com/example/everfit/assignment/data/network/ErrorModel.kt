package com.example.everfit.assignment.data.network

import kotlinx.serialization.Serializable

@Serializable
data class ErrorModel(
    val code: Int = -1,
    val message: String = "",
)
