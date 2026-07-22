package com.dangle.jobtracker.ui.application

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JobApplicationScreen(
    uiState: JobApplicationUiState,
    snackbarHostState: SnackbarHostState,
    onEvent: (JobApplicationEvent) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Add New Application") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Navigate back"
                        )
                    }
                }
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = uiState.companyName,
                onValueChange = { onEvent(JobApplicationEvent.CompanyNameChanged(it)) },
                label = { Text("Company Name") },
                isError = uiState.companyNameError != null,
                supportingText = {
                    uiState.companyNameError?.let { Text(it) }
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = uiState.positionTitle,
                onValueChange = { onEvent(JobApplicationEvent.PositionTitleChanged(it)) },
                label = { Text("Position Title") },
                isError = uiState.positionTitleError != null,
                supportingText = {
                    uiState.positionTitleError?.let { Text(it) }
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Button(
                onClick = { onEvent(JobApplicationEvent.SaveClicked) },
                // Prevent double-submissions while loading
                enabled = uiState.isSubmitEnabled && !uiState.isSubmitting,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (uiState.isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Save Application")
                }
            }
        }
    }
}