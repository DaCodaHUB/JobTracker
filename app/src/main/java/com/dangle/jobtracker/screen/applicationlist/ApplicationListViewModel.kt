// feature/applicationlist/ApplicationListViewModel.kt
package com.dangle.jobtracker.screen.applicationlist

import androidx.lifecycle.ViewModel
import com.dangle.jobtracker.model.ApplicationStatus
import com.dangle.jobtracker.model.JobApplication
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class ApplicationListViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(ApplicationListUiState())
    val uiState: StateFlow<ApplicationListUiState> = _uiState.asStateFlow()

    init {
        // Mock initial data load
        _uiState.update {
            it.copy(
                applications = listOf(
                    JobApplication("1", "Acme Corp", "Android Developer", ApplicationStatus.INTERVIEWING, "2026-07-15"),
                    JobApplication("2", "Tech Inc", "Senior Mobile Engineer", ApplicationStatus.APPLIED, "2026-07-18")
                )
            )
        }
    }

    fun onEvent(event: ApplicationListEvent) {
        when (event) {
            is ApplicationListEvent.SearchChanged -> {
                _uiState.update { it.copy(searchQuery = event.query) }
            }
            is ApplicationListEvent.StatusSelected -> {
                _uiState.update { it.copy(selectedStatus = event.status) }
            }
            is ApplicationListEvent.ApplicationClicked -> { /* Handled via navigation */ }
            ApplicationListEvent.AddApplicationClicked -> { /* Handled via navigation */ }
        }
    }
}