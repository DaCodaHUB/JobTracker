package com.dangle.jobtracker.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.dangle.jobtracker.data.local.entity.JobApplicationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface JobApplicationDao {

    @Query("SELECT * FROM job_applications ORDER BY appliedDate DESC")
    fun getAllApplications(): Flow<List<JobApplicationEntity>>

    @Query("SELECT * FROM job_applications WHERE isPendingSync = 1")
    suspend fun getPendingApplications(): List<JobApplicationEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertApplication(entity: JobApplicationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertApplications(entities: List<JobApplicationEntity>)

    @Update
    suspend fun updateApplication(entity: JobApplicationEntity)

    @Delete
    suspend fun deleteApplication(entity: JobApplicationEntity)

    @Delete
    suspend fun deleteApplications(entities: List<JobApplicationEntity>)
}
