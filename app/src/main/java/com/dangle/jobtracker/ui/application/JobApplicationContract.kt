// JobApplicationContract.kt
package com.dangle.jobtracker.ui.application

data class JobApplicationUiState(
    val companyName: String = "",
    val positionTitle: String = "",
    val companyNameError: String? = null,
    val positionTitleError: String? = null,
    val isSubmitEnabled: Boolean = false,
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null
)

sealed interface JobApplicationEvent {
    data class CompanyNameChanged(val name: String) : JobApplicationEvent
    data class PositionTitleChanged(val title: String) : JobApplicationEvent
    data object SaveClicked : JobApplicationEvent
}

sealed interface JobApplicationSideEffect {
    data object NavigateBack : JobApplicationSideEffect
    data class ShowError(val message: String) : JobApplicationSideEffect
}