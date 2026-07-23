package com.dangle.jobtracker.ui.list

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun ApplicationListRoute(
    viewModel: ApplicationListViewModel,
    onNavigateToAddApplication: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.syncWithServer()
    }

    ApplicationListScreen(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        onAddClick = onNavigateToAddApplication
    )
}
