package com.dangle.jobtracker.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.dangle.jobtracker.domain.model.ApplicationStatus
import com.dangle.jobtracker.domain.model.JobApplication
import com.dangle.jobtracker.domain.model.SyncStatus

@Entity(tableName = "job_applications")
data class JobApplicationEntity(
    @PrimaryKey
    val id: String,
    val companyName: String,
    val positionTitle: String,
    val status: String,
    val appliedDate: String,
    val syncStatus: SyncStatus = SyncStatus.SYNCED
)

fun JobApplicationEntity.toDomainModel(): JobApplication {
    return JobApplication(
        id = id,
        companyName = companyName,
        positionTitle = positionTitle,
        status = ApplicationStatus.fromString(status),
        appliedDate = appliedDate,
        syncStatus = syncStatus
    )
}
