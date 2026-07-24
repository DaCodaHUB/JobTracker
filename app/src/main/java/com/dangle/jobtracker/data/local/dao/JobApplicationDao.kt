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

    @Query("SELECT * FROM job_applications")
    suspend fun getAllApplicationsSync(): List<JobApplicationEntity>

    @Query("SELECT * FROM job_applications WHERE syncStatus != 'SYNCED'")
    suspend fun getPendingApplications(): List<JobApplicationEntity>

    @Query("SELECT * FROM job_applications WHERE id = :id")
    suspend fun getApplicationById(id: String): JobApplicationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertApplication(entity: JobApplicationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertApplications(entities: List<JobApplicationEntity>)

    @Update
    suspend fun updateApplication(entity: JobApplicationEntity)

    @Query("""
        UPDATE job_applications 
        SET version = serverVersion, 
            syncStatus = 'PENDING_UPDATE', 
            serverCompany = NULL, 
            serverPositionTitle = NULL,
            serverStatus = NULL, 
            serverAppliedDate = NULL,
            serverVersion = NULL 
        WHERE id = :id
    """)
    suspend fun resolveKeepMine(id: String)

    @Query("""
        UPDATE job_applications 
        SET companyName = serverCompany, 
            positionTitle = serverPositionTitle,
            status = serverStatus, 
            appliedDate = serverAppliedDate,
            version = serverVersion, 
            syncStatus = 'SYNCED', 
            serverCompany = NULL, 
            serverPositionTitle = NULL,
            serverStatus = NULL, 
            serverAppliedDate = NULL,
            serverVersion = NULL 
        WHERE id = :id
    """)
    suspend fun resolveKeepServer(id: String)

    @Query("DELETE FROM job_applications WHERE id = :id")
    suspend fun deleteById(id: String)

    @Delete
    suspend fun deleteApplication(entity: JobApplicationEntity)

    @Delete
    suspend fun deleteApplications(entities: List<JobApplicationEntity>)
}
