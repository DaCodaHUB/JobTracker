package com.dangle.jobtracker.domain.model

/**
 * Represents the current stage of a job application process.
 */
enum class ApplicationStatus {
    SAVED,
    APPLIED,
    INTERVIEWING,
    OFFER,
    REJECTED;

    companion object {
        /**
         * Safely parses a string into an [ApplicationStatus], defaulting to [APPLIED].
         */
        fun fromString(value: String?): ApplicationStatus {
            if (value == null) return APPLIED
            return entries.find {
                it.name.equals(value, ignoreCase = true)
            } ?: APPLIED
        }
    }
}

/**
 * Defines the synchronization state of a domain object relative to the server.
 */
enum class SyncStatus {
    /** Item matches the server state. */
    SYNCED,
    /** Item exists only locally and needs to be created on the server. */
    PENDING_CREATE,
    /** Item was updated locally and needs the change pushed to the server. */
    PENDING_UPDATE,
    /** Item was deleted locally and needs to be removed from the server. */
    PENDING_DELETE,
    /** Divergent state detected between local and server that requires resolution. */
    CONFLICT
}

/**
 * Domain model for a Job Application.
 * This is the primary data structure used across the UI and Business logic layers.
 */
data class JobApplication(
    val id: String,
    val companyName: String,
    val positionTitle: String,
    val status: ApplicationStatus,
    val appliedDate: String,
    val location: String = "",
    val jobUrl: String = "",
    val notes: String = "",
    val idempotencyKey: String = "",
    val syncStatus: SyncStatus = SyncStatus.SYNCED,
    val version: Int = 1,
    
    // Server state fields used for conflict resolution comparisons
    val serverCompany: String? = null,
    val serverPositionTitle: String? = null,
    val serverStatus: ApplicationStatus? = null,
    val serverAppliedDate: String? = null,
    val serverLocation: String? = null,
    val serverJobUrl: String? = null,
    val serverNotes: String? = null,
    val serverIdempotencyKey: String? = null,
    val serverVersion: Int? = null
)
