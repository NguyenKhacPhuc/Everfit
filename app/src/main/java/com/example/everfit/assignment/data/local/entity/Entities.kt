package com.example.everfit.assignment.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/** Server truth. Replaced wholesale on every refresh. */
@Entity(tableName = "workout_assignments")
data class WorkoutAssignmentEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "day_index") val dayIndex: Int,
    val title: String,
    @ColumnInfo(name = "server_status") val serverStatus: Int,
    @ColumnInfo(name = "total_exercises") val totalExercises: Int,
)

/**
 * Separate from the assignment so a refresh structurally cannot destroy it.
 *
 * A mutable `is_completed` column would be less code, and would silently revert
 * the user's tap on the next refresh.
 */
@Entity(tableName = "completion_overrides")
data class CompletionOverrideEntity(
    @PrimaryKey @ColumnInfo(name = "assignment_id") val assignmentId: String,
    @ColumnInfo(name = "is_completed") val isCompleted: Boolean,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
)

/** The two tables joined. */
data class WorkoutWithOverride(
    @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "day_index") val dayIndex: Int,
    @ColumnInfo(name = "title") val title: String,
    @ColumnInfo(name = "server_status") val serverStatus: Int,
    @ColumnInfo(name = "total_exercises") val totalExercises: Int,
    @ColumnInfo(name = "local_override") val localOverride: Boolean?,
)
