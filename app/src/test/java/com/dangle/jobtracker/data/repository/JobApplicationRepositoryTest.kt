package com.dangle.jobtracker.data.repository

import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import app.cash.turbine.test
import com.apollographql.apollo.ApolloClient
import com.dangle.jobtracker.data.local.dao.JobApplicationDao
import com.dangle.jobtracker.data.local.entity.JobApplicationEntity
import com.dangle.jobtracker.domain.model.ApplicationStatus
import com.dangle.jobtracker.domain.model.SyncStatus
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class JobApplicationRepositoryTest {

    private val apolloClient: ApolloClient = mockk()
    private val dao: JobApplicationDao = mockk()
    private val workManager: WorkManager = mockk(relaxed = true)
    private val testDispatcher = StandardTestDispatcher()
    
    private lateinit var repository: JobApplicationRepositoryImpl

    @Before
    fun setup() {
        repository = JobApplicationRepositoryImpl(
            apolloClient = apolloClient,
            dao = dao,
            workManager = workManager,
            ioDispatcher = testDispatcher
        )
    }

    @Test
    fun `getApplications correctly collects and emits data from DAO`() = runTest(testDispatcher) {
        val entities = listOf(
            JobApplicationEntity(
                id = "1", companyName = "Co 1", positionTitle = "Dev",
                status = "APPLIED", appliedDate = "2024-01-01"
            )
        )
        every { dao.getAllApplications() } returns flowOf(entities)

        repository.getApplications().test {
            val result = awaitItem()
            assertEquals(1, result.size)
            assertEquals("Co 1", result[0].companyName)
            awaitComplete()
        }
    }

    @Test
    fun `createApplication inserts with PENDING_CREATE and enqueues sync`() = runTest(testDispatcher) {
        coEvery { dao.insertApplication(any()) } returns Unit
        
        val result = repository.createApplication(
            companyName = "New Co",
            positionTitle = "Senior Dev",
            status = ApplicationStatus.INTERVIEWING,
            appliedDate = "2024-02-01"
        )

        assertEquals(true, result.isSuccess)
        val createdApp = result.getOrNull()
        assertEquals("New Co", createdApp?.companyName)
        assertEquals(SyncStatus.PENDING_CREATE, createdApp?.syncStatus)

        coVerify { 
            dao.insertApplication(match { 
                it.companyName == "New Co" && it.syncStatus == SyncStatus.PENDING_CREATE 
            }) 
        }
        coVerify { 
            workManager.enqueueUniqueWork(
                eq("SyncJobApplicationsWork"),
                any<ExistingWorkPolicy>(),
                any<OneTimeWorkRequest>()
            )
        }
    }
}
