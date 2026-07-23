package com.dangle.jobtracker.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import com.dangle.jobtracker.data.local.AppDatabase
import com.dangle.jobtracker.data.local.dao.JobApplicationDao

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "job_application_database"
        ).build()
    }

    @Provides
    @Singleton
    fun provideJobApplicationDao(database: AppDatabase): JobApplicationDao {
        return database.jobApplicationDao()
    }
}