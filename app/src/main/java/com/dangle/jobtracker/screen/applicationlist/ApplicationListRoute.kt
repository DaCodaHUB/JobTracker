package com.dangle.jobtracker.screen.applicationlist

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun ApplicationListRoute(
    onAddClick: () -> Unit,
    viewModel: ApplicationListViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ApplicationListScreen(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        onAddClick = onAddClick
    )
}