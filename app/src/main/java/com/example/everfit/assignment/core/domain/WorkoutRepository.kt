package com.example.everfit.assignment.core.domain

import com.example.everfit.assignment.core.model.Result
import com.example.everfit.assignment.core.model.WorkoutAssignment
import kotlinx.coroutines.flow.Flow

/**
 * Declared here and implemented in `data`, so the dependency points inward.
 *
 * [refresh] writes into the same store [observeWeek] reads, rather than
 * returning data — which is what makes cache-before-network an ordering
 * property rather than a timing accident.
 */
interface WorkoutRepository {

    fun observeWeek(): Flow<List<WorkoutAssignment>>

    /** Never clears the cache on failure. */
    suspend fun refresh(): Result<Unit>

    suspend fun toggleCompletion(assignmentId: String)
}
