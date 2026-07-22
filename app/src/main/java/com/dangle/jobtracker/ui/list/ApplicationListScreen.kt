// feature/applicationlist/ApplicationListScreen.kt
package com.dangle.jobtracker.ui.list

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dangle.jobtracker.ui.list.components.ApplicationCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApplicationListScreen(
    uiState: ApplicationListUiState,
    onEvent: (ApplicationListEvent) -> Unit,
    onAddClick: () -> Unit, // Add navigation callback
    modifier: Modifier = Modifier
) {
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
                        onClick = { onEvent(ApplicationListEvent.ApplicationClicked(application.id)) }
                    )
                }
            }
        }
    }
}