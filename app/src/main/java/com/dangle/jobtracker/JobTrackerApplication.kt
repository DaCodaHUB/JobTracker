package com.dangle.jobtracker

import android.app.Application
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.dangle.jobtracker.data.repository.JobApplicationRepository
import com.dangle.jobtracker.util.ConnectivityObserver
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
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
    lateinit var connectivityObserver: ConnectivityObserver

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Application created")
        observeConnectivity()
    }

    private fun observeConnectivity() {
        applicationScope.launch {
            connectivityObserver.isConnected.collectLatest { isConnected ->
                Log.d(TAG, "Connectivity changed: isConnected = $isConnected")
                if (isConnected) {
                    Log.d(TAG, "Network restored, refreshing and scheduling sync")
                    repository.refreshApplications()
                    repository.scheduleSync()
                }
            }
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
