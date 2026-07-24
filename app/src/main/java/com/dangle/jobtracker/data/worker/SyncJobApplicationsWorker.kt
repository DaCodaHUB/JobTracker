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
import com.dangle.jobtracker.UpdateJobApplicationStatusMutation
import com.dangle.jobtracker.data.local.dao.JobApplicationDao
import com.dangle.jobtracker.data.local.entity.JobApplicationEntity
import com.dangle.jobtracker.data.repository.toEntity
import com.dangle.jobtracker.domain.model.SyncStatus
import com.dangle.jobtracker.type.CreateJobApplicationInput
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.io.IOException

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
        val pendingApplications = dao.getPendingApplications()
        Log.d(TAG, "Found ${pendingApplications.size} pending applications")
        var hasNetworkError = false
        var hasFatalError = false

        for (entity in pendingApplications) {
            try {
                Log.d(TAG, "Processing entity ${entity.id} with status ${entity.syncStatus}")
                when (entity.syncStatus) {
                    SyncStatus.PENDING_CREATE -> handleCreate(entity)
                    SyncStatus.PENDING_UPDATE -> handleUpdate(entity)
                    SyncStatus.PENDING_DELETE -> handleDelete(entity)
                    else -> continue
                }
            } catch (e: ConflictException) {
                Log.w(TAG, "Conflict detected for entity ${entity.id}, fetching server state")
                try {
                    val response = apolloClient.query(GetJobApplicationQuery(id = entity.id)).execute()
                    val serverApp = response.data?.jobApplication
                    if (serverApp != null) {
                        if (entity.status == serverApp.status) {
                            Log.d(TAG, "Status matches server for ${entity.id}, auto-resolving conflict")
                            dao.updateApplication(entity.copy(
                                syncStatus = SyncStatus.SYNCED,
                                companyName = serverApp.companyName,
                                positionTitle = serverApp.positionTitle,
                                status = serverApp.status,
                                appliedDate = serverApp.appliedDate,
                                version = serverApp.version,
                                serverCompany = null,
                                serverPositionTitle = null,
                                serverStatus = null,
                                serverAppliedDate = null,
                                serverVersion = null
                            ))
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
                        // If it's gone from server, maybe we should just delete it or mark as conflict with null server data
                        dao.updateApplication(entity.copy(syncStatus = SyncStatus.CONFLICT))
                    }
                } catch (fetchError: Exception) {
                    Log.e(TAG, "Failed to fetch server state for conflict on ${entity.id}", fetchError)
                    dao.updateApplication(entity.copy(syncStatus = SyncStatus.CONFLICT))
                }
            } catch (e: Exception) {
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
                Log.d(TAG, "Sync finished with network error, retrying")
                Result.retry()
            }
            hasFatalError -> {
                Log.d(TAG, "Sync finished with fatal error")
                Result.failure()
            }
            else -> {
                Log.d(TAG, "Sync finished successfully")
                Result.success()
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
            throw Exception("Failed to create application: ${response.errors?.firstOrNull()?.message}")
        }

        val data = response.data?.createJobApplication
        Log.d(TAG, "Create response data: $data")
        if (data != null) {
            dao.deleteApplication(entity)
            dao.insertApplication(data.toEntity())
        } else {
            throw Exception("No data returned from server for create")
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
            throw Exception("Failed to update application: ${response.errors?.firstOrNull()?.message}")
        }

        val updatedData = response.data?.updateJobApplicationStatus
        Log.d(TAG, "Update response data: $updatedData")
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
        
        // Either way, if there are no errors, we should remove it locally.
        dao.deleteApplication(entity)
    }

    private class ConflictException : Exception()
}
