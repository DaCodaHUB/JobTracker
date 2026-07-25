package com.dangle.jobtracker

import android.app.Application
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.dangle.jobtracker.data.repository.JobApplicationRepository
import com.dangle.jobtracker.data.sync.SyncManager
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class JobTrackerApplication : Application(), Configuration.Provider {

    companion object {
        private const val TAG = "JobTrackerApp"
    }

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var repository: JobApplicationRepository

    @Inject
    lateinit var syncManager: SyncManager

    @Inject
    lateinit var applicationScope: CoroutineScope

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Application created")
        syncManager.startMonitoring()
        startSubscription()
    }

    private fun startSubscription() {
        applicationScope.launch {
            repository.observeRealtimeUpdates().collect { }
        }
    }

    override val workManagerConfiguration: Configuration
        get() {
            Log.d(TAG, "WorkManager configuration requested")
            return Configuration.Builder()
                .setWorkerFactory(workerFactory)
                .setMinimumLoggingLevel(Log.DEBUG)
                .build()
        }
}
