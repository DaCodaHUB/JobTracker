// JobTrackerDestinations.kt
package com.dangle.jobtracker

sealed class Screen(val route: String) {
    data object ApplicationList : Screen("application_list")
    data object AddApplication : Screen("add_application")
}