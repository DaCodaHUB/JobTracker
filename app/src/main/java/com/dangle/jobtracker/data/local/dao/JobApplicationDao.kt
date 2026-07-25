package com.dangle.jobtracker.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.dangle.jobtracker.data.local.entity.JobApplicationEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for managing job applications in the local Room database.
 * 
 * This interface defines atomic operations for offline-first data management,
 * including primary queries, background sync filters, and manual conflict resolution.
 */
@Dao
interface JobApplicationDao {

    /**
     * Observes all job applications, sorted by date.
     * The sorting ensures the most recent applications appear at the top of the UI.
     */
    @Query("SELECT * FROM job_applications ORDER BY appliedDate DESC")
    fun getAllApplications(): Flow<List<JobApplicationEntity>>

    /**
     * Synchronous fetch of all applications, primarily used during sync reconciliation.
     */
    @Query("SELECT * FROM job_applications")
    suspend fun getAllApplicationsSync(): List<JobApplicationEntity>

    /**
     * Retrieves items that have local changes waiting to be pushed to the server.
     */
    @Query("SELECT * FROM job_applications WHERE syncStatus != 'SYNCED'")
    suspend fun getPendingApplications(): List<JobApplicationEntity>

    @Query("SELECT * FROM job_applications WHERE id = :id")
    suspend fun getApplicationById(id: String): JobApplicationEntity?

    /**
     * Standard insert using REPLACE strategy to handle simple ID-based updates.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertApplication(entity: JobApplicationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertApplications(entities: List<JobApplicationEntity>)

    @Update
    suspend fun updateApplication(entity: JobApplicationEntity)

    /**
     * Conflict Resolution: Overwrites server data with local state.
     * Sets the local version to the server's version to satisfy optimistic locking
     * during the next push, and clears the server snapshot fields.
     */
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

    /**
     * Conflict Resolution: Overwrites local state with server data.
     * Resets the item to SYNCED and clears the divergent snapshots.
     */
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

    /**
     * Hard delete by ID, used when a PENDING_CREATE item is deleted before reaching the server.
     */
    @Query("DELETE FROM job_applications WHERE id = :id")
    suspend fun deleteById(id: String)

    @Delete
    suspend fun deleteApplication(entity: JobApplicationEntity)

    @Delete
    suspend fun deleteApplications(entities: List<JobApplicationEntity>)
}
