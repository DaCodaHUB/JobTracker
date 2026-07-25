package com.dangle.jobtracker.ui.application

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.material3.SnackbarHostState
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * The entry point for the Job Application creation screen.
 * 
 * This composable function acts as a bridge between the [JobApplicationViewModel] and the 
 * stateless [JobApplicationScreen]. It is responsible for:
 * 1. Hoisting and observing UI state with lifecycle awareness.
 * 2. Collecting and handling one-off side effects (navigation, snackbars).
 * 3. Wiring up user interaction events to the ViewModel.
 *
 * @param viewModel Injected ViewModel that manages the screen's logic and data.
 * @param onBackClick Navigation callback to return to the previous screen.
 */
@Composable
fun JobApplicationRoute(
    viewModel: JobApplicationViewModel,
    onBackClick: () -> Unit
) {
    // Observe the UI state flow with lifecycle awareness to prevent resource leaks
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    // Snackbar state is hoisted here to survive recompositions while the effect block runs
    val snackbarHostState = remember { SnackbarHostState() }

    // Side-effect collection: Handles one-time events from the ViewModel
    LaunchedEffect(viewModel.effect) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is JobApplicationSideEffect.NavigateBack -> onBackClick()
                is JobApplicationSideEffect.ShowError -> {
                    snackbarHostState.showSnackbar(effect.message)
                }
            }
        }
    }

    // Delegate to the pure UI implementation
    JobApplicationScreen(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onEvent = viewModel::onEvent,
        onBackClick = onBackClick
    )
}
