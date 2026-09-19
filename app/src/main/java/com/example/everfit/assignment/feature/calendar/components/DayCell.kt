package com.example.everfit.assignment.feature.calendar.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.everfit.assignment.core.ui.theme.EverfitTheme
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

/**
 * One day of the week: its label on the left, its workouts on the right.
 *
 * Layout confirmed against docs/design/training.png — seven rows, not seven
 * columns, because a day holds multiple workout cards.
 */
@Composable
fun DayCell(
    date: LocalDate,
    isToday: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit = {},
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = EverfitTheme.sizes.screenPadding,
                    vertical = EverfitTheme.spacing.lg,
                ),
            verticalAlignment = Alignment.Top,
        ) {
            DayLabel(date = date, isToday = isToday)

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(EverfitTheme.spacing.sm),
            ) {
                content()
            }
        }

        HorizontalDivider(
            thickness = EverfitTheme.sizes.dividerThickness,
            color = EverfitTheme.colors.divider,
        )
    }
}

/**
 * Day-of-week above day-of-month, per the brief.
 *
 * Today is the accent colour applied to the **text**, not a filled background —
 * an earlier version used a filled pill, which the design does not do.
 *
 * Locale is pinned to English so labels stay MON/TUE and screenshot checks are
 * deterministic; a localised app would read this from the configuration.
 */
@Composable
private fun DayLabel(date: LocalDate, isToday: Boolean) {
    Column(
        modifier = Modifier.width(EverfitTheme.sizes.dayLabelWidth),
        horizontalAlignment = Alignment.Start,
    ) {
        Text(
            text = date.dayOfWeek
                .getDisplayName(TextStyle.SHORT, Locale.ENGLISH)
                .uppercase(Locale.ENGLISH),
            style = MaterialTheme.typography.labelSmall,
            color = if (isToday) EverfitTheme.colors.accent
            else EverfitTheme.colors.textSecondary,
        )
        Text(
            text = date.dayOfMonth.toString(),
            // bodyLarge, not titleMedium: the day number is 16sp/400 per the
            // design, while the card title at the same size is bold.
            style = MaterialTheme.typography.bodyLarge,
            color = if (isToday) EverfitTheme.colors.accent
            else EverfitTheme.colors.textPrimary,
        )
    }
}

@Preview(showBackground = true, widthDp = 375)
@Composable
private fun DayCellPreview() {
    EverfitTheme {
        Column {
            DayCell(date = LocalDate.parse("2026-09-14"), isToday = false)
            DayCell(date = LocalDate.parse("2026-09-19"), isToday = true)
        }
    }
}
