package com.dangle.jobtracker.ui.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dangle.jobtracker.data.repository.JobApplicationRepository
import com.dangle.jobtracker.domain.model.ApplicationStatus
import com.dangle.jobtracker.domain.model.SyncStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the Application List screen.
 * 
 * It manages the UI state by combining data from the [JobApplicationRepository]
 * with user-defined filters like search queries.
 */
@HiltViewModel
class ApplicationListViewModel @Inject constructor(
    private val repository: JobApplicationRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _isLoading = MutableStateFlow(false)

    /**
     * The combined UI state for the list screen.
     * Logic here focuses on filtering and sorting data already reconciled by the Repository.
     */
    val uiState: StateFlow<ApplicationListUiState> = combine(
        repository.getApplications(),
        _searchQuery,
        _isLoading
    ) { allApplications, query, isLoading ->
        
        // Filter by search query and hide items marked for deletion
        val filtered = allApplications.filter { app ->
            val matchesSearch = query.isBlank() || app.companyName.contains(query, ignoreCase = true)
            val notDeleted = app.syncStatus != SyncStatus.PENDING_DELETE
            matchesSearch && notDeleted
        }

        // Calculate statistics based on all non-deleted applications
        val nonDeletedApps = allApplications.filter { it.syncStatus != SyncStatus.PENDING_DELETE }
        val activeCount = nonDeletedApps.count { it.status != ApplicationStatus.REJECTED }
        val interviewCount = nonDeletedApps.count { it.status == ApplicationStatus.INTERVIEWING }
        val offerCount = nonDeletedApps.count { it.status == ApplicationStatus.OFFER }
        
        val totalCount = nonDeletedApps.size
        val respondedCount = nonDeletedApps.count { it.status != ApplicationStatus.APPLIED }
        val responseRate = if (totalCount > 0) {
            (respondedCount.toFloat() / totalCount * 100).toInt()
        } else 0

        ApplicationListUiState(
            applications = filtered, // Sorting is handled at the DAO level
            statistics = JobStatistics(
                activeCount = activeCount,
                interviewCount = interviewCount,
                responseRate = responseRate,
                offerCount = offerCount
            ),
            searchQuery = query,
            isLoading = isLoading
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ApplicationListUiState(isLoading = true)
    )

    /**
     * Manually triggers a refresh from the server.
     */
    fun syncWithServer() {
        viewModelScope.launch {
            _isLoading.update { true }
            repository.refreshApplications()
            _isLoading.update { false }
        }
    }

    /**
     * Centralized event handler for the list screen.
     */
    fun onEvent(event: ApplicationListEvent) {
        when (event) {
            ApplicationListEvent.Refresh -> {
                syncWithServer()
            }
            is ApplicationListEvent.SearchChanged -> {
                _searchQuery.update { event.query }
            }
            is ApplicationListEvent.DeleteApplication -> {
                viewModelScope.launch {
                    repository.deleteApplication(event.id)
                }
            }
            is ApplicationListEvent.UpdateApplicationStatus -> {
                viewModelScope.launch {
                    repository.updateStatus(event.id, event.status)
                }
            }
            is ApplicationListEvent.UpdateNotes -> {
                viewModelScope.launch {
                    repository.updateNotes(event.id, event.notes)
                }
            }
            is ApplicationListEvent.ResolveKeepLocal -> {
                viewModelScope.launch {
                    repository.resolveKeepMine(event.id)
                }
            }
            is ApplicationListEvent.ResolveKeepServer -> {
                viewModelScope.launch {
                    repository.resolveKeepServer(event.id)
                }
            }
        }
    }
}
