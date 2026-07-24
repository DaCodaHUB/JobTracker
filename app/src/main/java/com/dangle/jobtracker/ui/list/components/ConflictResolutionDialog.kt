package com.dangle.jobtracker.ui.list.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dangle.jobtracker.domain.model.ApplicationStatus
import com.dangle.jobtracker.domain.model.JobApplication
import com.dangle.jobtracker.domain.model.SyncStatus

@Composable
fun ConflictResolutionDialog(
    localApp: JobApplication,
    serverApp: JobApplication,
    onKeepLocal: () -> Unit,
    onKeepServer: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "Sync Conflict")
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "The server has a different version of this application. Which one should we keep?",
                    style = MaterialTheme.typography.bodyMedium
                )

                ConflictHeader()
                
                ConflictRow(
                    label = "Company",
                    localValue = localApp.companyName,
                    serverValue = serverApp.companyName
                )
                
                ConflictRow(
                    label = "Position",
                    localValue = localApp.positionTitle,
                    serverValue = serverApp.positionTitle
                )
                
                ConflictStatusRow(
                    localStatus = localApp.status,
                    serverStatus = serverApp.status
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onKeepServer) {
                Text("Keep Server")
            }
        },
        dismissButton = {
            TextButton(onClick = onKeepLocal) {
                Text("Keep Mine")
            }
        },
        modifier = modifier
    )
}

@Composable
private fun ConflictHeader() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Field",
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.weight(1f),
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Local",
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.weight(1.5f),
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Server",
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.weight(1.5f),
            color = MaterialTheme.colorScheme.secondary,
            fontWeight = FontWeight.Bold
        )
    }
    HorizontalDivider(modifier = Modifier.padding(top = 4.dp))
}

@Composable
private fun ConflictRow(
    label: String,
    localValue: String,
    serverValue: String
) {
    val isDifferent = localValue != serverValue
    val contentColor = if (isDifferent) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.weight(1f),
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = localValue,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1.5f),
            color = contentColor
        )
        Text(
            text = serverValue,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1.5f),
            color = contentColor
        )
    }
}

@Composable
private fun ConflictStatusRow(
    localStatus: ApplicationStatus,
    serverStatus: ApplicationStatus
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Status",
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.weight(1f),
            fontWeight = FontWeight.SemiBold
        )
        StatusBadge(
            status = localStatus,
            modifier = Modifier.weight(1.5f)
        )
        StatusBadge(
            status = serverStatus,
            modifier = Modifier.weight(1.5f)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ConflictResolutionDialogPreview() {
    MaterialTheme {
        ConflictResolutionDialog(
            localApp = JobApplication(
                id = "1",
                companyName = "Google (Modified)",
                positionTitle = "Android Dev",
                status = ApplicationStatus.INTERVIEWING,
                appliedDate = "2024-01-01",
                syncStatus = SyncStatus.CONFLICT,
                version = 2
            ),
            serverApp = JobApplication(
                id = "1",
                companyName = "Google",
                positionTitle = "Senior Android Dev",
                status = ApplicationStatus.APPLIED,
                appliedDate = "2024-01-01",
                syncStatus = SyncStatus.SYNCED,
                version = 3
            ),
            onKeepLocal = {},
            onKeepServer = {},
            onDismiss = {}
        )
    }
}
