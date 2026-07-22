package com.dangle.jobtracker.ui.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dangle.jobtracker.data.repository.JobApplicationRepository
import com.dangle.jobtracker.data.repository.JobApplicationRepositoryImpl
import com.dangle.jobtracker.domain.model.JobApplication
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ApplicationListViewModel(
    private val repository: JobApplicationRepository = JobApplicationRepositoryImpl()
) : ViewModel() {

    private val _uiState = MutableStateFlow(ApplicationListUiState(isLoading = true))
    val uiState: StateFlow<ApplicationListUiState> = _uiState.asStateFlow()
    private var allApplications: List<JobApplication> = emptyList()

    init {
        repository.observeApplications()
            .onEach { applications ->
                allApplications = applications
                applyFilters()
            }
            .launchIn(viewModelScope)

        loadApplications()
    }

    fun loadApplications() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            repository.getApplications()
                .onSuccess { applications ->
                    _uiState.update { it.copy(applications = applications) }
                    allApplications = applications
                    applyFilters()
                }
                .onFailure { error ->
                    // Just clear the loading state on failure.
                    // (You can also add an error message to your UiState if you have an error field)
                    _uiState.update { it.copy(isLoading = false) }
                }
        }
    }

    private fun applyFilters() {
        val query = uiState.value.searchQuery
        val status = uiState.value.selectedStatus

        val filtered = allApplications.filter { app ->
            val matchesSearch = query.isBlank() || app.companyName.contains(query, ignoreCase = true)
            val matchesStatus = status == null || app.status == status
            matchesSearch && matchesStatus
        }

        // Update the list and clear the loading spinner at the same time
        _uiState.update { it.copy(applications = filtered, isLoading = false) }
    }

    fun onEvent(event: ApplicationListEvent) {
        when (event) {
            ApplicationListEvent.Refresh -> {
                loadApplications()
            }
            is ApplicationListEvent.ApplicationClicked -> {
                // TODO: Handle navigating to detail view or item selection
            }
            is ApplicationListEvent.SearchChanged -> {
                // Moved from onFailure: Update state and trigger filter
                _uiState.update { it.copy(searchQuery = event.query) }
                applyFilters()
            }
            is ApplicationListEvent.StatusSelected -> {
                // Filled in the TODO: Update state and trigger filter
                _uiState.update { it.copy(selectedStatus = event.status) }
                applyFilters()
            }
        }
    }
}