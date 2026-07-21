package com.dangle.jobtracker.model

// model/JobApplication.kt
enum class ApplicationStatus {
    APPLIED, INTERVIEWING, OFFERED, REJECTED
}

data class JobApplication(
    val id: String,
    val companyName: String,
    val positionTitle: String,
    val status: ApplicationStatus,
    val appliedDate: String
)