package com.dangle.jobtracker.data.worker

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.exception.ApolloNetworkException
import com.dangle.jobtracker.data.local.dao.JobApplicationDao
import com.dangle.jobtracker.data.local.entity.JobApplicationEntity
import com.dangle.jobtracker.domain.model.SyncStatus
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
class SyncJobApplicationsWorkerTest {

    private lateinit var context: Context
    private val dao: JobApplicationDao = mockk()
    private val apolloClient: ApolloClient = mockk()

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun `doWork returns Result retry on network exception`() = runTest {
        val pendingEntity = JobApplicationEntity(
            id = "1", companyName = "Co", positionTitle = "Dev",
            status = "APPLIED", appliedDate = "2024-01-01",
            syncStatus = SyncStatus.PENDING_CREATE
        )
        coEvery { dao.getPendingApplications() } returns listOf(pendingEntity)
        
        // Mocking apollo mutation to throw network exception
        coEvery { apolloClient.mutation(any<com.dangle.jobtracker.CreateJobApplicationMutation>()).execute() } throws ApolloNetworkException("No connection")

        val worker = TestListenableWorkerBuilder<SyncJobApplicationsWorker>(context)
            .setWorkerFactory(object : androidx.work.WorkerFactory() {
                override fun createWorker(
                    appContext: Context,
                    workerClassName: String,
                    workerParameters: WorkerParameters
                ): ListenableWorker? {
                    return SyncJobApplicationsWorker(appContext, workerParameters, dao, apolloClient)
                }
            })
            .build()

        val result = worker.doWork()
        assertEquals(ListenableWorker.Result.retry(), result)
    }
}
