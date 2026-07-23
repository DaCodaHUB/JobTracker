package com.dangle.jobtracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.dangle.jobtracker.ui.application.JobApplicationRoute
import com.dangle.jobtracker.ui.application.JobApplicationViewModel
import com.dangle.jobtracker.ui.list.ApplicationListRoute
import com.dangle.jobtracker.ui.list.ApplicationListViewModel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContent {
            val app = (application as JobTrackerApplication)
            val repository = app.repository

            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()

                    NavHost(
                        navController = navController,
                        startDestination = Screen.ApplicationList.route
                    ) {
                        composable("application_list") {
                            val viewModel: ApplicationListViewModel = viewModel(
                                factory = viewModelFactory {
                                    initializer {
                                        ApplicationListViewModel(repository)
                                    }
                                }
                            )
                            ApplicationListRoute(
                                viewModel = viewModel,
                                onNavigateToAddApplication = {
                                    navController.navigate(Screen.AddApplication.route)
                                }
                            )
                        }

                        composable(Screen.AddApplication.route) {
                            val viewModel: JobApplicationViewModel = viewModel(
                                factory = viewModelFactory {
                                    initializer {
                                        JobApplicationViewModel(repository)
                                    }
                                }
                            )
                            JobApplicationRoute(
                                viewModel = viewModel,
                                onBackClick = {
                                    navController.popBackStack()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
