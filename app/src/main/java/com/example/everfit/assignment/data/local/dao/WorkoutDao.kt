package com.example.everfit.assignment.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.example.everfit.assignment.data.local.entity.CompletionOverrideEntity
import com.example.everfit.assignment.data.local.entity.WorkoutAssignmentEntity
import com.example.everfit.assignment.data.local.entity.WorkoutWithOverride
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutDao {

    /**
     * The merge is a LEFT JOIN rather than two flows combined in Kotlin.
     *
     * It is the relational expression of "assignments, with their override if
     * any" — and it is one query, one invalidation and one emission, where
     * `combine` would need two subscriptions, would re-emit on either table
     * changing, and would rebuild a lookup map on every emission.
     *
     * It also keeps the pair atomic. That is *not* load-bearing today: nothing
     * writes both tables in one transaction, so `combine` could not actually
     * tear. It would become load-bearing the moment something did — pruning
     * redundant overrides during a refresh, say, which is exactly what option B
     * in Drill 05 §6 would have done.
     */
    @Query(
        """
        SELECT a.id            AS id,
               a.day_index     AS day_index,
               a.title         AS title,
               a.server_status AS server_status,
               a.total_exercises AS total_exercises,
               c.is_completed  AS local_override
        FROM workout_assignments a
        LEFT JOIN completion_overrides c ON c.assignment_id = a.id
        ORDER BY a.day_index ASC, a.id ASC
        """
    )
    fun observeAll(): Flow<List<WorkoutWithOverride>>

    @Query("DELETE FROM workout_assignments")
    suspend fun deleteAllAssignments()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAssignments(items: List<WorkoutAssignmentEntity>)

    /**
     * Replaces server truth atomically. Note it touches only
     * `workout_assignments` — never `completion_overrides`.
     */
    @Transaction
    suspend fun replaceAssignments(items: List<WorkoutAssignmentEntity>) {
        deleteAllAssignments()
        insertAssignments(items)
    }
}

@Dao
interface CompletionDao {

    @Query("SELECT is_completed FROM completion_overrides WHERE assignment_id = :assignmentId")
    suspend fun overrideFor(assignmentId: String): Boolean?

    @Upsert
    suspend fun upsert(override: CompletionOverrideEntity)

    @Query("DELETE FROM completion_overrides WHERE assignment_id = :assignmentId")
    suspend fun delete(assignmentId: String)
}
