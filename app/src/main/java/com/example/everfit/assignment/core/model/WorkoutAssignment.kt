package com.example.everfit.assignment.core.model

import com.example.everfit.assignment.core.domain.resolveCompletion

/**
 * [dayIndex] is the API's `day` field (0 = Monday). No date here on purpose —
 * the payload carries none, and WeekProvider owns that mapping.
 *
 * [localOverride] null means "no local opinion", which is not "not completed".
 */
data class WorkoutAssignment(
    val id: String,
    val dayIndex: Int,
    val title: String,
    val storedStatus: StoredStatus,
    val totalExercises: Int,
    val localOverride: Boolean? = null,
) {
    val isCompleted: Boolean get() = resolveCompletion(localOverride, storedStatus)
}
