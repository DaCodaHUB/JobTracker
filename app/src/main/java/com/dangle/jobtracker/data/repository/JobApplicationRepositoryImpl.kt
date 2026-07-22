package com.dangle.jobtracker.data.repository

import com.apollographql.apollo.ApolloClient
import com.dangle.jobtracker.CreateJobApplicationMutation
import com.dangle.jobtracker.GetJobApplicationsQuery
import com.dangle.jobtracker.UpdateJobApplicationStatusMutation
import com.dangle.jobtracker.data.network.ApolloClientProvider
import com.dangle.jobtracker.domain.model.ApplicationStatus
import com.dangle.jobtracker.domain.model.JobApplication
import com.dangle.jobtracker.type.CreateJobApplicationInput
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.withContext

class JobApplicationRepositoryImpl(
    private val apolloClient: ApolloClient = ApolloClientProvider.client
) : JobApplicationRepository {

    companion object {
        private val refreshSignal = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    override fun observeApplications(): Flow<List<JobApplication>> = refreshSignal
        .onStart { emit(Unit) }
        .flatMapLatest {
            flow {
                emit(getApplications().getOrDefault(emptyList()))
            }
        }

    override suspend fun getApplications(): Result<List<JobApplication>> = withContext(Dispatchers.IO) {
        try {
            val response = apolloClient.query(GetJobApplicationsQuery()).execute()

            if (response.hasErrors()) {
                val errorMessage = response.errors?.firstOrNull()?.message ?: "Unknown GraphQL Error"
                Result.failure(Exception(errorMessage))
            } else {
                val items = response.data?.jobApplications?.map { queryItem ->
                    JobApplication(
                        id = queryItem.id,
                        companyName = queryItem.companyName,
                        positionTitle = queryItem.positionTitle,
                        status = ApplicationStatus.fromString(queryItem.status),
                        appliedDate = queryItem.appliedDate
                    )
                } ?: emptyList()

                Result.success(items)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun createApplication(
        companyName: String,
        positionTitle: String,
        status: ApplicationStatus,
        appliedDate: String
    ): Result<JobApplication> = withContext(Dispatchers.IO) {
        try {
            val response = apolloClient.mutation(
                CreateJobApplicationMutation(
                    input = CreateJobApplicationInput(
                        companyName = companyName,
                        positionTitle = positionTitle,
                        status = status.name,
                        appliedDate = appliedDate
                    )
                )
            ).execute()

            val data = response.data?.createJobApplication
            if (data != null) {
                refreshSignal.tryEmit(Unit)
                Result.success(
                    JobApplication(
                        id = data.id,
                        companyName = data.companyName,
                        positionTitle = data.positionTitle,
                        status = ApplicationStatus.fromString(data.status),
                        appliedDate = data.appliedDate
                    )
                )
            } else {
                Result.failure(Exception("Failed to create application"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateStatus(id: String, newStatus: ApplicationStatus): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = apolloClient.mutation(
                UpdateJobApplicationStatusMutation(id = id, status = newStatus.name)
            ).execute()

            if (response.hasErrors()) {
                Result.failure(Exception(response.errors?.firstOrNull()?.message ?: "Update failed"))
            } else {
                refreshSignal.tryEmit(Unit)
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}