package com.dangle.jobtracker.data.repository

import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
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
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val workRequest = OneTimeWorkRequestBuilder<SyncJobApplicationsWorker>()
            .setConstraints(constraints)
            .build()
        workManager.enqueueUniqueWork(
            "SyncJobApplicationsWork",
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
    }

    override suspend fun refreshApplications(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = apolloClient.query(GetJobApplicationsQuery()).execute()
            val serverItems = response.data?.jobApplications
            
            if (response.hasErrors() || serverItems == null) {
                val errorMessage = response.errors?.firstOrNull()?.message ?: "Fetch failed"
                Result.failure(Exception(errorMessage))
            } else {
                val localItems = dao.getAllApplicationsSync()
                val localMap = localItems.associateBy { it.id }
                val serverMap = serverItems.associateBy { it.id }

                // 1. Identify items to update, insert, or mark as conflicted
                val toUpdateOrInsert = mutableListOf<JobApplicationEntity>()
                val toMarkConflicted = mutableListOf<JobApplicationEntity>()

                serverItems.forEach { serverItem ->
                    val localItem = localMap[serverItem.id]
                    if (localItem == null || localItem.version < serverItem.version) {
                        val shouldAutoResolve = localItem != null && 
                                localItem.syncStatus != SyncStatus.SYNCED && 
                                localItem.status == serverItem.status

                        if (localItem == null || localItem.syncStatus == SyncStatus.SYNCED || shouldAutoResolve) {
                            // New item, synced item update, or auto-resolved conflict
                            toUpdateOrInsert.add(serverItem.toEntity())
                        } else {
                            // Actual conflict
                            toMarkConflicted.add(
                                localItem.copy(
                                    syncStatus = SyncStatus.CONFLICT,
                                    serverCompany = serverItem.companyName,
                                    serverPositionTitle = serverItem.positionTitle,
                                    serverStatus = serverItem.status,
                                    serverAppliedDate = serverItem.appliedDate,
                                    serverVersion = serverItem.version
                                )
                            )
                        }
                    }
                }

                // 2. Identify items to delete:
                // - SYNCED locally but missing from server (deleted elsewhere)
                // - PENDING_DELETE locally but missing from server (sync success but local cleanup missed)
                val toDelete = localItems.filter { localItem ->
                    val isMissingFromServer = !serverMap.containsKey(localItem.id)
                    isMissingFromServer && (localItem.syncStatus == SyncStatus.SYNCED || localItem.syncStatus == SyncStatus.PENDING_DELETE)
                }

                if (toUpdateOrInsert.isNotEmpty()) {
                    dao.insertApplications(toUpdateOrInsert)
                }
                if (toMarkConflicted.isNotEmpty()) {
                    dao.insertApplications(toMarkConflicted)
                }
                if (toDelete.isNotEmpty()) {
                    dao.deleteApplications(toDelete)
                }

                // Trigger sync of local changes whenever we refresh
                scheduleSync()

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

    override suspend fun deleteApplication(id: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val entity = dao.getApplicationById(id)
            if (entity != null) {
                if (entity.syncStatus == SyncStatus.PENDING_CREATE) {
                    // Item never reached the server, just delete locally
                    dao.deleteById(id)
                } else {
                    // Mark for deletion on server
                    val updatedEntity = entity.copy(syncStatus = SyncStatus.PENDING_DELETE)
                    dao.updateApplication(updatedEntity)
                    scheduleSync()
                }
                Result.success(Unit)
            } else {
                Result.failure(Exception("Application not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun resolveKeepMine(id: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            dao.resolveKeepMine(id)
            scheduleSync()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun resolveKeepServer(id: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            dao.resolveKeepServer(id)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
