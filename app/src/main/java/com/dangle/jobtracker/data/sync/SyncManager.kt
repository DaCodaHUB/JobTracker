package com.dangle.jobtracker.data.sync

import android.content.Context
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import com.dangle.jobtracker.data.worker.SyncWorker
import com.dangle.jobtracker.util.NetworkMonitor
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val networkMonitor: NetworkMonitor,
    private val externalScope: CoroutineScope
) {
    companion object {
        private const val TAG = "SyncManager"
        private const val SYNC_WORK_NAME = "JobTrackerSyncWork"
    }

    fun startMonitoring() {
        externalScope.launch {
            networkMonitor.isOnline.collectLatest { isOnline ->
                Log.d(TAG, "Network status changed: isOnline = $isOnline")
                if (isOnline) {
                    scheduleSync()
                }
            }
        }
    }

    private fun scheduleSync() {
        Log.d(TAG, "Scheduling sync work...")
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                WorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS
            )
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            SYNC_WORK_NAME,
            ExistingWorkPolicy.KEEP, // Keep existing work if already running
            syncRequest
        )
    }
}
