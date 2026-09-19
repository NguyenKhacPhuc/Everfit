package com.example.everfit.assignment.core.model.common

import kotlinx.serialization.Serializable

@Serializable
data class ErrorModel(
    val code: Int = -1,
    val message: String = "",
)
