// feature/applicationlist/ApplicationListViewModel.kt
package com.dangle.jobtracker.ui.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dangle.jobtracker.data.repository.JobApplicationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ApplicationListViewModel(
    private val repository: JobApplicationRepository = JobApplicationRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(ApplicationListUiState(isLoading = true))
    val uiState: StateFlow<ApplicationListUiState> = _uiState.asStateFlow()

    init {
        loadApplications()
    }

    fun loadApplications() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            repository.getApplications()
                .onSuccess { applications ->
                    _uiState.update {
                        it.copy(
                            applications = applications,
                            isLoading = false,
                            errorMessage = null
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.localizedMessage
                        )
                    }
                }
        }
    }

    fun onEvent(event: ApplicationListEvent) {
        when (event) {
            ApplicationListEvent.Refresh -> {
                loadApplications()
            }
            ApplicationListEvent.AddApplicationClicked -> {
                // Handled via UI navigation callback or side-effect channel
            }
            is ApplicationListEvent.ApplicationClicked -> {
                // TODO: Handle navigating to detail view or item selection
            }
            is ApplicationListEvent.SearchChanged -> {
                // TODO: Update search query filter in UiState
            }
            is ApplicationListEvent.StatusSelected -> {
                // TODO: Update status filter in UiState
            }
        }
    }
}