package com.example.everfit.assignment.domain.model

import com.example.everfit.assignment.domain.resolveCompletion

/**
 * One workout on one day of the week.
 *
 * [dayIndex] is the API's `day` field (0 = Monday … 6 = Sunday). There is no date
 * here on purpose: the payload carries none, and WeekProvider owns the mapping
 * onto the current week.
 *
 * [localOverride] is the user's own mark, held separately from [storedStatus] so
 * a refresh that disagrees cannot silently revert it. Null means "no local
 * opinion", which is not the same as "not completed".
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
