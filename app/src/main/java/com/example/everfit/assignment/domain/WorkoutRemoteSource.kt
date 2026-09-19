package com.example.everfit.assignment.domain

import com.example.everfit.assignment.model.WorkoutAssignment
import kotlinx.coroutines.flow.Flow

/**
 * The network, as the repository sees it.
 *
 * A cold [Flow] rather than a suspend function, matching the shared
 * safeApiCall/asResult idiom: failures travel as exceptions and are typed once,
 * at the collecting end.
 *
 * It also stays testable in the way rung 3.3 needs — a fake can hold the flow
 * open, which is the only way to prove cache emits *before* the network returns.
 */
interface WorkoutRemoteSource {
    fun fetchWorkouts(): Flow<List<WorkoutAssignment>>
}
