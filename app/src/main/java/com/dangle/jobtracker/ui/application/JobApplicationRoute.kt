package com.dangle.jobtracker.ui.application

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun JobApplicationRoute(
    onBackClick: () -> Unit,
    viewModel: JobApplicationViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Handle one-off side effects
    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is JobApplicationSideEffect.NavigateBack -> onBackClick()
                is JobApplicationSideEffect.ShowError -> {
                    // TODO: Trigger a Snackbar or Toast
                }
            }
        }
    }

    JobApplicationScreen(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        onBackClick = onBackClick
    )
}