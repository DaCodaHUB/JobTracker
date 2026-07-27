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
import com.dangle.jobtracker.di.IoDispatcher
import com.dangle.jobtracker.domain.model.ApplicationStatus
import com.dangle.jobtracker.domain.model.JobApplication
import com.dangle.jobtracker.domain.model.SyncStatus
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject

/**
 * Implementation of [JobApplicationRepository] following the Single Source of Truth (SSOT) pattern.
 *
 * This repository manages job application data by prioritizing a local Room database as the primary data source.
 * It handles offline-first capabilities by:
 * 1. Performing immediate local updates with a [SyncStatus] flag.
 * 2. Scheduling background synchronization using [WorkManager].
 * 3. Reconciling local data with remote server state during refresh operations.
 *
 * @property apolloClient GraphQL client for remote communication.
 * @property dao Local Data Access Object for Room database operations.
 * @property workManager Manager for scheduling background sync tasks.
 * @property ioDispatcher Dispatcher for executing blocking I/O operations.
 */
class JobApplicationRepositoryImpl @Inject constructor (
    private val apolloClient: ApolloClient,
    private val dao: JobApplicationDao,
    private val workManager: WorkManager,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : JobApplicationRepository {

    /**
     * Observes a stream of job applications from the local database.
     * The UI should observe this flow to stay in sync with the SSOT.
     */
    override fun getApplications(): Flow<List<JobApplication>> {
        return dao.getAllApplications().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    /**
     * Enqueues a [SyncJobApplicationsWorker] to push pending local changes to the server.
     * Uses [ExistingWorkPolicy.REPLACE] to ensure any previous pending sync is updated
     * with the latest local state.
     */
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

    /**
     * Fetches the latest data from the server and reconciles it with the local database.
     *
     * The reconciliation logic handles:
     * - **Inserts/Updates:** Adds new items or updates local items if the server version is higher.
     * - **Auto-resolution:** If a local item has a version mismatch but the business data (status)
     *   is identical, it updates the version and marks it as SYNCED.
     * - **Conflicts:** If a local item has pending changes and the server version is higher
     *   with different data, the item is marked as CONFLICT for user resolution.
     * - **Deletions:** Removes local items that are marked as SYNCED but are missing from the server.
     */
    override suspend fun refreshApplications(): Result<Unit> = withContext(ioDispatcher) {
        try {
            val response = apolloClient.query(GetJobApplicationsQuery()).execute()
            val serverItems = response.data?.jobApplications
            
            if (response.hasErrors() || serverItems == null) {
                val errorMessage = response.errors?.firstOrNull()?.message ?: "Fetch failed"
                Result.failure(Exception(errorMessage))
            } else {
                val localItems = dao.getAllApplicationsSync()
                val serverMap = serverItems.associateBy { it.id }
                
                // Process each server item and ensure no duplicates are created
                serverItems.forEach { upsertSafely(it.toEntity()) }
                
                // Identify items that were deleted on the server but are still present locally as SYNCED
                val toDelete = localItems.filter { localItem ->
                    val isMissingFromServer = !serverMap.containsKey(localItem.id)
                    isMissingFromServer && (localItem.syncStatus == SyncStatus.SYNCED || localItem.syncStatus == SyncStatus.PENDING_DELETE)
                }

                if (toDelete.isNotEmpty()) {
                    dao.deleteApplications(toDelete)
                }
                
                // Trigger a push of any local changes that might have been detected/created during refresh
                scheduleSync()

                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Atomically updates or inserts an application while preventing duplicates 
     * between local-only and server-synced items.
     */
    private suspend fun upsertSafely(serverEntity: JobApplicationEntity) {
        // 1. Check if we already have this ID
        val localById = dao.getApplicationById(serverEntity.id)
        if (localById != null) {
            processUpdate(localById, serverEntity)
            return
        }

        // 2. Check for Business Key match (Company + Position) among local-only items
        val match = dao.findAnyMatchByBusinessKey(serverEntity.companyName, serverEntity.positionTitle)
        if (match != null) {
            // We found a match with a different ID (likely a local UUID being replaced by a server ID)
            if (match.syncStatus == SyncStatus.PENDING_DELETE) {
                // If it's pending delete locally, don't re-insert it from server
                return
            }
            
            // Replace the local placeholder with the official server version
            dao.deleteApplication(match)
            dao.insertApplication(serverEntity)
            return
        }

        // 3. Brand new item from server
        dao.insertApplication(serverEntity)
    }

    /**
     * Handles updating an existing local entity with newer server data.
     */
    private suspend fun processUpdate(local: JobApplicationEntity, server: JobApplicationEntity) {
        if (local.syncStatus == SyncStatus.PENDING_DELETE) return
        
        // If local is synced, just overwrite if server is same or newer
        if (local.syncStatus == SyncStatus.SYNCED) {
            if (server.version >= local.version) {
                dao.insertApplication(server)
            }
            return
        }

        // If local has pending changes (UPDATE), check for conflict
        if (server.version > local.version) {
            // Check if the business data has diverged
            val hasSameData = local.status == server.status &&
                    local.companyName == server.companyName &&
                    local.positionTitle == server.positionTitle &&
                    local.location == server.location &&
                    local.jobUrl == server.jobUrl &&
                    local.notes == server.notes

            if (hasSameData) {
                // Same data, just update version and mark synced
                dao.insertApplication(server)
            } else {
                // Data differs: Mark as CONFLICT and store server snapshot for UI
                dao.updateApplication(local.copy(
                    syncStatus = SyncStatus.CONFLICT,
                    serverCompany = server.companyName,
                    serverPositionTitle = server.positionTitle,
                    serverStatus = server.status,
                    serverAppliedDate = server.appliedDate,
                    serverLocation = server.location,
                    serverJobUrl = server.jobUrl,
                    serverNotes = server.notes,
                    serverVersion = server.version
                ))
            }
        }
    }

    /**
     * Creates a new job application locally with [SyncStatus.PENDING_CREATE].
     * Generates a temporary local ID prefixed with "local_".
     */
    override suspend fun createApplication(
        companyName: String,
        positionTitle: String,
        status: ApplicationStatus,
        appliedDate: String,
        location: String,
        jobUrl: String,
        notes: String
    ): Result<JobApplication> = withContext(ioDispatcher) {
        val initialEntity = JobApplicationEntity(
            id = "local_${UUID.randomUUID()}",
            companyName = companyName, positionTitle = positionTitle,
            status = status.name, appliedDate = appliedDate,
            location = location, jobUrl = jobUrl, notes = notes,
            syncStatus = SyncStatus.PENDING_CREATE
        )
        dao.insertApplication(initialEntity)

        scheduleSync()

        Result.success(initialEntity.toDomain())
    }

    /**
     * Updates the status of an existing application.
     * If the item was already [SyncStatus.SYNCED], it marks it as [SyncStatus.PENDING_UPDATE].
     */
    override suspend fun updateStatus(id: String, newStatus: ApplicationStatus): Result<Unit> = withContext(ioDispatcher) {
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

    /**
     * Deletes an application.
     * - If the item is PENDING_CREATE (local only), it is hard-deleted immediately.
     * - Otherwise, it is marked as PENDING_DELETE to be synced with the server later.
     */
    override suspend fun deleteApplication(id: String): Result<Unit> = withContext(ioDispatcher) {
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

    /**
     * Resolves a conflict by overwriting server data with local pending changes.
     */
    override suspend fun resolveKeepMine(id: String): Result<Unit> = withContext(ioDispatcher) {
        try {
            dao.resolveKeepMine(id)
            scheduleSync()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Resolves a conflict by overwriting local changes with server data.
     */
    override suspend fun resolveKeepServer(id: String): Result<Unit> = withContext(ioDispatcher) {
        try {
            dao.resolveKeepServer(id)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
