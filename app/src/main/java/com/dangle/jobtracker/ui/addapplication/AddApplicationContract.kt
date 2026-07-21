package com.dangle.jobtracker.ui.addapplication

data class AddApplicationUiState(
    val companyName: String = "",
    val positionTitle: String = "",
    val companyNameError: String? = null,
    val positionTitleError: String? = null,
    val isSubmitEnabled: Boolean = false
)

sealed interface AddApplicationEvent {
    data class CompanyNameChanged(val name: String) : AddApplicationEvent
    data class PositionTitleChanged(val title: String) : AddApplicationEvent
    data object Submit : AddApplicationEvent
}