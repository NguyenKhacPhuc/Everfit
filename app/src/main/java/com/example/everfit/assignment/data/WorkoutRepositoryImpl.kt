package com.example.everfit.assignment.data

import com.example.everfit.assignment.data.base.Result
import com.example.everfit.assignment.data.local.dao.CompletionDao
import com.example.everfit.assignment.data.local.dao.WorkoutDao
import com.example.everfit.assignment.data.local.entity.CompletionOverrideEntity
import com.example.everfit.assignment.data.mapper.toDomain
import com.example.everfit.assignment.data.mapper.toEntity
import com.example.everfit.assignment.domain.WorkoutRemoteSource
import com.example.everfit.assignment.domain.WorkoutRepository
import com.example.everfit.assignment.model.WorkoutAssignment
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * Cache is the source of truth; the network only writes into it.
 *
 * [observeWeek] reads the local store, so cached content reaches the UI without
 * waiting on any request. [refresh] returns no data — it writes and reports
 * success or failure. That is what makes "cache first, network second" an
 * ordering property of the design rather than a timing accident.
 */
class WorkoutRepositoryImpl(
    private val remote: WorkoutRemoteSource,
    private val workoutDao: WorkoutDao,
    private val completionDao: CompletionDao,
    private val ioDispatcher: CoroutineDispatcher,
) : WorkoutRepository {

    override fun observeWeek(): Flow<List<WorkoutAssignment>> =
        workoutDao.observeAll().map { rows -> rows.map { it.toDomain() } }

    override suspend fun refresh(): Result<Unit> = withContext(ioDispatcher) {
        when (val fetched = remote.fetchWorkouts()) {
            is Result.Success -> {
                // Writes workout_assignments ONLY. completion_overrides is never
                // touched here, which is what stops a refresh reverting a local
                // mark (rung 5.5).
                workoutDao.replaceAssignments(fetched.data.map { it.toEntity() })
                Result.Success(Unit)
            }

            // Non-destructive by construction: no branch clears the cache.
            is Result.Error -> fetched
            Result.Loading -> Result.Loading
        }
    }

    override suspend fun toggleCompletion(assignmentId: String): Unit = withContext(ioDispatcher) {
        val current = completionDao.overrideFor(assignmentId)
        completionDao.upsert(
            CompletionOverrideEntity(
                assignmentId = assignmentId,
                isCompleted = !(current ?: false),
                updatedAt = System.currentTimeMillis(),
            )
        )
    }
}
