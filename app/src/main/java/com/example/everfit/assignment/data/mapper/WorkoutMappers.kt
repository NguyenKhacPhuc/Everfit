package com.example.everfit.assignment.data.mapper

import com.example.everfit.assignment.data.local.entity.WorkoutAssignmentEntity
import com.example.everfit.assignment.data.local.entity.WorkoutWithOverride
import com.example.everfit.assignment.data.remote.dto.DayDto
import com.example.everfit.assignment.model.StoredStatus
import com.example.everfit.assignment.model.WorkoutAssignment

/**
 * Wire -> domain. One direction, at the boundary it crosses.
 *
 * Flattens the nested day/assignment shape by carrying the day index onto each
 * assignment, so callers work with a flat list and group it as the UI needs.
 */
fun List<DayDto>.toDomain(): List<WorkoutAssignment> =
    flatMap { day ->
        day.assignments.map { assignment ->
            WorkoutAssignment(
                id = assignment.id,
                dayIndex = day.day,
                title = assignment.title,
                storedStatus = StoredStatus.fromRaw(assignment.status),
                totalExercises = assignment.totalExercise,
            )
        }
    }

fun WorkoutAssignment.toEntity() = WorkoutAssignmentEntity(
    id = id,
    dayIndex = dayIndex,
    title = title,
    serverStatus = storedStatus.raw,
    totalExercises = totalExercises,
)

fun WorkoutWithOverride.toDomain() = WorkoutAssignment(
    id = id,
    dayIndex = dayIndex,
    title = title,
    storedStatus = StoredStatus.fromRaw(serverStatus),
    totalExercises = totalExercises,
    localOverride = localOverride,
)
