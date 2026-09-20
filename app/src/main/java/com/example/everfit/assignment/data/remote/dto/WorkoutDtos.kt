package com.example.everfit.assignment.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DayDto(
    @SerialName("_id") val id: String,
    val day: Int,
    val assignments: List<AssignmentDto> = emptyList(),
)

@Serializable
data class AssignmentDto(
    @SerialName("_id") val id: String,
    val title: String,
    val status: Int,
    @SerialName("total_exercise") val totalExercise: Int,
)
