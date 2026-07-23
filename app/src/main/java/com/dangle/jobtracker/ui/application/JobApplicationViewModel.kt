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

@HiltViewModel
class JobApplicationViewModel @Inject constructor (
    private val repository: JobApplicationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(JobApplicationUiState())
    val uiState: StateFlow<JobApplicationUiState> = _uiState.asStateFlow()

    // Single source of truth for UI effects (navigation, snackbars)
    private val _effect = Channel<JobApplicationSideEffect>()
    val effect = _effect.receiveAsFlow()

    fun onEvent(event: JobApplicationEvent) {
        when (event) {
            is JobApplicationEvent.CompanyNameChanged -> {
                _uiState.update { currentState ->
                    currentState.copy(
                        companyName = event.name,
                        companyNameError = null, // Clear error on typing
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
            JobApplicationEvent.SaveClicked -> saveApplication()
        }
    }

    private fun saveApplication() {
        val currentState = _uiState.value
        if (!currentState.isSubmitEnabled || currentState.isSubmitting) return

        // Lock the form while submitting and clear any previous error
        _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }

        viewModelScope.launch {
            val result = repository.createApplication(
                companyName = currentState.companyName,
                positionTitle = currentState.positionTitle,
                status = ApplicationStatus.APPLIED,
                appliedDate = LocalDate.now().toString()
            )

            result.onSuccess {
                _uiState.update { it.copy(isSubmitting = false) }
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
