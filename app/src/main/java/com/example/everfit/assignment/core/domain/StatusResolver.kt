package com.example.everfit.assignment.core.domain

import com.example.everfit.assignment.core.model.DayPosition
import com.example.everfit.assignment.core.model.DisplayStatus
import com.example.everfit.assignment.core.model.StoredStatus

/**
 * Two steps, deliberately separate.
 *
 * Top-level functions, so they have no `this` and cannot read a repository or a
 * `var` even by accident. Purity is enforced by scope rather than discipline.
 */

/**
 * Step 1 — did the user finish it?
 *
 * A local override always wins. That is what lets a tap survive a refresh whose
 * response still reports the old server status.
 */
fun resolveCompletion(override: Boolean?, stored: StoredStatus): Boolean =
    override ?: (stored == StoredStatus.COMPLETED)

/**
 * Step 2 — what does the cell show?
 *
 * A future day is UPCOMING **whatever** its completion, which is why this cannot
 * be folded into step 1. Collapsing the two passes every other case and fails
 * exactly there.
 */
fun resolveDisplay(position: DayPosition, isCompleted: Boolean): DisplayStatus =
    when (position) {
        DayPosition.FUTURE -> DisplayStatus.UPCOMING
        DayPosition.TODAY -> if (isCompleted) DisplayStatus.COMPLETED else DisplayStatus.ASSIGNED
        DayPosition.PAST -> if (isCompleted) DisplayStatus.COMPLETED else DisplayStatus.MISSED
    }
