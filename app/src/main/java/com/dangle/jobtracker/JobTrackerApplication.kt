package com.dangle.jobtracker

import android.app.Application
import com.dangle.jobtracker.data.local.AppDatabase
import com.dangle.jobtracker.data.repository.JobApplicationRepository
import com.dangle.jobtracker.data.repository.JobApplicationRepositoryImpl
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class JobTrackerApplication : Application() {

    // Database singleton
    val database: AppDatabase by lazy { AppDatabase.getDatabase(this) }

    // Repository singleton
    val repository: JobApplicationRepository by lazy {
        JobApplicationRepositoryImpl(
            dao = database.jobApplicationDao()
        )
    }
}
