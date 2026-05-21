package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ui.screens.ConnectionScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.DiagnosticsScreen
import com.example.ui.screens.FlightHistoryScreen
import com.example.ui.screens.FpvScreen
import com.example.ui.screens.MissionPlannerScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.DroneViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: DroneViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()

                    NavHost(
                        navController = navController,
                        startDestination = "splash"
                    ) {
                        composable("splash") {
                            SplashScreen(
                                onNavigateToNext = {
                                    navController.navigate("connection") {
                                        popUpTo("splash") { inclusive = true }
                                    }
                                }
                            )
                        }

                        composable("connection") {
                            ConnectionScreen(
                                viewModel = viewModel,
                                onNavigateToDashboard = { navController.navigate("dashboard") },
                                onNavigateToSettings = { navController.navigate("settings") }
                            )
                        }

                        composable("dashboard") {
                            DashboardScreen(
                                viewModel = viewModel,
                                onNavigateToFpv = { navController.navigate("fpv") },
                                onNavigateToPlanner = { navController.navigate("planner") },
                                onNavigateToDiagnostics = { navController.navigate("diagnostics") },
                                onNavigateToHistory = { navController.navigate("history") },
                                onNavigateToSettings = { navController.navigate("settings") }
                            )
                        }

                        composable("fpv") {
                            FpvScreen(
                                viewModel = viewModel,
                                onNavigateToDashboard = { navController.navigate("dashboard") },
                                onNavigateToPlanner = { navController.navigate("planner") },
                                onNavigateToDiagnostics = { navController.navigate("diagnostics") },
                                onNavigateToHistory = { navController.navigate("history") }
                            )
                        }

                        composable("planner") {
                            MissionPlannerScreen(
                                viewModel = viewModel,
                                onNavigateToDashboard = { navController.navigate("dashboard") },
                                onNavigateToFpv = { navController.navigate("fpv") },
                                onNavigateToDiagnostics = { navController.navigate("diagnostics") },
                                onNavigateToHistory = { navController.navigate("history") }
                            )
                        }

                        composable("diagnostics") {
                            DiagnosticsScreen(
                                viewModel = viewModel,
                                onNavigateToDashboard = { navController.navigate("dashboard") },
                                onNavigateToFpv = { navController.navigate("fpv") },
                                onNavigateToPlanner = { navController.navigate("planner") },
                                onNavigateToHistory = { navController.navigate("history") }
                            )
                        }

                        composable("history") {
                            FlightHistoryScreen(
                                viewModel = viewModel,
                                onNavigateToDashboard = { navController.navigate("dashboard") },
                                onNavigateToFpv = { navController.navigate("fpv") },
                                onNavigateToPlanner = { navController.navigate("planner") },
                                onNavigateToDiagnostics = { navController.navigate("diagnostics") }
                            )
                        }

                        composable("settings") {
                            SettingsScreen(
                                viewModel = viewModel,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}
