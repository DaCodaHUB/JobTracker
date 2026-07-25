package com.dangle.jobtracker.ui.application

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dangle.jobtracker.domain.model.ApplicationStatus
import com.dangle.jobtracker.domain.model.JobApplication
import com.dangle.jobtracker.ui.list.components.StatusBadge

/**
 * Displays the full details of a specific job application.
 * 
 * This screen supports:
 * 1. Seamless Shared Element Transitions from the list via [SharedTransitionScope].
 * 2. In-place status management using a Material 3 [ExposedDropdownMenuBox].
 * 
 * @param application The data model to display.
 * @param animatedVisibilityScope Scope for coordinating Shared Element animations.
 * @param onStatusChange Callback when the user modifies the application status.
 * @param onBackClick Callback to return to the list screen.
 */
@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SharedTransitionScope.ApplicationDetailScreen(
    application: JobApplication,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onStatusChange: (ApplicationStatus) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Application Detail") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
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
                .padding(16.dp)
        ) {
            // Container Shared Element: Uses sharedBounds to animate the card expansion
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .sharedBounds(
                        sharedContentState = rememberSharedContentState(key = "card-${application.id}"),
                        animatedVisibilityScope = animatedVisibilityScope
                    )
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Text Shared Element: The company name "flies" into its new position
                        Text(
                            text = application.companyName,
                            style = MaterialTheme.typography.headlineMedium,
                            modifier = Modifier.sharedElement(
                                sharedContentState = rememberSharedContentState(key = "company-${application.id}"),
                                animatedVisibilityScope = animatedVisibilityScope
                            )
                        )
                        StatusBadge(status = application.status)
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Text Shared Element: The position title "flies" into its new position
                    Text(
                        text = application.positionTitle,
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.sharedElement(
                            sharedContentState = rememberSharedContentState(key = "position-${application.id}"),
                            animatedVisibilityScope = animatedVisibilityScope
                        )
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "Current Status",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))

                    // Material 3 Status Picker: Allows immediate local update via callback
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = application.status.name,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
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
                                    text = { Text(status.name) },
                                    onClick = {
                                        onStatusChange(status)
                                        expanded = false
                                    },
                                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Text(
                        text = "Applied on: ${application.appliedDate}",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
    }
}
