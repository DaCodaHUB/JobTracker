package com.dangle.jobtracker.data.repository

import com.dangle.jobtracker.CreateJobApplicationMutation
import com.dangle.jobtracker.GetJobApplicationsQuery
import com.dangle.jobtracker.data.local.entity.JobApplicationEntity
import com.dangle.jobtracker.domain.model.ApplicationStatus
import com.dangle.jobtracker.domain.model.JobApplication

fun GetJobApplicationsQuery.JobApplication.toEntity(): JobApplicationEntity {
    return JobApplicationEntity(
        id = id,
        companyName = companyName,
        positionTitle = positionTitle,
        status = status,
        appliedDate = appliedDate,
        isPendingSync = false
    )
}

fun CreateJobApplicationMutation.CreateJobApplication.toEntity(): JobApplicationEntity {
    return JobApplicationEntity(
        id = id,
        companyName = companyName,
        positionTitle = positionTitle,
        status = status,
        appliedDate = appliedDate,
        isPendingSync = false
    )
}

fun JobApplicationEntity.toDomain(): JobApplication {
    return JobApplication(
        id = id,
        companyName = companyName,
        positionTitle = positionTitle,
        status = ApplicationStatus.fromString(status),
        appliedDate = appliedDate,
        isPendingSync = isPendingSync
    )
}
