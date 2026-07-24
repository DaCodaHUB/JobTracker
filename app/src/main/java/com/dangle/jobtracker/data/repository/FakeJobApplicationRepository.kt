package com.dangle.jobtracker.data.repository

import com.dangle.jobtracker.domain.model.JobApplication
import com.dangle.jobtracker.domain.model.ApplicationStatus
import com.dangle.jobtracker.domain.model.SyncStatus
import com.dangle.jobtracker.data.repository.JobApplicationRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map

class FakeJobApplicationRepository : JobApplicationRepository {
    
    companion object {
        // In-memory list shared across all instances
        private val applications = mutableListOf<JobApplication>()
        private val _applicationsFlow = MutableStateFlow<List<JobApplication>>(emptyList())
    }

    override fun getApplications(): Flow<List<JobApplication>> = _applicationsFlow.asStateFlow()

    override fun scheduleSync() {
        // No-op in fake
    }

    override suspend fun refreshApplications(): Result<Unit> {
        delay(500)
        return Result.success(Unit)
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
            appliedDate = appliedDate,
            syncStatus = SyncStatus.SYNCED,
            version = 1
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

    override suspend fun deleteApplication(id: String): Result<Unit> {
        delay(300)
        val index = applications.indexOfFirst { it.id == id }
        if (index != -1) {
            applications.removeAt(index)
            _applicationsFlow.value = applications.toList()
            return Result.success(Unit)
        }
        return Result.failure(Exception("Application not found"))
    }

    override suspend fun resolveKeepMine(id: String): Result<Unit> {
        delay(300)
        val index = applications.indexOfFirst { it.id == id }
        if (index != -1) {
            val app = applications[index]
            applications[index] = app.copy(
                version = app.serverVersion ?: app.version,
                syncStatus = SyncStatus.PENDING_UPDATE,
                serverCompany = null,
                serverStatus = null,
                serverVersion = null
            )
            _applicationsFlow.value = applications.toList()
            return Result.success(Unit)
        }
        return Result.failure(Exception("Application not found"))
    }

    override suspend fun resolveKeepServer(id: String): Result<Unit> {
        delay(300)
        val index = applications.indexOfFirst { it.id == id }
        if (index != -1) {
            val app = applications[index]
            applications[index] = app.copy(
                companyName = app.serverCompany ?: app.companyName,
                status = app.serverStatus ?: app.status,
                version = app.serverVersion ?: app.version,
                syncStatus = SyncStatus.SYNCED,
                serverCompany = null,
                serverStatus = null,
                serverVersion = null
            )
            _applicationsFlow.value = applications.toList()
            return Result.success(Unit)
        }
        return Result.failure(Exception("Application not found"))
    }
}
