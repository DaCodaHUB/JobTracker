package com.dangle.jobtracker.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.exception.ApolloException
import com.dangle.jobtracker.CreateJobApplicationMutation
import com.dangle.jobtracker.DeleteJobApplicationMutation
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

    override suspend fun doWork(): Result {
        val pendingApplications = dao.getPendingApplications()

        if (pendingApplications.isEmpty()) {
            return Result.success()
        }

        var hasError = false

        for (entity in pendingApplications) {
            try {
                when (entity.syncStatus) {
                    SyncStatus.PENDING_CREATE -> handleCreate(entity)
                    SyncStatus.PENDING_UPDATE -> handleUpdate(entity)
                    SyncStatus.PENDING_DELETE -> handleDelete(entity)
                    SyncStatus.SYNCED -> continue
                }
            } catch (e: IOException) {
                return Result.retry()
            } catch (e: ApolloException) {
                return Result.retry()
            } catch (e: Exception) {
                hasError = true
            }
        }

        return if (hasError) Result.failure() else Result.success()
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

        val data = response.data?.createJobApplication
        if (data != null && !response.hasErrors()) {
            dao.deleteApplication(entity)
            dao.insertApplication(data.toEntity())
        } else {
            throw Exception("Failed to create application on server")
        }
    }

    private suspend fun handleUpdate(entity: JobApplicationEntity) {
        val response = apolloClient.mutation(
            UpdateJobApplicationStatusMutation(
                id = entity.id,
                status = entity.status
            )
        ).execute()

        if (!response.hasErrors()) {
            dao.updateApplication(entity.copy(syncStatus = SyncStatus.SYNCED))
        } else {
            throw Exception("Failed to update application on server")
        }
    }

    private suspend fun handleDelete(entity: JobApplicationEntity) {
        val response = apolloClient.mutation(
            DeleteJobApplicationMutation(id = entity.id)
        ).execute()

        if (!response.hasErrors()) {
            dao.deleteApplication(entity)
        } else {
            throw Exception("Failed to delete application on server")
        }
    }
}
