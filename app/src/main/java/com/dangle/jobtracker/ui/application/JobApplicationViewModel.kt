package com.dangle.jobtracker.ui.application

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dangle.jobtracker.data.repository.JobApplicationRepository
import com.dangle.jobtracker.domain.model.ApplicationStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

/**
 * ViewModel responsible for managing the state and logic of the Job Application creation screen.
 * 
 * It follows a Unidirectional Data Flow (UDF) pattern:
 * - **State:** Exposed via [uiState] to the view.
 * - **Events:** Received via [onEvent] from the view.
 * - **Effects:** Emitted via [effect] for one-off UI actions like navigation.
 */
@HiltViewModel
class JobApplicationViewModel @Inject constructor (
    private val repository: JobApplicationRepository
) : ViewModel() {

    // Internal mutable state flow for the UI state
    private val _uiState = MutableStateFlow(JobApplicationUiState())
    // Public read-only version of the UI state
    val uiState: StateFlow<JobApplicationUiState> = _uiState.asStateFlow()

    // Channel for one-off side effects (e.g., NavigateBack, ShowError)
    private val _effect = Channel<JobApplicationSideEffect>()
    val effect = _effect.receiveAsFlow()

    /**
     * Primary entry point for UI events.
     * Centralizing event handling makes the logic easier to trace and test.
     */
    fun onEvent(event: JobApplicationEvent) {
        when (event) {
            is JobApplicationEvent.CompanyNameChanged -> {
                _uiState.update { currentState ->
                    currentState.copy(
                        companyName = event.name,
                        companyNameError = null, // Clear error on typing
                        // Re-evaluate submit button state based on new input
                        isSubmitEnabled = event.name.isNotBlank() && currentState.positionTitle.isNotBlank()
                    )
                }
            }
            is JobApplicationEvent.PositionTitleChanged -> {
                _uiState.update { currentState ->
                    currentState.copy(
                        positionTitle = event.title,
                        positionTitleError = null, // Clear error on typing
                        isSubmitEnabled = currentState.companyName.isNotBlank() && event.title.isNotBlank()
                    )
                }
            }
            is JobApplicationEvent.StatusChanged -> {
                _uiState.update { it.copy(selectedStatus = event.status) }
            }
            is JobApplicationEvent.LocationChanged -> {
                _uiState.update { it.copy(location = event.location) }
            }
            is JobApplicationEvent.JobUrlChanged -> {
                _uiState.update { it.copy(jobUrl = event.url) }
            }
            is JobApplicationEvent.NotesChanged -> {
                _uiState.update { it.copy(notes = event.notes) }
            }
            JobApplicationEvent.SaveClicked -> saveApplication()
        }
    }

    /**
     * Triggers the application creation process in the repository.
     * This method handles form locking, repository calls, and navigation effects.
     */
    private fun saveApplication() {
        val currentState = _uiState.value
        // Guard against invalid submissions or multiple clicks
        if (!currentState.isSubmitEnabled || currentState.isSubmitting) return

        // Lock the form and clear any previous errors
        _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }

        viewModelScope.launch {
            val result = repository.createApplication(
                companyName = currentState.companyName,
                positionTitle = currentState.positionTitle,
                status = currentState.selectedStatus,
                appliedDate = LocalDate.now().toString(),
                location = currentState.location,
                jobUrl = currentState.jobUrl,
                notes = currentState.notes
            )

            result.onSuccess {
                _uiState.update { it.copy(isSubmitting = false) }
                // Signal the view to navigate back on success
                _effect.send(JobApplicationSideEffect.NavigateBack)
            }

            result.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isSubmitting = false,
                        errorMessage = error.message ?: "Failed to save application"
                    )
                }
            }
        }
    }
}
