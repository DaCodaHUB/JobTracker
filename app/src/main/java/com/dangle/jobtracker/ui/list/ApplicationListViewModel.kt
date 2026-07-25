package com.dangle.jobtracker.ui.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dangle.jobtracker.data.repository.JobApplicationRepository
import com.dangle.jobtracker.domain.model.ApplicationStatus
import com.dangle.jobtracker.domain.model.JobApplication
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

@HiltViewModel
class ApplicationListViewModel @Inject constructor(
    private val repository: JobApplicationRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _selectedStatus = MutableStateFlow<ApplicationStatus?>(null)
    private val _isLoading = MutableStateFlow(false)

    val uiState: StateFlow<ApplicationListUiState> = combine(
        repository.getApplications(),
        _searchQuery,
        _selectedStatus,
        _isLoading
    ) { allApplications, query, status, isLoading ->
        val filtered = allApplications.filter { app ->
            val matchesSearch = query.isBlank() || app.companyName.contains(query, ignoreCase = true)
            val matchesStatus = status == null || app.status == status
            val notDeleted = app.syncStatus != SyncStatus.PENDING_DELETE
            matchesSearch && matchesStatus && notDeleted
        }

        ApplicationListUiState(
            applications = filtered.sortedWith(
                compareByDescending<JobApplication> { it.appliedDate }
                    .thenBy { it.companyName.trim().lowercase() }
                    .thenBy { it.positionTitle.trim().lowercase() }
            ),
            searchQuery = query,
            selectedStatus = status,
            isLoading = isLoading
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ApplicationListUiState(isLoading = true)
    )

    fun syncWithServer() {
        viewModelScope.launch {
            _isLoading.update { true }
            repository.refreshApplications()
            _isLoading.update { false }
        }
    }

    fun onEvent(event: ApplicationListEvent) {
        when (event) {
            ApplicationListEvent.Refresh -> {
                syncWithServer()
            }
            is ApplicationListEvent.ApplicationClicked -> {
                // TODO: Handle navigating to detail view or item selection
            }
            is ApplicationListEvent.SearchChanged -> {
                _searchQuery.update { event.query }
            }
            is ApplicationListEvent.StatusSelected -> {
                _selectedStatus.update { event.status }
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
