// JobTrackerDestinations.kt
package com.dangle.jobtracker

sealed class Screen(val route: String) {
    data object ApplicationList : Screen("application_list")
    data object AddApplication : Screen("add_application")
    data object ApplicationDetail : Screen("application_detail/{applicationId}") {
        fun createRoute(applicationId: String) = "application_detail/$applicationId"
    }
}