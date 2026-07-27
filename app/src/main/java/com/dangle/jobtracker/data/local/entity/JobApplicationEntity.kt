package com.dangle.jobtracker.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.dangle.jobtracker.domain.model.SyncStatus

/**
 * Local database representation of a Job Application.
 * 
 * This model contains the local current state as well as "server snapshot" fields.
 * The server fields are populated only when a sync conflict is detected, allowing 
 * the UI to present a side-by-side comparison for user resolution.
 */
@Entity(tableName = "job_applications")
data class JobApplicationEntity(
    @PrimaryKey
    val id: String,
    val companyName: String,
    val positionTitle: String,
    val status: String,
    val appliedDate: String,
    val location: String = "",
    val jobUrl: String = "",
    val notes: String = "",
    
    /** Indicates whether this item is synced, pending a push, or in conflict. */
    val syncStatus: SyncStatus = SyncStatus.SYNCED,
    
    /** Optimistic locking version provided by the server. */
    val version: Int = 1,
    
    // Server Snapshots: These hold divergent data found on the server during sync
    val serverCompany: String? = null,
    val serverPositionTitle: String? = null,
    val serverStatus: String? = null,
    val serverAppliedDate: String? = null,
    val serverLocation: String? = null,
    val serverJobUrl: String? = null,
    val serverNotes: String? = null,
    val serverVersion: Int? = null
)
