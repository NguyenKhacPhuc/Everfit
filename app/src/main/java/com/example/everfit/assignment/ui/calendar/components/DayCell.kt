package com.example.everfit.assignment.ui.calendar.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.everfit.assignment.ui.theme.EverfitTheme
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

/**
 * One day of the week: its label on the left, its workouts on the right.
 *
 * LAYOUT ASSUMPTION — pending the design. The brief requires that a day
 * "can hold multiple workouts", each showing a name, an exercise count and a
 * status. Seven side-by-side columns cannot fit that, so the week is laid out
 * as seven rows. Confirm against the Figma export before Intent 04.
 */
@Composable
fun DayCell(
    date: LocalDate,
    isToday: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit = {},
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = EverfitTheme.spacing.xs),
        verticalAlignment = Alignment.Top,
    ) {
        DayLabel(date = date, isToday = isToday)

        Column(
            modifier = Modifier
                .padding(start = EverfitTheme.spacing.md)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(EverfitTheme.spacing.sm),
        ) {
            content()
        }
    }
}

/**
 * Day-of-week above day-of-month, per the brief.
 *
 * Locale is pinned to English so the labels stay "Mon/Tue" and screenshot checks
 * are deterministic. A localised app would take this from the configuration.
 */
@Composable
private fun DayLabel(date: LocalDate, isToday: Boolean) {
    val shape = RoundedCornerShape(EverfitTheme.sizes.cellCorner)
    Column(
        modifier = Modifier
            .width(LABEL_WIDTH)
            .clip(shape)
            .background(
                if (isToday) EverfitTheme.colors.todayHighlight
                else androidx.compose.ui.graphics.Color.Transparent
            )
            .padding(
                horizontal = EverfitTheme.spacing.sm,
                vertical = EverfitTheme.spacing.sm,
            ),
        horizontalAlignment = Alignment.Start,
    ) {
        Text(
            text = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.ENGLISH),
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Start,
            color = if (isToday) EverfitTheme.colors.onTodayHighlight
            else EverfitTheme.colors.textSecondary,
        )
        Text(
            text = date.dayOfMonth.toString(),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Start,
            color = if (isToday) EverfitTheme.colors.onTodayHighlight
            else EverfitTheme.colors.textPrimary,
        )
    }
}

/** Placeholder width until the design provides one. */
private val LABEL_WIDTH = 56.dp

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun DayCellPreview() {
    EverfitTheme {
        Column {
            DayCell(date = LocalDate.parse("2026-09-14"), isToday = false)
            DayCell(date = LocalDate.parse("2026-09-19"), isToday = true)
        }
    }
}
