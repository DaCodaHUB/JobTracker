package com.dangle.jobtracker.screen.addapplication

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddApplicationScreen(
    uiState: AddApplicationUiState,
    onEvent: (AddApplicationEvent) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
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
                onValueChange = { onEvent(AddApplicationEvent.CompanyNameChanged(it)) },
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
                onValueChange = { onEvent(AddApplicationEvent.PositionTitleChanged(it)) },
                label = { Text("Position Title") },
                isError = uiState.positionTitleError != null,
                supportingText = {
                    uiState.positionTitleError?.let { Text(it) }
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Button(
                onClick = { onEvent(AddApplicationEvent.Submit) },
                enabled = uiState.isSubmitEnabled,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save Application")
            }
        }
    }
}