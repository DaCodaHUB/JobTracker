package com.dangle.jobtracker.ui.application

import android.util.Log
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.dangle.jobtracker.domain.model.ApplicationStatus
import com.dangle.jobtracker.domain.model.JobApplication
import com.dangle.jobtracker.ui.application.components.TimelineView
import com.dangle.jobtracker.ui.list.components.StatusBadge
import com.dangle.jobtracker.util.UrlUtils

/**
 * Displays the full details of a specific job application.
 */
@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SharedTransitionScope.ApplicationDetailScreen(
    application: JobApplication,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onStatusChange: (ApplicationStatus) -> Unit,
    onNotesChange: (String) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uriHandler = LocalUriHandler.current

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
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Profile Summary Card
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .sharedBounds(
                        sharedContentState = rememberSharedContentState(key = "card-${application.id}"),
                        animatedVisibilityScope = animatedVisibilityScope
                    )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Logo
                        CompanyLogo(
                            companyName = application.companyName,
                            jobUrl = application.jobUrl,
                            size = 64.dp
                        )

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = application.companyName,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.sharedElement(
                                    sharedContentState = rememberSharedContentState(key = "company-${application.id}"),
                                    animatedVisibilityScope = animatedVisibilityScope
                                )
                            )
                            Text(
                                text = application.positionTitle,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.sharedElement(
                                    sharedContentState = rememberSharedContentState(key = "position-${application.id}"),
                                    animatedVisibilityScope = animatedVisibilityScope
                                )
                            )
                        }
                        StatusBadge(status = application.status)
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

                    // Info List
                    InfoRow(icon = Icons.Default.DateRange, label = "Applied on", value = application.appliedDate)
                    InfoRow(icon = Icons.Default.LocationOn, label = "Location", value = application.location.ifBlank { "Not specified" })
                    InfoRow(
                        icon = Icons.Default.Info,
                        label = "Job Posting",
                        value = application.jobUrl.ifBlank { "No link provided" },
                        isLink = application.jobUrl.isNotBlank(),
                        onClick = { if (application.jobUrl.isNotBlank()) uriHandler.openUri(application.jobUrl) }
                    )
                }
            }

            // 2. Interactive Timeline Card
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                TimelineView(
                    currentStatus = application.status,
                    onStatusClick = onStatusChange,
                    modifier = Modifier.padding(16.dp)
                )
            }

            // 3. Notes Card
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                var isEditing by remember { mutableStateOf(false) }
                var editedNotes by remember(application.notes) { mutableStateOf(application.notes) }

                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Notes",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (!isEditing) {
                            IconButton(onClick = { isEditing = true }) {
                                Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit Notes")
                            }
                        }
                    }

                    if (isEditing) {
                        OutlinedTextField(
                            value = editedNotes,
                            onValueChange = { editedNotes = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Add notes about this application...") }
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = { 
                                isEditing = false
                                editedNotes = application.notes 
                            }) {
                                Text("Cancel")
                            }
                            TextButton(onClick = { 
                                onNotesChange(editedNotes)
                                isEditing = false 
                            }) {
                                Text("Save")
                            }
                        }
                    } else {
                        Text(
                            text = application.notes.ifBlank { "Add notes about this application..." },
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (application.notes.isBlank()) MaterialTheme.colorScheme.outline else Color.Unspecified
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun CompanyLogo(companyName: String, jobUrl: String, size: androidx.compose.ui.unit.Dp) {
    var isImageLoaded by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        if (!isImageLoaded) {
            Text(
                text = companyName.take(1).uppercase(),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        val logoUrl = UrlUtils.getLogoUrl(jobUrl)
        if (logoUrl != null) {
            AsyncImage(
                model = logoUrl,
                contentDescription = "$companyName logo",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit,
                onState = { state ->
                    isImageLoaded = state is coil3.compose.AsyncImagePainter.State.Success
                }
            )
        }
    }
}

@Composable
private fun InfoRow(
    icon: ImageVector,
    label: String,
    value: String,
    isLink: Boolean = false,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.outline
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = "$label:",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.width(100.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = if (isLink) MaterialTheme.colorScheme.primary else Color.Unspecified,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .clickable(enabled = isLink, onClick = onClick)
        )
    }
}
