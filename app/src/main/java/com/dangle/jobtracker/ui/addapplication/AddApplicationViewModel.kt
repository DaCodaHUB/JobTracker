package com.dangle.jobtracker.ui.addapplication

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class AddApplicationViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(AddApplicationUiState())
    val uiState: StateFlow<AddApplicationUiState> = _uiState.asStateFlow()

    fun onEvent(event: AddApplicationEvent) {
        when (event) {
            is AddApplicationEvent.CompanyNameChanged -> {
                _uiState.update { currentState ->
                    val error = if (event.name.isBlank()) "Company name cannot be empty" else null
                    currentState.copy(
                        companyName = event.name,
                        companyNameError = error,
                        isSubmitEnabled = event.name.isNotBlank() && currentState.positionTitle.isNotBlank()
                    )
                }
            }
            is AddApplicationEvent.PositionTitleChanged -> {
                _uiState.update { currentState ->
                    val error = if (event.title.isBlank()) "Position title cannot be empty" else null
                    currentState.copy(
                        positionTitle = event.title,
                        positionTitleError = error,
                        isSubmitEnabled = currentState.companyName.isNotBlank() && event.title.isNotBlank()
                    )
                }
            }
            AddApplicationEvent.Submit -> {
                // Submit logic will integrate with GraphQL on Day 4
            }
        }
    }
}