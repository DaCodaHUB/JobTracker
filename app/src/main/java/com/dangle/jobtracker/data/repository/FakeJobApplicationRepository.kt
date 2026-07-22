package com.dangle.jobtracker.data.repository

import com.dangle.jobtracker.domain.model.JobApplication
import com.dangle.jobtracker.domain.model.ApplicationStatus
import com.dangle.jobtracker.data.repository.JobApplicationRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeJobApplicationRepository : JobApplicationRepository {
    
    companion object {
        // In-memory list shared across all instances
        private val applications = mutableListOf<JobApplication>()
        private val _applicationsFlow = MutableStateFlow<List<JobApplication>>(emptyList())
    }

    override fun observeApplications(): Flow<List<JobApplication>> = _applicationsFlow.asStateFlow()

    override suspend fun getApplications(): Result<List<JobApplication>> {
        // Optional: Add a delay to test your UI's loading spinner
        delay(800)

        // Return a copy of the list
        return Result.success(applications.toList())
    }

    override suspend fun createApplication(
        companyName: String,
        positionTitle: String,
        status: ApplicationStatus,
        appliedDate: String
    ): Result<JobApplication> {
        delay(500)

        val newApp = JobApplication(
            id = (applications.size + 1).toString(),
            companyName = companyName,
            positionTitle = positionTitle,
            status = status,
            appliedDate = appliedDate
        )

        applications.add(newApp)
        _applicationsFlow.value = applications.toList()
        return Result.success(newApp)
    }

    override suspend fun updateStatus(id: String, newStatus: ApplicationStatus): Result<Unit> {
        delay(300)
        val index = applications.indexOfFirst { it.id == id }
        if (index != -1) {
            applications[index] = applications[index].copy(status = newStatus)
            _applicationsFlow.value = applications.toList()
            return Result.success(Unit)
        }
        return Result.failure(Exception("Application not found"))
    }
}