package com.dangle.jobtracker.data.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.exception.ApolloException
import com.dangle.jobtracker.CreateJobApplicationMutation
import com.dangle.jobtracker.DeleteJobApplicationMutation
import com.dangle.jobtracker.GetJobApplicationQuery
import com.dangle.jobtracker.UpdateJobApplicationMutation
import com.dangle.jobtracker.data.local.dao.JobApplicationDao
import com.dangle.jobtracker.data.local.entity.JobApplicationEntity
import com.dangle.jobtracker.data.repository.toEntity
import com.dangle.jobtracker.domain.model.SyncStatus
import com.dangle.jobtracker.type.CreateJobApplicationInput
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.io.IOException

/**
 * A [CoroutineWorker] responsible for synchronizing pending local changes with the remote server.
 * 
 * This worker:
 * 1. Fetches all [JobApplicationEntity] with a non-SYNCED [SyncStatus].
 * 2. Iterates through them and executes the corresponding GraphQL mutation.
 * 3. Handles conflicts by fetching the latest server state and updating the local entity.
 * 4. Categorizes errors into retriable (network) and fatal (logic/server) errors.
 */
@HiltWorker
class SyncJobApplicationsWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val dao: JobApplicationDao,
    private val apolloClient: ApolloClient
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val TAG = "SyncWorker"
    }

    override suspend fun doWork(): Result {
        Log.d(TAG, "Starting sync work")
        
        // Fetch only items that need to be pushed to the server
        val pendingApplications = dao.getPendingApplications()
        Log.d(TAG, "Found ${pendingApplications.size} pending applications")
        
        var hasNetworkError = false
        var hasFatalError = false

        for (entity in pendingApplications) {
            try {
                Log.d(TAG, "Processing entity ${entity.id} with status ${entity.syncStatus}")
                
                // Route to the specific sync handler based on the pending action
                when (entity.syncStatus) {
                    SyncStatus.PENDING_CREATE -> handleCreate(entity)
                    SyncStatus.PENDING_UPDATE -> handleUpdate(entity)
                    SyncStatus.PENDING_DELETE -> handleDelete(entity)
                    else -> continue
                }
            } catch (e: ConflictException) {
                // Server rejected the update because the version is stale.
                // We fetch the latest server state to allow the user (or auto-logic) to resolve.
                Log.w(TAG, "Conflict detected for entity ${entity.id}, fetching server state")
                try {
                    val response = apolloClient.query(GetJobApplicationQuery(id = entity.id)).execute()
                    val serverApp = response.data?.jobApplication
                    if (serverApp != null) {
                        // If the server data matches local intent, we auto-resolve to avoid bugging the user
                        val hasSameData = entity.status == serverApp.status &&
                                entity.companyName == serverApp.companyName &&
                                entity.positionTitle == serverApp.positionTitle &&
                                entity.location == (serverApp.location ?: "") &&
                                entity.jobUrl == (serverApp.jobUrl ?: "") &&
                                entity.notes == (serverApp.notes ?: "")

                        if (hasSameData) {
                            Log.d(TAG, "Status matches server for ${entity.id}, auto-resolving conflict")
                            dao.updateApplication(entity.copy(
                                syncStatus = SyncStatus.SYNCED,
                                companyName = serverApp.companyName,
                                positionTitle = serverApp.positionTitle,
                                status = serverApp.status,
                                appliedDate = serverApp.appliedDate,
                                location = serverApp.location ?: "",
                                jobUrl = serverApp.jobUrl ?: "",
                                notes = serverApp.notes ?: "",
                                version = serverApp.version,
                                serverCompany = null,
                                serverPositionTitle = null,
                                serverStatus = null,
                                serverAppliedDate = null,
                                serverLocation = null,
                                serverJobUrl = null,
                                serverNotes = null,
                                serverVersion = null
                            ))
                        } else {
                            // Data differs significantly: Mark as CONFLICT and store server snapshot for UI comparison
                            dao.updateApplication(entity.copy(
                                syncStatus = SyncStatus.CONFLICT,
                                serverCompany = serverApp.companyName,
                                serverPositionTitle = serverApp.positionTitle,
                                serverStatus = serverApp.status,
                                serverAppliedDate = serverApp.appliedDate,
                                serverLocation = serverApp.location,
                                serverJobUrl = serverApp.jobUrl,
                                serverNotes = serverApp.notes,
                                serverVersion = serverApp.version
                            ))
                        }
                    } else {
                        // Entity no longer exists on the server
                        dao.updateApplication(entity.copy(syncStatus = SyncStatus.CONFLICT))
                    }
                } catch (fetchError: Exception) {
                    Log.e(TAG, "Failed to fetch server state for conflict on ${entity.id}", fetchError)
                    dao.updateApplication(entity.copy(syncStatus = SyncStatus.CONFLICT))
                }
            } catch (e: Exception) {
                // Categorize exceptions to decide whether to retry the worker later
                when (e) {
                    is ApolloException, is IOException -> {
                        Log.e(TAG, "Retriable network error syncing entity ${entity.id}: ${e.message}")
                        hasNetworkError = true
                    }
                    else -> {
                        Log.e(TAG, "Fatal error syncing entity ${entity.id}: ${e.message}", e)
                        hasFatalError = true
                    }
                }
            }
        }

        return when {
            hasNetworkError -> {
                // Transient error: WorkManager will retry based on backoff policy
                Log.d(TAG, "Sync finished with network error, retrying")
                Result.retry()
            }
            hasFatalError -> {
                // Unrecoverable error: stop trying for this work instance
                Log.d(TAG, "Sync finished with fatal error")
                Result.failure()
            }
            else -> {
                // All pending changes processed or marked appropriately
                Log.d(TAG, "Sync finished successfully")
                Result.success()
            }
        }
    }

    /**
     * Executes the create mutation. On success, replaces the temporary local entity 
     * (with "local_" ID) with the official server entity.
     */
    private suspend fun handleCreate(entity: JobApplicationEntity) {
        val response = apolloClient.mutation(
            CreateJobApplicationMutation(
                input = CreateJobApplicationInput(
                    companyName = entity.companyName,
                    positionTitle = entity.positionTitle,
                    status = entity.status,
                    appliedDate = entity.appliedDate,
                    location = com.apollographql.apollo.api.Optional.present(entity.location),
                    jobUrl = com.apollographql.apollo.api.Optional.present(entity.jobUrl),
                    notes = com.apollographql.apollo.api.Optional.present(entity.notes)
                )
            )
        ).execute()

        if (response.hasErrors()) {
            if (response.errors?.any { it.extensions?.get("code") == "CONFLICT" } == true) {
                throw ConflictException()
            }
            throw Exception("Failed to create application: ${response.errors?.firstOrNull()?.message}")
        }

        val data = response.data?.createJobApplication
        Log.d(TAG, "Create response data: $data")
        if (data != null) {
            // Delete local temp row and insert the official server-side row
            dao.deleteApplication(entity)
            dao.insertApplication(data.toEntity())
        } else {
            throw Exception("No data returned from server for create")
        }
    }

    /**
     * Executes the update mutation using optimistic locking (version check).
     */
    private suspend fun handleUpdate(entity: JobApplicationEntity) {
        val response = apolloClient.mutation(
            UpdateJobApplicationMutation(
                id = entity.id,
                status = com.apollographql.apollo.api.Optional.present(entity.status),
                notes = com.apollographql.apollo.api.Optional.present(entity.notes),
                version = entity.version
            )
        ).execute()

        if (response.hasErrors()) {
            if (response.errors?.any { it.extensions?.get("code") == "CONFLICT" } == true) {
                throw ConflictException()
            }
            throw Exception("Failed to update application: ${response.errors?.firstOrNull()?.message}")
        }

        val updatedData = response.data?.updateJobApplication
        if (updatedData != null) {
            dao.updateApplication(
                entity.copy(
                    syncStatus = SyncStatus.SYNCED,
                    version = updatedData.version
                )
            )
        } else {
            throw Exception("No data returned from server for update")
        }
    }

    /**
     * Executes the deletion mutation on the server and cleans up local database on success.
     */
    private suspend fun handleDelete(entity: JobApplicationEntity) {
        Log.d(TAG, "Syncing deletion for entity: ${entity.id}")
        val response = apolloClient.mutation(
            DeleteJobApplicationMutation(id = entity.id, version = entity.version)
        ).execute()

        if (response.hasErrors()) {
            val errors = response.errors?.joinToString { it.message }
            Log.e(TAG, "Server returned errors during deletion for ${entity.id}: $errors")
            if (response.errors?.any { it.extensions?.get("code") == "CONFLICT" } == true) {
                throw ConflictException()
            }
            throw Exception("Failed to delete application: $errors")
        }

        Log.d(TAG, "Delete response data: ${response.data}")
        val success = response.data?.deleteJobApplication ?: false
        if (success) {
            Log.d(TAG, "Successfully deleted entity ${entity.id} on server")
        } else {
            Log.w(TAG, "Server returned false for deletion of ${entity.id}. It might have been already deleted.")
        }
        
        // Cleanup local storage now that the server is aware
        dao.deleteApplication(entity)
    }

    class ConflictException : Exception()
}
