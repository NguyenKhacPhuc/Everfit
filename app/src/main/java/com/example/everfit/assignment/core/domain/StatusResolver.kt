package com.example.everfit.assignment.core.domain

import com.example.everfit.assignment.core.model.DayPosition
import com.example.everfit.assignment.core.model.DisplayStatus
import com.example.everfit.assignment.core.model.StoredStatus

/**
 * A local override wins permanently — no later server response can change a
 * workout the user has tapped. There is no write API, so a local mark is a
 * client-side fact by construction. Trade-offs: Drill 05 §6.
 */
fun resolveCompletion(override: Boolean?, stored: StoredStatus): Boolean =
    override ?: (stored == StoredStatus.COMPLETED)

/**
 * A future day is UPCOMING whatever its completion, which is why this stays
 * separate from [resolveCompletion] — folding the two passes every other case
 * and fails exactly there.
 */
fun resolveDisplay(position: DayPosition, isCompleted: Boolean): DisplayStatus =
    when (position) {
        DayPosition.FUTURE -> DisplayStatus.UPCOMING
        DayPosition.TODAY -> if (isCompleted) DisplayStatus.COMPLETED else DisplayStatus.ASSIGNED
        DayPosition.PAST -> if (isCompleted) DisplayStatus.COMPLETED else DisplayStatus.MISSED
    }
