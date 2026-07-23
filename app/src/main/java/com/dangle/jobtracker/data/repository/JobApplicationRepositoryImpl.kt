package com.dangle.jobtracker.data.repository

import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.apollographql.apollo.ApolloClient
import com.dangle.jobtracker.GetJobApplicationsQuery
import com.dangle.jobtracker.data.local.dao.JobApplicationDao
import com.dangle.jobtracker.data.local.entity.JobApplicationEntity
import com.dangle.jobtracker.data.worker.SyncJobApplicationsWorker
import com.dangle.jobtracker.domain.model.ApplicationStatus
import com.dangle.jobtracker.domain.model.JobApplication
import com.dangle.jobtracker.domain.model.SyncStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject

class JobApplicationRepositoryImpl @Inject constructor (
    private val apolloClient: ApolloClient,
    private val dao: JobApplicationDao,
    private val workManager: WorkManager
) : JobApplicationRepository {

    override fun getApplications(): Flow<List<JobApplication>> {
        return dao.getAllApplications().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun scheduleSync() {
        val workRequest = OneTimeWorkRequestBuilder<SyncJobApplicationsWorker>()
            .build()
        workManager.enqueueUniqueWork(
            "SyncJobApplicationsWork",
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
    }

    override suspend fun refreshApplications(): Result<Unit> = withContext(Dispatchers.IO) {
        scheduleSync()
        try {
            val response = apolloClient.query(GetJobApplicationsQuery()).execute()
            val serverItems = response.data?.jobApplications
            
            if (response.hasErrors() || serverItems == null) {
                val errorMessage = response.errors?.firstOrNull()?.message ?: "Fetch failed"
                Result.failure(Exception(errorMessage))
            } else {
                dao.insertApplications(serverItems.map { it.toEntity() })
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun createApplication(
        companyName: String,
        positionTitle: String,
        status: ApplicationStatus,
        appliedDate: String
    ): Result<JobApplication> = withContext(Dispatchers.IO) {
        val initialEntity = JobApplicationEntity(
            id = "local_${UUID.randomUUID()}",
            companyName = companyName, positionTitle = positionTitle,
            status = status.name, appliedDate = appliedDate,
            syncStatus = SyncStatus.PENDING_CREATE
        )
        dao.insertApplication(initialEntity)

        scheduleSync()

        Result.success(initialEntity.toDomain())
    }

    override suspend fun updateStatus(id: String, newStatus: ApplicationStatus): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val entity = dao.getApplicationById(id)
            if (entity != null) {
                val updatedEntity = entity.copy(
                    status = newStatus.name,
                    syncStatus = if (entity.syncStatus == SyncStatus.SYNCED) SyncStatus.PENDING_UPDATE else entity.syncStatus
                )
                dao.updateApplication(updatedEntity)
                scheduleSync()
                Result.success(Unit)
            } else {
                Result.failure(Exception("Application not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
