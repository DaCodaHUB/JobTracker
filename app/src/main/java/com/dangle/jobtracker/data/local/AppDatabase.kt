package com.dangle.jobtracker.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.dangle.jobtracker.data.local.dao.JobApplicationDao
import com.dangle.jobtracker.data.local.entity.JobApplicationEntity

@Database(entities = [JobApplicationEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun jobApplicationDao(): JobApplicationDao
}
