package com.example.everfit.assignment.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Server truth. Replaced wholesale on every refresh.
 */
@Entity(tableName = "workout_assignments")
data class WorkoutAssignmentEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "day_index") val dayIndex: Int,
    val title: String,
    @ColumnInfo(name = "server_status") val serverStatus: Int,
    @ColumnInfo(name = "total_exercises") val totalExercises: Int,
)

/**
 * The user's own mark, in a table of its own.
 *
 * This separation is the whole answer to rung 5.5: a refresh replaces
 * `workout_assignments` and structurally cannot touch this table, so a local
 * mark cannot be destroyed by a server response that disagrees with it.
 *
 * A single mutable `is_completed` column on the assignment would be less code
 * and would silently revert the user's tap on the next refresh — a bug that
 * appears only after a refresh, so it survives casual testing.
 */
@Entity(tableName = "completion_overrides")
data class CompletionOverrideEntity(
    @PrimaryKey @ColumnInfo(name = "assignment_id") val assignmentId: String,
    @ColumnInfo(name = "is_completed") val isCompleted: Boolean,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
)

/** Projection of the two tables joined, so the merge happens in SQL, not in memory. */
data class WorkoutWithOverride(
    @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "day_index") val dayIndex: Int,
    @ColumnInfo(name = "title") val title: String,
    @ColumnInfo(name = "server_status") val serverStatus: Int,
    @ColumnInfo(name = "total_exercises") val totalExercises: Int,
    @ColumnInfo(name = "local_override") val localOverride: Boolean?,
)
