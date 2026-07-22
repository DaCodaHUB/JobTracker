package com.dangle.jobtracker.ui.list

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue

@Composable
fun ApplicationListRoute(
    viewModel: ApplicationListViewModel,
    onNavigateToAddApplication: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadApplications()
    }

    ApplicationListScreen(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        // Pass the navigation callback directly to the screen
        onAddClick = onNavigateToAddApplication
    )
}