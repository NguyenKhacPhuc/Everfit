package com.example.everfit.assignment.domain

import com.example.everfit.assignment.data.base.Result
import com.example.everfit.assignment.domain.model.WorkoutAssignment

/**
 * The network, as the repository sees it.
 *
 * Exists so the repository can be tested on the JVM against a source the test
 * controls — specifically one that can be held open, which is the only way to
 * prove cache emits *before* the network returns rather than merely alongside it.
 */
interface WorkoutRemoteSource {
    suspend fun fetchWorkouts(): Result<List<WorkoutAssignment>>
}
