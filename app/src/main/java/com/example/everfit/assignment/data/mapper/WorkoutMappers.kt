package com.example.everfit.assignment.data.mapper

import com.example.everfit.assignment.data.remote.dto.WorkoutsResponseDto
import com.example.everfit.assignment.domain.model.StoredStatus
import com.example.everfit.assignment.domain.model.WorkoutAssignment

/**
 * Wire -> domain. One direction, at the boundary it crosses.
 *
 * Flattens the nested day/assignment shape: the day index is carried onto each
 * assignment, so callers work with a flat list and group it as the UI needs.
 */
fun WorkoutsResponseDto.toDomain(): List<WorkoutAssignment> =
    data.flatMap { day ->
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
