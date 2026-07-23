package com.dangle.jobtracker.data.repository

import com.dangle.jobtracker.domain.model.ApplicationStatus
import com.dangle.jobtracker.domain.model.JobApplication
import kotlinx.coroutines.flow.Flow

interface JobApplicationRepository {
    
    /**
     * Observe the list of applications from the local database.
     */
    fun getApplications(): Flow<List<JobApplication>>

    /**
     * Fetch the latest applications from the network and sync to the local database.
     */
    suspend fun refreshApplications(): Result<Unit>

    /**
     * Sync any locally saved applications that haven't been pushed to the server yet.
     */
    suspend fun syncPendingApplications(): Result<Unit>

    suspend fun createApplication(
        companyName: String,
        positionTitle: String,
        status: ApplicationStatus,
        appliedDate: String
    ): Result<JobApplication>

    suspend fun updateStatus(id: String, newStatus: ApplicationStatus): Result<Unit>
}
