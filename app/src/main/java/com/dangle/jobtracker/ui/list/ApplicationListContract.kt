// feature/applicationlist/ApplicationListContract.kt
package com.dangle.jobtracker.ui.list

import com.dangle.jobtracker.domain.model.ApplicationStatus
import com.dangle.jobtracker.domain.model.JobApplication

data class ApplicationListUiState(
    val applications: List<JobApplication> = emptyList(),
    val searchQuery: String = "",
    val selectedStatus: ApplicationStatus? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

sealed interface ApplicationListEvent {
    data class SearchChanged(val query: String) : ApplicationListEvent
    data class StatusSelected(val status: ApplicationStatus?) : ApplicationListEvent
    data class ApplicationClicked(val id: String) : ApplicationListEvent
    data object Refresh : ApplicationListEvent
}