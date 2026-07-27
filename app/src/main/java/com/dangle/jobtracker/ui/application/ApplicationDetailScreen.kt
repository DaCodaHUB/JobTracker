package com.dangle.jobtracker.ui.application

import android.util.Log
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.dangle.jobtracker.domain.model.ApplicationStatus
import com.dangle.jobtracker.domain.model.JobApplication
import com.dangle.jobtracker.ui.list.components.StatusBadge
import com.dangle.jobtracker.util.UrlUtils

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
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Larger Logo in Detail with Letter Placeholder Fallback
                        var isImageLoaded by remember { mutableStateOf(false) }

                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            if (!isImageLoaded) {
                                Text(
                                    text = application.companyName.take(1).uppercase(),
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            val logoUrl = UrlUtils.getLogoUrl(application.jobUrl)
                            if (logoUrl != null) {
                                AsyncImage(
                                    model = logoUrl,
                                    contentDescription = "${application.companyName} logo",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Fit,
                                    onState = { state ->
                                        when (state) {
                                            is coil3.compose.AsyncImagePainter.State.Error -> {
                                                Log.e("ApplicationDetail", "Error loading logo for ${application.companyName}: ${state.result.throwable}")
                                                isImageLoaded = false
                                            }
                                            is coil3.compose.AsyncImagePainter.State.Success -> {
                                                isImageLoaded = true
                                            }
                                            else -> {}
                                        }
                                    }
                                )
                            }
                        }
                        StatusBadge(status = application.status)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Text Shared Element: The company name "flies" into its new position
                    Text(
                        text = application.companyName,
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.sharedElement(
                            sharedContentState = rememberSharedContentState(key = "company-${application.id}"),
                            animatedVisibilityScope = animatedVisibilityScope
                        )
                    )
                    
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
                    
                    if (application.location.isNotBlank()) {
                        DetailItem(label = "Location", value = application.location)
                    }

                    if (application.jobUrl.isNotBlank()) {
                        DetailItem(label = "Job URL", value = application.jobUrl)
                    }

                    if (application.notes.isNotBlank()) {
                        DetailItem(label = "Notes", value = application.notes)
                    }

                    DetailItem(label = "Applied on", value = application.appliedDate)
                }
            }
        }
    }
}

@Composable
private fun DetailItem(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}
