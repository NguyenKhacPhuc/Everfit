package com.example.everfit.assignment

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.everfit.assignment.ui.theme.EverfitTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            EverfitTheme {
                PlaceholderScreen()
            }
        }
    }
}

/**
 * Rung 0.2/0.3 scaffold, replaced by CalendarScreen in Intent 01.
 *
 * Deliberately uses only theme tokens — no literal colour, dp or sp. That is the
 * verification for rung 0.3: if this screen can be written without literals, the
 * token layer is complete enough for the real screen.
 */
@Composable
private fun PlaceholderScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(EverfitTheme.colors.screenBackground)
            .padding(EverfitTheme.spacing.lg),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "Training Calendar",
            style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
            color = EverfitTheme.colors.textPrimary,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PlaceholderScreenPreview() {
    EverfitTheme { PlaceholderScreen() }
}
