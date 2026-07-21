package com.dangle.jobtracker.data.repository

import com.apollographql.apollo.ApolloClient
import com.dangle.jobtracker.GetJobApplicationsQuery
import com.dangle.jobtracker.data.network.ApolloClientProvider
import com.dangle.jobtracker.domain.model.ApplicationStatus
import com.dangle.jobtracker.domain.model.JobApplication
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class JobApplicationRepository(
    private val apolloClient: ApolloClient = ApolloClientProvider.client
) {
    suspend fun getApplications(): Result<List<JobApplication>> = withContext(Dispatchers.IO) {
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
                        // Map the GraphQL string/enum to your domain ApplicationStatus
                        status = queryItem.status.toApplicationStatus(),
                        appliedDate = queryItem.appliedDate
                    )
                } ?: emptyList()

                Result.success(items)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

// Helper function to safely convert string to ApplicationStatus
private fun String?.toApplicationStatus(): ApplicationStatus {
    if (this == null) return ApplicationStatus.APPLIED
    return ApplicationStatus.entries.find {
        it.name.equals(this, ignoreCase = true)
    } ?: ApplicationStatus.APPLIED // Fallback default if no match
}