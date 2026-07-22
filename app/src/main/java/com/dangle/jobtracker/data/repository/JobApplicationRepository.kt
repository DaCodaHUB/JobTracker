package com.dangle.jobtracker.data.repository

import com.dangle.jobtracker.domain.model.ApplicationStatus
import com.dangle.jobtracker.domain.model.JobApplication
import kotlinx.coroutines.flow.Flow

interface JobApplicationRepository {
    // No function bodies here, just the signatures
    suspend fun getApplications(): Result<List<JobApplication>>
    
    fun observeApplications(): Flow<List<JobApplication>>

    suspend fun createApplication(
        companyName: String,
        positionTitle: String,
        status: ApplicationStatus,
        appliedDate: String
    ): Result<JobApplication>

    suspend fun updateStatus(id: String, newStatus: ApplicationStatus): Result<Unit>
}