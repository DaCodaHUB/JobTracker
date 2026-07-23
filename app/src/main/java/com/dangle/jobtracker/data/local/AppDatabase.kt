package com.dangle.jobtracker.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.dangle.jobtracker.data.local.dao.JobApplicationDao
import com.dangle.jobtracker.data.local.entity.JobApplicationEntity
import com.dangle.jobtracker.domain.model.SyncStatus

@Database(entities = [JobApplicationEntity::class], version = 4, exportSchema = false)
@TypeConverters(AppDatabase.Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun jobApplicationDao(): JobApplicationDao

    class Converters {
        @TypeConverter
        fun fromSyncStatus(value: SyncStatus): String = value.name

        @TypeConverter
        fun toSyncStatus(value: String): SyncStatus = SyncStatus.valueOf(value)
    }
}
