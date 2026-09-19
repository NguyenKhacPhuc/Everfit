package com.example.everfit.assignment.domain

import com.example.everfit.assignment.data.base.Result
import com.example.everfit.assignment.model.WorkoutAssignment
import kotlinx.coroutines.flow.Flow

/**
 * The boundary between the UI and everything that stores or fetches.
 *
 * Declared in `domain` and implemented in `data`, so the dependency arrow points
 * inward at the place it would otherwise invert.
 *
 * [observeWeek] is a standing observation of local state, not a request. Refresh
 * writes into that same local store rather than returning data, which is what
 * makes "cache first, network second" an ordering property of the design rather
 * than a timing accident.
 */
interface WorkoutRepository {

    /** Emits immediately from cache, then again whenever the cache changes. */
    fun observeWeek(): Flow<List<WorkoutAssignment>>

    /** Fetches and writes into the cache. Never clears it on failure. */
    suspend fun refresh(): Result<Unit>

    /** Flips the user's local mark for one assignment, addressed by id. */
    suspend fun toggleCompletion(assignmentId: String)
}
