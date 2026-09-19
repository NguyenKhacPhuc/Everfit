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
 * A local override always wins, and **keeps winning for good**. Once a workout
 * has been tapped, no later server response can change its completion state.
 *
 * The alternative considered (Drill 05 §6, option B) was to drop an override
 * once the server agreed with it, releasing the lock so that a *subsequent*
 * server change would be respected again. It was not taken, for two reasons:
 *
 *  - It resolves nothing. Where local and server disagree — the only case that
 *    matters — B behaves exactly like this does. It only garbage-collects
 *    overrides that have become redundant.
 *  - There is no write API. The toggle is never pushed anywhere, so a local
 *    mark is a client-side fact by construction and no merge rule makes it
 *    agree with the backend. The brief asks for the state to be toggled
 *    "locally", which is precisely this.
 *
 * The cost, stated plainly: the override table only ever grows, and a genuine
 * later change on the server is invisible for any workout the user has touched.
 * Both become worth fixing the moment completion can be written back.
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
