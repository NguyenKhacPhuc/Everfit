package com.example.everfit.assignment.data

import com.example.everfit.assignment.core.model.Result
import com.example.everfit.assignment.data.base.asResult
import com.example.everfit.assignment.data.network.ApiError
import com.example.everfit.assignment.data.local.dao.CompletionDao
import com.example.everfit.assignment.data.local.dao.WorkoutDao
import com.example.everfit.assignment.data.local.entity.CompletionOverrideEntity
import com.example.everfit.assignment.data.mapper.toDomain
import com.example.everfit.assignment.data.mapper.toEntity
import com.example.everfit.assignment.core.domain.WorkoutRemoteSource
import com.example.everfit.assignment.core.domain.WorkoutRepository
import com.example.everfit.assignment.core.model.WorkoutAssignment
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/** Cache is the source of truth; the network only writes into it. */
class WorkoutRepositoryImpl(
    private val remote: WorkoutRemoteSource,
    private val workoutDao: WorkoutDao,
    private val completionDao: CompletionDao,
    private val ioDispatcher: CoroutineDispatcher,
) : WorkoutRepository {

    override fun observeWeek(): Flow<List<WorkoutAssignment>> =
        workoutDao.observeAll().map { rows -> rows.map { it.toDomain() } }

    override suspend fun refresh(): Result<Unit> = withContext(ioDispatcher) {
        val fetched = remote.fetchWorkouts()
            .asResult()
            .filterNot { it is Result.Loading }
            .firstOrNull() // not first(): safeApiCall can complete without emitting
            ?: Result.Error(ApiError.UNKNOWN, "empty response")

        when (fetched) {
            is Result.Success -> {
                workoutDao.replaceAssignments(fetched.data.map { it.toEntity() })
                Result.Success(Unit)
            }

            // No branch clears the cache.
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
