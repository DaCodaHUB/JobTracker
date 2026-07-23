package com.dangle.jobtracker.domain.model

enum class ApplicationStatus {
    APPLIED,
    INTERVIEWING,
    OFFER,
    REJECTED;

    companion object {
        fun fromString(value: String?): ApplicationStatus {
            if (value == null) return APPLIED
            return entries.find {
                it.name.equals(value, ignoreCase = true)
            } ?: APPLIED
        }
    }
}

enum class SyncStatus {
    SYNCED,
    PENDING_CREATE,
    PENDING_UPDATE,
    PENDING_DELETE
}

data class JobApplication(
    val id: String,
    val companyName: String,
    val positionTitle: String,
    val status: ApplicationStatus,
    val appliedDate: String,
    val syncStatus: SyncStatus = SyncStatus.SYNCED
)