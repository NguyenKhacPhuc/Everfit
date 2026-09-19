package com.example.everfit.assignment

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.example.everfit.assignment.domain.WeekProvider
import com.example.everfit.assignment.ui.calendar.components.WeekGrid
import com.example.everfit.assignment.ui.theme.EverfitTheme
import java.time.Clock
import java.time.LocalDate

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Intent 01 scaffold: the grid is driven directly by WeekProvider.
        // Intent 03 replaces this with a Koin-provided CalendarViewModel.
        val weekProvider = WeekProvider(Clock.systemDefaultZone())

        setContent {
            EverfitTheme {
                WeekGrid(
                    weekDates = weekProvider.currentWeek(),
                    today = weekProvider.today(),
                )
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 640)
@Composable
private fun CalendarPreview() {
    val monday = LocalDate.parse("2026-09-14")
    EverfitTheme {
        WeekGrid(
            weekDates = List(7) { monday.plusDays(it.toLong()) },
            today = LocalDate.parse("2026-09-19"),
        )
    }
}
