package com.dangle.jobtracker.ui.list.components

import android.util.Log
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
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
import com.dangle.jobtracker.domain.model.SyncStatus
import com.dangle.jobtracker.util.UrlUtils

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun ApplicationCard(
    application: JobApplication,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    with(sharedTransitionScope) {
        OutlinedCard(
            onClick = onClick,
            modifier = modifier
                .fillMaxWidth()
                .sharedBounds(
                    sharedContentState = rememberSharedContentState(key = "card-${application.id}"),
                    animatedVisibilityScope = animatedVisibilityScope
                )
        ) {
            Row(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Company Logo with Letter Placeholder Fallback
                var isImageLoaded by remember { mutableStateOf(false) }

                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    if (!isImageLoaded) {
                        Text(
                            text = application.companyName.take(1).uppercase(),
                            style = MaterialTheme.typography.titleMedium,
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
                                        Log.e("ApplicationCard", "Error loading logo for ${application.companyName}: ${state.result.throwable}")
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

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = application.companyName,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.sharedElement(
                                sharedContentState = rememberSharedContentState(key = "company-${application.id}"),
                                animatedVisibilityScope = animatedVisibilityScope
                            )
                        )
                        StatusBadge(status = application.status)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = application.positionTitle,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.sharedElement(
                            sharedContentState = rememberSharedContentState(key = "position-${application.id}"),
                            animatedVisibilityScope = animatedVisibilityScope
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Text(
                            text = "Applied: ${application.appliedDate}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                        if (application.syncStatus == SyncStatus.CONFLICT) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Conflict",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.error
                            )
                        } else if (application.syncStatus != SyncStatus.SYNCED) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Syncing",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }
            }
        }
    }
}
