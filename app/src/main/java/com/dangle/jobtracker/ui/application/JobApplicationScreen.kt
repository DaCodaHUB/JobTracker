package com.dangle.jobtracker.ui.application

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dangle.jobtracker.domain.model.ApplicationStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JobApplicationScreen(
    uiState: JobApplicationUiState,
    snackbarHostState: SnackbarHostState,
    onEvent: (JobApplicationEvent) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Text(
                        "Add application",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cancel"
                        )
                    }
                },
                actions = {
                    TextButton(
                        onClick = { onEvent(JobApplicationEvent.SaveClicked) },
                        enabled = uiState.isSubmitEnabled && !uiState.isSubmitting
                    ) {
                        if (uiState.isSubmitting) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        } else {
                            Text("Save")
                        }
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
                .consumeWindowInsets(innerPadding)
                .imePadding()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            val fieldShape = RoundedCornerShape(12.dp)

            // Company Field
            FormSection(label = "Company") {
                OutlinedTextField(
                    value = uiState.companyName,
                    onValueChange = { onEvent(JobApplicationEvent.CompanyNameChanged(it)) },
                    placeholder = { Text("Company name", color = Color.Gray) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = fieldShape,
                    isError = uiState.companyNameError != null,
                    singleLine = true
                )
            }

            // Role Field
            FormSection(label = "Role") {
                OutlinedTextField(
                    value = uiState.positionTitle,
                    onValueChange = { onEvent(JobApplicationEvent.PositionTitleChanged(it)) },
                    placeholder = { Text("Job title", color = Color.Gray) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = fieldShape,
                    isError = uiState.positionTitleError != null,
                    singleLine = true
                )
            }

            // Status Field
            FormSection(label = "Status") {
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = uiState.selectedStatus.name.lowercase().replaceFirstChar { it.uppercase() },
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                        shape = fieldShape,
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
                            .fillMaxWidth()
                    )

                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        ApplicationStatus.entries.forEach { status ->
                            DropdownMenuItem(
                                text = { Text(status.name.lowercase().replaceFirstChar { it.uppercase() }) },
                                onClick = {
                                    onEvent(JobApplicationEvent.StatusChanged(status))
                                    expanded = false
                                },
                                contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                            )
                        }
                    }
                }
            }

            // Location Field
            FormSection(label = "Location") {
                OutlinedTextField(
                    value = uiState.location,
                    onValueChange = { onEvent(JobApplicationEvent.LocationChanged(it)) },
                    placeholder = { Text("e.g. Remote or New York, NY", color = Color.Gray) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = fieldShape,
                    singleLine = true
                )
            }

            // Job URL Field
            FormSection(label = "Job URL") {
                OutlinedTextField(
                    value = uiState.jobUrl,
                    onValueChange = { onEvent(JobApplicationEvent.JobUrlChanged(it)) },
                    placeholder = { Text("https://company.com/careers/123", color = Color.Gray) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = fieldShape,
                    singleLine = true
                )
            }

            // Notes Field
            FormSection(label = "Notes") {
                OutlinedTextField(
                    value = uiState.notes,
                    onValueChange = { onEvent(JobApplicationEvent.NotesChanged(it)) },
                    placeholder = { Text("Add notes about this application...", color = Color.Gray) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 100.dp),
                    shape = fieldShape,
                    maxLines = 5
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun FormSection(
    label: String,
    content: @Composable () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        content()
    }
}
