// feature/applicationlist/ApplicationListScreen.kt
package com.dangle.jobtracker.ui.list

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dangle.jobtracker.domain.model.JobApplication
import com.dangle.jobtracker.domain.model.SyncStatus
import com.dangle.jobtracker.ui.list.components.ApplicationCard
import com.dangle.jobtracker.ui.list.components.ConflictResolutionDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApplicationListScreen(
    uiState: ApplicationListUiState,
    onEvent: (ApplicationListEvent) -> Unit,
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var applicationToResolve by remember { mutableStateOf<JobApplication?>(null) }
    var applicationToDelete by remember { mutableStateOf<JobApplication?>(null) }

    if (applicationToDelete != null) {
        AlertDialog(
            onDismissRequest = { applicationToDelete = null },
            title = { Text("Delete Application") },
            text = { Text("Are you sure you want to delete the application for ${applicationToDelete?.companyName}?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        applicationToDelete?.let { onEvent(ApplicationListEvent.DeleteApplication(it.id)) }
                        applicationToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { applicationToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (applicationToResolve != null) {
        val app = applicationToResolve!!
        ConflictResolutionDialog(
            localApp = app,
            serverApp = app.copy(
                companyName = app.serverCompany ?: app.companyName,
                positionTitle = app.serverPositionTitle ?: app.positionTitle,
                status = app.serverStatus ?: app.status,
                appliedDate = app.serverAppliedDate ?: app.appliedDate,
                version = app.serverVersion ?: app.version
            ),
            onKeepLocal = {
                onEvent(ApplicationListEvent.ResolveKeepLocal(app.id))
                applicationToResolve = null
            },
            onKeepServer = {
                onEvent(ApplicationListEvent.ResolveKeepServer(app.id))
                applicationToResolve = null
            },
            onDismiss = { applicationToResolve = null }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Job Applications") }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddClick
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Application"
                )
            }
        },
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Search Input Field
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = { onEvent(ApplicationListEvent.SearchChanged(it)) },
                label = { Text("Search applications") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // Optimized Lazy List with Stable Keys
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(
                    items = uiState.applications,
                    key = { application -> application.id } // Stable key for recomposition performance
                ) { application ->
                    ApplicationCard(
                        application = application,
                        onClick = {
                            if (application.syncStatus == SyncStatus.CONFLICT) {
                                applicationToResolve = application
                            } else {
                                onEvent(ApplicationListEvent.ApplicationClicked(application.id))
                            }
                        },
                        onDelete = {
                            applicationToDelete = application
                        },
                        onStatusChange = { newStatus ->
                            onEvent(ApplicationListEvent.UpdateApplicationStatus(application.id, newStatus))
                        }
                    )
                }
            }
        }
    }
}