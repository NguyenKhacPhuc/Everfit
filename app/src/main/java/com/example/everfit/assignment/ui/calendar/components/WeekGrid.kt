package com.example.everfit.assignment.ui.calendar.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.everfit.assignment.ui.theme.EverfitTheme
import java.time.LocalDate

/**
 * The week, Monday to Sunday.
 *
 * Takes dates directly rather than reading a clock, so it renders identically in a
 * preview, a screenshot test and the app. Dates are present before any workout data
 * exists — which is what makes the brief's loading requirement (correct dates, empty
 * cells) fall out rather than need retrofitting.
 */
@Composable
fun WeekGrid(
    weekDates: List<LocalDate>,
    today: LocalDate,
    modifier: Modifier = Modifier,
    dayContent: @Composable (LocalDate) -> Unit = {},
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            // Background first so it bleeds under the system bars, then inset the
            // content. Android 15+ draws edge-to-edge by default, so without this
            // the Monday row sits under the status bar.
            .background(EverfitTheme.colors.screenBackground)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(EverfitTheme.spacing.lg),
    ) {
        weekDates.forEach { date ->
            DayCell(date = date, isToday = date == today) {
                dayContent(date)
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 640)
@Composable
private fun WeekGridPreview() {
    val monday = LocalDate.parse("2026-09-14")
    EverfitTheme {
        WeekGrid(
            weekDates = List(7) { monday.plusDays(it.toLong()) },
            today = LocalDate.parse("2026-09-19"),
        )
    }
}
