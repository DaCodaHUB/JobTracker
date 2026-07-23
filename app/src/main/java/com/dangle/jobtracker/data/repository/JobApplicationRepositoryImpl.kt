package com.dangle.jobtracker.data.repository

import com.apollographql.apollo.ApolloClient
import com.dangle.jobtracker.CreateJobApplicationMutation
import com.dangle.jobtracker.GetJobApplicationsQuery
import com.dangle.jobtracker.UpdateJobApplicationStatusMutation
import com.dangle.jobtracker.data.local.dao.JobApplicationDao
import com.dangle.jobtracker.data.local.entity.JobApplicationEntity
import com.dangle.jobtracker.data.network.ApolloClientProvider
import com.dangle.jobtracker.domain.model.ApplicationStatus
import com.dangle.jobtracker.domain.model.JobApplication
import com.dangle.jobtracker.type.CreateJobApplicationInput
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.UUID

class JobApplicationRepositoryImpl(
    private val apolloClient: ApolloClient = ApolloClientProvider.client,
    private val dao: JobApplicationDao
) : JobApplicationRepository {

    override fun getApplications(): Flow<List<JobApplication>> {
        return dao.getAllApplications().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun syncPendingApplications(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val pending = dao.getPendingApplications()
            pending.forEach { syncSingleApplication(it) }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun refreshApplications(): Result<Unit> = withContext(Dispatchers.IO) {
        syncPendingApplications()
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
            isPendingSync = true
        )
        dao.insertApplication(initialEntity)

        // Attempt background sync
        syncSingleApplication(initialEntity)

        Result.success(initialEntity.toDomain())
    }

    private suspend fun syncSingleApplication(entity: JobApplicationEntity) {
        try {
            val response = apolloClient.mutation(
                CreateJobApplicationMutation(
                    input = CreateJobApplicationInput(
                        companyName = entity.companyName,
                        positionTitle = entity.positionTitle,
                        status = entity.status,
                        appliedDate = entity.appliedDate
                    )
                )
            ).execute()

            val data = response.data?.createJobApplication
            if (data != null && !response.hasErrors()) {
                dao.deleteApplication(entity)
                dao.insertApplication(data.toEntity())
            }
        } catch (e: Exception) {
            // Silently fail, item remains as pending
        }
    }

    override suspend fun updateStatus(id: String, newStatus: ApplicationStatus): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = apolloClient.mutation(
                UpdateJobApplicationStatusMutation(id = id, status = newStatus.name)
            ).execute()

            if (response.hasErrors()) {
                Result.failure(Exception(response.errors?.firstOrNull()?.message ?: "Update failed"))
            } else {
                refreshApplications()
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
