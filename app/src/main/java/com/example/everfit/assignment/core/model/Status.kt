package com.example.everfit.assignment.core.model

/**
 * Undocumented by the API. Read off the design against the fixture: 1 is MISSED
 * and 2 is COMPLETED — the reverse of the intuitive ordering. Do not "fix" this
 * without re-checking docs/design/training.png.
 */
enum class StoredStatus(val raw: Int) {
    ASSIGNED(0),
    MISSED(1),
    COMPLETED(2),

    /** Anything the server adds later. Renders greyed rather than crashing. */
    UNKNOWN(Int.MIN_VALUE);

    companion object {
        fun fromRaw(raw: Int): StoredStatus =
            entries.firstOrNull { it.raw == raw } ?: UNKNOWN
    }
}

enum class DayPosition { PAST, TODAY, FUTURE }

/**
 * Distinct from [StoredStatus]: the brief defines status by temporal position,
 * so the same stored value renders differently depending on the day.
 */
enum class DisplayStatus { COMPLETED, MISSED, ASSIGNED, UPCOMING }
