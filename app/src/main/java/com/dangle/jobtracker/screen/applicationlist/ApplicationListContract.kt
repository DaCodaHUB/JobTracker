// feature/applicationlist/ApplicationListContract.kt
package com.dangle.jobtracker.screen.applicationlist

import com.dangle.jobtracker.model.ApplicationStatus
import com.dangle.jobtracker.model.JobApplication

data class ApplicationListUiState(
    val applications: List<JobApplication> = emptyList(),
    val searchQuery: String = "",
    val selectedStatus: ApplicationStatus? = null,
    val isLoading: Boolean = false
)

sealed interface ApplicationListEvent {
    data class SearchChanged(val query: String) : ApplicationListEvent
    data class StatusSelected(val status: ApplicationStatus?) : ApplicationListEvent
    data class ApplicationClicked(val id: String) : ApplicationListEvent
    data object AddApplicationClicked : ApplicationListEvent
}