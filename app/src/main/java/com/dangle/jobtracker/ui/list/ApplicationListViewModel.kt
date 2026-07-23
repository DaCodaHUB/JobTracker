package com.dangle.jobtracker.ui.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dangle.jobtracker.data.repository.JobApplicationRepository
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
class ApplicationListViewModel @Inject constructor (
    private val repository: JobApplicationRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _selectedStatus = MutableStateFlow<com.dangle.jobtracker.domain.model.ApplicationStatus?>(null)
    private val _isLoading = MutableStateFlow(false)

    val uiState: StateFlow<ApplicationListUiState> = combine(
        repository.getApplications(),
        _searchQuery,
        _selectedStatus,
        _isLoading
    ) { allApplications, query, status, isLoading ->
        val reconciled = reconcileApplications(allApplications)
        
        val filtered = reconciled.filter { app ->
            val matchesSearch = query.isBlank() || app.companyName.contains(query, ignoreCase = true)
            val matchesStatus = status == null || app.status == status
            matchesSearch && matchesStatus
        }

        ApplicationListUiState(
            applications = filtered.sortedByDescending { it.appliedDate },
            searchQuery = query,
            selectedStatus = status,
            isLoading = isLoading
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ApplicationListUiState(isLoading = true)
    )

    private fun reconcileApplications(all: List<JobApplication>): List<JobApplication> {
        val synced = all.filter { it.syncStatus == SyncStatus.SYNCED }
        val pending = all.filter { it.syncStatus != SyncStatus.SYNCED }

        // Hide pending items that match a synced one (robust comparison)
        val uniquePending = pending.filter { p ->
            synced.none { s ->
                s.companyName.trim().equals(p.companyName.trim(), ignoreCase = true) &&
                s.positionTitle.trim().equals(p.positionTitle.trim(), ignoreCase = true)
            }
        }
        return synced + uniquePending
    }

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
        }
    }
}
