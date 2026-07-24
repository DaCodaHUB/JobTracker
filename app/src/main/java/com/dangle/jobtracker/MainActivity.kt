package com.dangle.jobtracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.dangle.jobtracker.ui.application.JobApplicationRoute
import com.dangle.jobtracker.ui.application.JobApplicationViewModel
import com.dangle.jobtracker.ui.list.ApplicationListRoute
import com.dangle.jobtracker.ui.list.ApplicationListViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContent {
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
                            val listViewModel: ApplicationListViewModel = hiltViewModel()

                            ApplicationListRoute(
                                viewModel = listViewModel,
                                onNavigateToAddApplication = {
                                    navController.navigate(Screen.AddApplication.route)
                                }
                            )
                        }

                        composable(Screen.AddApplication.route) {
                            val addViewModel: JobApplicationViewModel = hiltViewModel()

                            JobApplicationRoute(
                                viewModel = addViewModel,
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
