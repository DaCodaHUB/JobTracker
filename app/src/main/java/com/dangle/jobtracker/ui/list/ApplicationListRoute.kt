package com.dangle.jobtracker.ui.list

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue

@Composable
fun ApplicationListRoute(
    viewModel: ApplicationListViewModel,
    // This callback comes from your NavHost setup in MainActivity
    onNavigateToAddApplication: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    ApplicationListScreen(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        // Pass the navigation callback directly to the screen
        onAddClick = onNavigateToAddApplication
    )
}