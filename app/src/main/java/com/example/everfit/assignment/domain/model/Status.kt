package com.example.everfit.assignment.domain.model

/**
 * The `status` field as the API sends it.
 *
 * The API does not document these values. The mapping below was read off
 * docs/design/training.png against the committed fixture: `status=1` items render
 * as "Missed" and the `status=2` item renders as "Completed". That is the reverse
 * of the intuitive ordering, so do not "correct" it without re-checking the design.
 */
enum class StoredStatus(val raw: Int) {
    ASSIGNED(0),
    MISSED(1),
    COMPLETED(2),

    /** Anything the server adds later. Renders greyed; never crashes. */
    UNKNOWN(Int.MIN_VALUE);

    companion object {
        fun fromRaw(raw: Int): StoredStatus =
            entries.firstOrNull { it.raw == raw } ?: UNKNOWN
    }
}

/** Where a day sits relative to today. */
enum class DayPosition { PAST, TODAY, FUTURE }

/**
 * What the cell actually shows. Distinct from [StoredStatus] because the brief
 * defines status by temporal position: the same stored value renders differently
 * depending on the day.
 */
enum class DisplayStatus { COMPLETED, MISSED, ASSIGNED, UPCOMING }
