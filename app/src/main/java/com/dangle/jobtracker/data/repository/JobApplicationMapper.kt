package com.dangle.jobtracker.data.repository

import com.dangle.jobtracker.CreateJobApplicationMutation
import com.dangle.jobtracker.GetJobApplicationQuery
import com.dangle.jobtracker.GetJobApplicationsQuery
import com.dangle.jobtracker.data.local.entity.JobApplicationEntity
import com.dangle.jobtracker.domain.model.ApplicationStatus
import com.dangle.jobtracker.domain.model.JobApplication
import com.dangle.jobtracker.domain.model.SyncStatus

fun GetJobApplicationsQuery.JobApplication.toEntity(): JobApplicationEntity {
    return JobApplicationEntity(
        id = id,
        companyName = companyName,
        positionTitle = positionTitle,
        status = status,
        appliedDate = appliedDate,
        location = location ?: "",
        jobUrl = jobUrl ?: "",
        notes = notes ?: "",
        idempotencyKey = idempotencyKey ?: "",
        syncStatus = SyncStatus.SYNCED,
        version = version,
        serverCompany = null,
        serverPositionTitle = null,
        serverStatus = null,
        serverAppliedDate = null,
        serverLocation = null,
        serverJobUrl = null,
        serverNotes = null,
        serverIdempotencyKey = null,
        serverVersion = null
    )
}

fun GetJobApplicationQuery.JobApplication.toEntity(): JobApplicationEntity {
    return JobApplicationEntity(
        id = id,
        companyName = companyName,
        positionTitle = positionTitle,
        status = status,
        appliedDate = appliedDate,
        location = location ?: "",
        jobUrl = jobUrl ?: "",
        notes = notes ?: "",
        idempotencyKey = idempotencyKey ?: "",
        syncStatus = SyncStatus.SYNCED,
        version = version,
        serverCompany = null,
        serverPositionTitle = null,
        serverStatus = null,
        serverAppliedDate = null,
        serverLocation = null,
        serverJobUrl = null,
        serverNotes = null,
        serverIdempotencyKey = null,
        serverVersion = null
    )
}

fun CreateJobApplicationMutation.CreateJobApplication.toEntity(): JobApplicationEntity {
    return JobApplicationEntity(
        id = id,
        companyName = companyName,
        positionTitle = positionTitle,
        status = status,
        appliedDate = appliedDate,
        location = location ?: "",
        jobUrl = jobUrl ?: "",
        notes = notes ?: "",
        idempotencyKey = idempotencyKey ?: "",
        syncStatus = SyncStatus.SYNCED,
        version = version,
        serverCompany = null,
        serverPositionTitle = null,
        serverStatus = null,
        serverAppliedDate = null,
        serverLocation = null,
        serverJobUrl = null,
        serverNotes = null,
        serverIdempotencyKey = null,
        serverVersion = null
    )
}

fun JobApplicationEntity.toDomain(): JobApplication {
    return JobApplication(
        id = id,
        companyName = companyName,
        positionTitle = positionTitle,
        status = ApplicationStatus.fromString(status),
        appliedDate = appliedDate,
        location = location,
        jobUrl = jobUrl,
        notes = notes,
        idempotencyKey = idempotencyKey,
        syncStatus = syncStatus,
        version = version,
        serverCompany = serverCompany,
        serverPositionTitle = serverPositionTitle,
        serverStatus = serverStatus?.let { ApplicationStatus.fromString(it) },
        serverAppliedDate = serverAppliedDate,
        serverLocation = serverLocation,
        serverJobUrl = serverJobUrl,
        serverNotes = serverNotes,
        serverIdempotencyKey = serverIdempotencyKey,
        serverVersion = serverVersion
    )
}

fun JobApplication.toEntity(): JobApplicationEntity {
    return JobApplicationEntity(
        id = id,
        companyName = companyName,
        positionTitle = positionTitle,
        status = status.name,
        appliedDate = appliedDate,
        location = location,
        jobUrl = jobUrl,
        notes = notes,
        idempotencyKey = idempotencyKey,
        syncStatus = syncStatus,
        version = version,
        serverCompany = serverCompany,
        serverPositionTitle = serverPositionTitle,
        serverStatus = serverStatus?.name,
        serverAppliedDate = serverAppliedDate,
        serverLocation = serverLocation,
        serverJobUrl = serverJobUrl,
        serverNotes = serverNotes,
        serverIdempotencyKey = serverIdempotencyKey,
        serverVersion = serverVersion
    )
}
