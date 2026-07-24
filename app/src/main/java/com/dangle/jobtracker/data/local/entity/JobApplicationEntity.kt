package com.dangle.jobtracker.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.dangle.jobtracker.domain.model.SyncStatus

@Entity(tableName = "job_applications")
data class JobApplicationEntity(
    @PrimaryKey
    val id: String,
    val companyName: String,
    val positionTitle: String,
    val status: String,
    val appliedDate: String,
    val syncStatus: SyncStatus = SyncStatus.SYNCED,
    val version: Int = 1,
    val serverCompany: String? = null,
    val serverPositionTitle: String? = null,
    val serverStatus: String? = null,
    val serverAppliedDate: String? = null,
    val serverVersion: Int? = null
)
