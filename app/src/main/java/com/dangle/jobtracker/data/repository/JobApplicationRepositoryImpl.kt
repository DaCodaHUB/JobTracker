package com.dangle.jobtracker.data.repository

import android.util.Log
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.exception.ApolloException
import com.dangle.jobtracker.CreateJobApplicationMutation
import com.dangle.jobtracker.DeleteJobApplicationMutation
import com.dangle.jobtracker.GetJobApplicationQuery
import com.dangle.jobtracker.GetJobApplicationsQuery
import com.dangle.jobtracker.OnJobApplicationUpdatedSubscription
import com.dangle.jobtracker.UpdateJobApplicationStatusMutation
import com.dangle.jobtracker.data.local.dao.JobApplicationDao
import com.dangle.jobtracker.data.local.entity.JobApplicationEntity
import com.dangle.jobtracker.data.worker.SyncWorker
import com.dangle.jobtracker.domain.model.ApplicationStatus
import com.dangle.jobtracker.domain.model.JobApplication
import com.dangle.jobtracker.domain.model.SyncStatus
import com.dangle.jobtracker.type.CreateJobApplicationInput
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.UUID
import javax.inject.Inject

class JobApplicationRepositoryImpl @Inject constructor (
    private val apolloClient: ApolloClient,
    private val dao: JobApplicationDao,
    private val workManager: WorkManager
) : JobApplicationRepository {

    private val syncMutex = Mutex()

    companion object {
        private const val TAG = "JobRepository"
        private const val SYNC_WORK_NAME = "JobTrackerSyncWork"
    }

    override fun getApplications(): Flow<List<JobApplication>> {
        return dao.getAllApplications().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun observeRealtimeUpdates(): Flow<Unit> {
        return apolloClient.subscription(OnJobApplicationUpdatedSubscription()).toFlow()
            .onEach { response ->
                val updatedApp = response.data?.jobApplicationUpdated
                if (updatedApp != null) {
                    syncMutex.withLock {
                        Log.d(TAG, "Subscription update for ${updatedApp.companyName}")
                        upsertSafely(updatedApp.toEntity())
                    }
                }
            }
            .map { }
            .catch { e -> 
                Log.e(TAG, "Subscription error", e)
            }
    }

    override suspend fun pushPendingMutations(): Result<Unit> = withContext(Dispatchers.IO) {
        syncMutex.withLock {
            try {
                val pending = dao.getPendingApplications()
                Log.d(TAG, "Pushing ${pending.size} pending changes")
                
                for (entity in pending) {
                    val current = dao.getApplicationById(entity.id)
                    if (current == null || current.syncStatus == SyncStatus.SYNCED) continue

                    try {
                        when (current.syncStatus) {
                            SyncStatus.PENDING_CREATE -> handleCreate(current)
                            SyncStatus.PENDING_UPDATE -> handleUpdate(current)
                            SyncStatus.PENDING_DELETE -> handleDelete(current)
                            else -> continue
                        }
                    } catch (e: ConflictException) {
                        handleConflict(current)
                    }
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Log.e(TAG, "Push failed", e)
                Result.failure(e)
            }
        }
    }

    private suspend fun handleCreate(entity: JobApplicationEntity) {
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

        if (response.hasErrors()) {
            if (response.errors?.any { it.extensions?.get("code") == "CONFLICT" } == true) {
                throw ConflictException()
            }
            throw Exception("Create failed")
        }

        val data = response.data?.createJobApplication
        if (data != null) {
            dao.replaceLocalWithServer(entity, data.toEntity())
        }
    }

    private suspend fun handleUpdate(entity: JobApplicationEntity) {
        val response = apolloClient.mutation(
            UpdateJobApplicationStatusMutation(
                id = entity.id,
                status = entity.status,
                version = entity.version
            )
        ).execute()

        if (response.hasErrors()) {
            if (response.errors?.any { it.extensions?.get("code") == "CONFLICT" } == true) {
                throw ConflictException()
            }
            throw Exception("Update failed")
        }

        val updated = response.data?.updateJobApplicationStatus
        if (updated != null) {
            val local = dao.getApplicationById(updated.id)
            if (local != null) {
                dao.updateApplication(local.copy(
                    status = updated.status,
                    version = updated.version,
                    syncStatus = SyncStatus.SYNCED
                ))
            }
        }
    }

    private suspend fun handleDelete(entity: JobApplicationEntity) {
        val response = apolloClient.mutation(
            DeleteJobApplicationMutation(id = entity.id, version = entity.version)
        ).execute()

        if (!response.hasErrors()) {
            dao.deleteApplication(entity)
        } else if (response.errors?.any { it.extensions?.get("code") == "CONFLICT" } == true) {
            throw ConflictException()
        }
    }

    private suspend fun handleConflict(entity: JobApplicationEntity) {
        val response = apolloClient.query(GetJobApplicationQuery(id = entity.id)).execute()
        val serverApp = response.data?.jobApplication
        if (serverApp != null) {
            if (entity.status == serverApp.status) {
                dao.replaceLocalWithServer(entity, serverApp.toEntity())
            } else {
                dao.updateApplication(entity.copy(
                    syncStatus = SyncStatus.CONFLICT,
                    serverCompany = serverApp.companyName,
                    serverPositionTitle = serverApp.positionTitle,
                    serverStatus = serverApp.status,
                    serverAppliedDate = serverApp.appliedDate,
                    serverVersion = serverApp.version
                ))
            }
        } else {
            dao.deleteApplication(entity)
        }
    }

    override suspend fun pullRemoteUpdates(): Result<Unit> = refreshApplications()

    override fun scheduleSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val workRequest = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(constraints)
            .build()
        workManager.enqueueUniqueWork(SYNC_WORK_NAME, ExistingWorkPolicy.KEEP, workRequest)
    }

    override suspend fun refreshApplications(): Result<Unit> = withContext(Dispatchers.IO) {
        syncMutex.withLock {
            try {
                val response = apolloClient.query(GetJobApplicationsQuery()).execute()
                val serverItems = response.data?.jobApplications
                if (response.hasErrors() || serverItems == null) {
                    Result.failure(Exception("Refresh failed"))
                } else {
                    val localItems = dao.getAllApplicationsSync()
                    val serverMap = serverItems.associateBy { it.id }

                    serverItems.forEach { upsertSafely(it.toEntity()) }

                    val toDelete = localItems.filter { 
                        it.syncStatus == SyncStatus.SYNCED && !serverMap.containsKey(it.id) 
                    }
                    if (toDelete.isNotEmpty()) dao.deleteApplications(toDelete)
                    
                    scheduleSync()
                    Result.success(Unit)
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    private suspend fun upsertSafely(serverEntity: JobApplicationEntity) {
        val localById = dao.getApplicationById(serverEntity.id)
        if (localById != null) {
            processUpdate(localById, serverEntity)
            return
        }

        val match = dao.findAnyMatchByBusinessKey(serverEntity.companyName, serverEntity.positionTitle)
        if (match != null) {
            if (match.syncStatus == SyncStatus.PENDING_DELETE) return
            if (match.id != serverEntity.id) {
                dao.replaceLocalWithServer(match, serverEntity)
            } else {
                dao.insertApplication(serverEntity)
            }
            return
        }

        dao.insertApplication(serverEntity)
    }

    private suspend fun processUpdate(local: JobApplicationEntity, server: JobApplicationEntity) {
        if (local.syncStatus == SyncStatus.PENDING_DELETE) return
        if (local.syncStatus == SyncStatus.SYNCED) {
            if (server.version >= local.version) dao.insertApplication(server)
            return
        }

        if (server.version > local.version) {
            if (local.status != server.status) {
                dao.updateApplication(local.copy(
                    serverCompany = server.companyName,
                    serverPositionTitle = server.positionTitle,
                    serverStatus = server.status,
                    serverAppliedDate = server.appliedDate,
                    serverVersion = server.version,
                    syncStatus = SyncStatus.CONFLICT
                ))
            } else {
                dao.insertApplication(server)
            }
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
                Result.failure(Exception("Not found"))
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
                    dao.deleteById(id)
                } else {
                    val updatedEntity = entity.copy(syncStatus = SyncStatus.PENDING_DELETE)
                    dao.updateApplication(updatedEntity)
                    scheduleSync()
                }
                Result.success(Unit)
            } else {
                Result.failure(Exception("Not found"))
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

    private class ConflictException : Exception()
}
