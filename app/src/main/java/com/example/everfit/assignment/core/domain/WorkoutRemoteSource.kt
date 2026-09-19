package com.example.everfit.assignment.core.domain

import com.example.everfit.assignment.core.model.WorkoutAssignment
import kotlinx.coroutines.flow.Flow

/**
 * A cold [Flow] rather than a suspend function: failures travel as exceptions
 * and are typed once by `asResult()` at the collecting end. A fake can also
 * hold it open, which is how the cache-before-network test is written.
 */
interface WorkoutRemoteSource {
    fun fetchWorkouts(): Flow<List<WorkoutAssignment>>
}
