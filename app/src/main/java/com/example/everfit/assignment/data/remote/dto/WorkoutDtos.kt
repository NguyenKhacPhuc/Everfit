package com.example.everfit.assignment.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * The wire format, mirrored exactly and doing nothing else.
 *
 * Renaming and reshaping happen at the mapping boundary, so a change in the
 * payload touches one file rather than the whole app.
 *
 * Note the payload carries no dates at all — only `day: 0..6`, which
 * WeekProvider maps onto the current Monday–Sunday week.
 */
@Serializable
data class WorkoutsResponseDto(
    val data: List<DayDto> = emptyList(),
)

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
