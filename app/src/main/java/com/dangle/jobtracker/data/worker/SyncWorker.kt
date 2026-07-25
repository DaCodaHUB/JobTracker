package com.dangle.jobtracker.data.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.dangle.jobtracker.data.repository.JobApplicationRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val repository: JobApplicationRepository
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val TAG = "SyncWorker"
    }

    override suspend fun doWork(): Result {
        Log.d(TAG, "Starting synchronization...")
        
        val pushResult = repository.pushPendingMutations()
        if (pushResult.isFailure) {
            Log.e(TAG, "Failed to push pending mutations")
            return Result.retry()
        }

        val pullResult = repository.pullRemoteUpdates()
        if (pullResult.isFailure) {
            Log.e(TAG, "Failed to pull remote updates")
            return Result.retry()
        }

        Log.d(TAG, "Synchronization completed successfully")
        return Result.success()
    }
}
