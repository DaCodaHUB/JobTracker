package com.dangle.jobtracker.data.local.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.dangle.jobtracker.data.local.AppDatabase
import com.dangle.jobtracker.data.local.entity.JobApplicationEntity
import com.dangle.jobtracker.domain.model.SyncStatus
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class JobApplicationDaoTest {

    private lateinit var dao: JobApplicationDao
    private lateinit var db: AppDatabase

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        dao = db.jobApplicationDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    @Throws(Exception::class)
    fun insertAndReplaceConflict() = runBlocking {
        val entity = JobApplicationEntity(
            id = "1",
            companyName = "Test Co",
            positionTitle = "Developer",
            status = "APPLIED",
            appliedDate = "2024-01-01",
            location = "London",
            jobUrl = "https://test.com",
            notes = "Test notes",
            idempotencyKey = "key-1",
            syncStatus = SyncStatus.PENDING_UPDATE,
            version = 1
        )
        dao.insertApplication(entity)

        // Simulate sync success by replacing the same ID with SYNCED status and incremented version
        val syncedEntity = entity.copy(
            syncStatus = SyncStatus.SYNCED,
            version = 2
        )
        dao.insertApplication(syncedEntity)

        val result = dao.getApplicationById("1")
        assertNotNull(result)
        assertEquals(SyncStatus.SYNCED, result?.syncStatus)
        assertEquals(2, result?.version)
        
        // Ensure no duplicate rows were created
        val all = dao.getAllApplicationsSync()
        assertEquals(1, all.size)
    }

    @Test
    fun deleteApplicationCorrectly() = runBlocking {
        val entity = JobApplicationEntity(
            id = "1",
            companyName = "Test Co",
            positionTitle = "Developer",
            status = "APPLIED",
            appliedDate = "2024-01-01",
            syncStatus = SyncStatus.SYNCED
        )
        dao.insertApplication(entity)
        dao.deleteApplication(entity)

        val result = dao.getApplicationById("1")
        assertEquals(null, result)
    }
}
