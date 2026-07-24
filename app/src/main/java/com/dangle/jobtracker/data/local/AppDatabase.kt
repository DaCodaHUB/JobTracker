package com.dangle.jobtracker.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.dangle.jobtracker.data.local.dao.JobApplicationDao
import com.dangle.jobtracker.data.local.entity.JobApplicationEntity
import com.dangle.jobtracker.domain.model.SyncStatus

@Database(entities = [JobApplicationEntity::class], version = 7, exportSchema = false)
@TypeConverters(AppDatabase.Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun jobApplicationDao(): JobApplicationDao

    class Converters {
        @TypeConverter
        fun fromSyncStatus(value: SyncStatus): String = value.name

        @TypeConverter
        fun toSyncStatus(value: String): SyncStatus = SyncStatus.valueOf(value)
    }

    companion object {
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE job_applications ADD COLUMN version INTEGER NOT NULL DEFAULT 1")
            }
        }
    }
}
