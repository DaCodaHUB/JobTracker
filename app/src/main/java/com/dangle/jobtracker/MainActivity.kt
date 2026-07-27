package com.dangle.jobtracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.dangle.jobtracker.domain.model.ThemeConfig
import com.dangle.jobtracker.ui.MainViewModel
import com.dangle.jobtracker.ui.application.ApplicationDetailScreen
import com.dangle.jobtracker.ui.application.JobApplicationRoute
import com.dangle.jobtracker.ui.application.JobApplicationViewModel
import com.dangle.jobtracker.ui.list.ApplicationListEvent
import com.dangle.jobtracker.ui.list.ApplicationListRoute
import com.dangle.jobtracker.ui.list.ApplicationListViewModel
import com.dangle.jobtracker.ui.theme.JobTrackerTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @OptIn(ExperimentalSharedTransitionApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContent {
            val mainViewModel: MainViewModel = hiltViewModel()
            val themeConfig by mainViewModel.themeConfig.collectAsState()

            val darkTheme = when (themeConfig) {
                ThemeConfig.FOLLOW_SYSTEM -> isSystemInDarkTheme()
                ThemeConfig.LIGHT -> false
                ThemeConfig.DARK -> true
            }

            JobTrackerTheme(darkTheme = darkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize()
                ) {
                    val navController = rememberNavController()

                    SharedTransitionLayout {
                        NavHost(
                            navController = navController,
                            startDestination = "main_graph"
                        ) {
                            navigation(
                                startDestination = Screen.ApplicationList.route,
                                route = "main_graph"
                            ) {
                                composable(Screen.ApplicationList.route) { backStackEntry ->
                                    val parentEntry = remember(backStackEntry) {
                                        navController.getBackStackEntry("main_graph")
                                    }
                                    val listViewModel: ApplicationListViewModel = hiltViewModel(parentEntry)

                                    ApplicationListRoute(
                                        viewModel = listViewModel,
                                        sharedTransitionScope = this@SharedTransitionLayout,
                                        animatedVisibilityScope = this@composable,
                                        onNavigateToDetail = { applicationId ->
                                            navController.navigate(Screen.ApplicationDetail.createRoute(applicationId))
                                        },
                                        onNavigateToAddApplication = {
                                            navController.navigate(Screen.AddApplication.route)
                                        }
                                    )
                                }

                                composable(
                                    route = Screen.ApplicationDetail.route,
                                    arguments = listOf(navArgument("applicationId") { type = NavType.StringType })
                                ) { backStackEntry ->
                                    val applicationId = backStackEntry.arguments?.getString("applicationId")
                                    val parentEntry = remember(backStackEntry) {
                                        navController.getBackStackEntry("main_graph")
                                    }
                                    val listViewModel: ApplicationListViewModel = hiltViewModel(parentEntry)
                                    val uiState by listViewModel.uiState.collectAsState()
                                    val application = uiState.applications.find { it.id == applicationId }

                                    application?.let {
                                        ApplicationDetailScreen(
                                            application = it,
                                            animatedVisibilityScope = this@composable,
                                            onStatusChange = { newStatus ->
                                                listViewModel.onEvent(ApplicationListEvent.UpdateApplicationStatus(it.id, newStatus))
                                            },
                                            onNotesChange = { newNotes ->
                                                listViewModel.onEvent(ApplicationListEvent.UpdateNotes(it.id, newNotes))
                                            },
                                            onBackClick = { navController.popBackStack() }
                                        )
                                    }
                                }
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
}
