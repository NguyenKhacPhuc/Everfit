package com.example.everfit.assignment

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.everfit.assignment.ui.calendar.CalendarEffect
import com.example.everfit.assignment.ui.calendar.CalendarScreen
import com.example.everfit.assignment.ui.calendar.CalendarViewModel
import com.example.everfit.assignment.ui.theme.EverfitTheme
import org.koin.androidx.compose.koinViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            EverfitTheme {
                val viewModel: CalendarViewModel = koinViewModel()
                val state by viewModel.state.collectAsStateWithLifecycle()
                val snackbarHostState = remember { SnackbarHostState() }

                // Effects are one-shot: collected here, never held in state, so a
                // rotation does not replay them.
                LaunchedEffect(viewModel) {
                    viewModel.effects.collect { effect ->
                        when (effect) {
                            is CalendarEffect.ShowMessage ->
                                snackbarHostState.showSnackbar(effect.message)
                        }
                    }
                }

                Scaffold(
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    containerColor = EverfitTheme.colors.screenBackground,
                ) { innerPadding ->
                    // Scaffold owns the system-bar insets so the snackbar sits
                    // above the navigation bar; CalendarScreen stays inset-free,
                    // which also lets its previews match the design exports.
                    CalendarScreen(
                        state = state,
                        onIntent = viewModel::onIntent,
                        modifier = Modifier.padding(innerPadding),
                    )
                }
            }
        }
    }
}
