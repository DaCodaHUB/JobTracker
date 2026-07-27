// feature/applicationlist/ApplicationListContract.kt
package com.dangle.jobtracker.ui.list

import com.dangle.jobtracker.domain.model.ApplicationStatus
import com.dangle.jobtracker.domain.model.JobApplication

data class JobStatistics(
    val activeCount: Int = 0,
    val interviewCount: Int = 0,
    val responseRate: Int = 0,
    val offerCount: Int = 0
)

data class ApplicationListUiState(
    val applications: List<JobApplication> = emptyList(),
    val statistics: JobStatistics = JobStatistics(),
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

sealed interface ApplicationListEvent {
    data class SearchChanged(val query: String) : ApplicationListEvent
    data class DeleteApplication(val id: String) : ApplicationListEvent
    data class UpdateApplicationStatus(val id: String, val status: ApplicationStatus) : ApplicationListEvent
    data class UpdateNotes(val id: String, val notes: String) : ApplicationListEvent
    data object Refresh : ApplicationListEvent
    data class ResolveKeepLocal(val id: String) : ApplicationListEvent
    data class ResolveKeepServer(val id: String) : ApplicationListEvent
}