package com.dangle.jobtracker.ui.list

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dangle.jobtracker.domain.model.JobApplication
import com.dangle.jobtracker.domain.model.SyncStatus
import com.dangle.jobtracker.ui.list.components.ApplicationCard
import com.dangle.jobtracker.ui.list.components.ConflictResolutionDialog

/**
 * The primary list screen for job applications.
 * 
 * This composable manages the high-level UI state for the list, including:
 * 1. Searching/filtering through the applications.
 * 2. Handling deletion confirmations via an [AlertDialog].
 * 3. Displaying conflict resolution comparisons using [ConflictResolutionDialog].
 * 4. Utilizing [SharedTransitionScope] for smooth animations to the detail screen.
 *
 * @param uiState Current UI state containing the list of applications and search query.
 * @param sharedTransitionScope The scope for shared element transitions.
 * @param animatedVisibilityScope Scope for coordinating visibility-based animations.
 * @param onEvent Callback for business events (e.g., deletions, search changes).
 * @param onItemClick Callback when an application is tapped to view details.
 * @param onAddClick Callback to navigate to the "Add Application" screen.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun ApplicationListScreen(
    uiState: ApplicationListUiState,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onEvent: (ApplicationListEvent) -> Unit,
    onItemClick: (String) -> Unit,
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // UI-only state for managing dialog visibility
    var applicationToResolve by remember { mutableStateOf<JobApplication?>(null) }
    var applicationToDelete by remember { mutableStateOf<JobApplication?>(null) }

    // Deletion confirmation workflow
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

    // Sync conflict resolution workflow
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
            // Search Input Field: Updates the UI state immediately on change
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = { onEvent(ApplicationListEvent.SearchChanged(it)) },
                label = { Text("Search applications") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // Optimized list with unique keys for recomposition performance
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(
                    items = uiState.applications,
                    key = { application -> application.id }
                ) { application ->
                    ApplicationCard(
                        application = application,
                        sharedTransitionScope = sharedTransitionScope,
                        animatedVisibilityScope = animatedVisibilityScope,
                        onClick = {
                            // Intercept clicks on conflicted items to show the resolution dialog
                            if (application.syncStatus == SyncStatus.CONFLICT) {
                                applicationToResolve = application
                            } else {
                                onItemClick(application.id)
                            }
                        },
                        onDelete = {
                            applicationToDelete = application
                        }
                    )
                }
            }
        }
    }
}
