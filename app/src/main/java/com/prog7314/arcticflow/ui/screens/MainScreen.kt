// app/src/main/java/com/prog7314/arcticflow/ui/screens/MainScreen.kt
package com.prog7314.arcticflow.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.prog7314.arcticflow.auth.AuthViewModel
import com.prog7314.arcticflow.navigation.NavManager
import com.prog7314.arcticflow.ui.components.BottomNavItem
import com.prog7314.arcticflow.ui.theme.ThemeState
import com.prog7314.arcticflow.viewmodels.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    userId: String,
    userRole: String,
    navManager: NavManager,
    themeState: ThemeState,
    onThemeChange: (ThemeState) -> Unit
) {
    val bottomNavController = rememberNavController()

    // Get the current route for highlighting the correct bottom nav item
    val navBackStackEntry by bottomNavController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Get the appropriate nav items based on role
    val navItems = if (userRole == "MANAGER") {
        BottomNavItem.getManagerItems()
    } else {
        BottomNavItem.getTechnicianItems()
    }

    // Determine start destination
    val startDestination = if (userRole == "MANAGER") {
        BottomNavItem.Dashboard.route
    } else {
        BottomNavItem.Dashboard.route
    }

    Scaffold(
        bottomBar = {
            NavigationBar(
                modifier = Modifier.height(64.dp),
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                navItems.forEach { item ->
                    NavigationBarItem(
                        icon = {
                            Icon(
                                item.icon,
                                contentDescription = item.label,
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        label = {
                            Text(
                                item.label,
                                style = MaterialTheme.typography.labelSmall
                            )
                        },
                        selected = currentRoute == item.route,
                        onClick = {
                            bottomNavController.navigate(item.route) {
                                popUpTo(bottomNavController.graph.startDestinationId) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            NavHost(
                navController = bottomNavController,
                startDestination = startDestination
            ) {
                // Dashboard - Common
                composable(BottomNavItem.Dashboard.route) {
                    when (userRole) {
                        "MANAGER" -> {
                            val viewModel: ManagerDashboardViewModel = viewModel()
                            ManagerDashboardScreen(
                                viewModel = viewModel,
                                navManager = navManager,
                                userId = userId
                            )
                        }
                        else -> {
                            val viewModel: TechnicianDashboardViewModel = viewModel(
                                factory = TechnicianDashboardViewModel.Factory(userId)
                            )
                            TechnicianDashboardScreen(
                                viewModel = viewModel,
                                navManager = navManager,
                                userId = userId
                            )
                        }
                    }
                }

                // Manager only screens
                composable(BottomNavItem.Buildings.route) {
                    val viewModel: QuoteViewModel = viewModel()
                    BuildingsScreen(
                        viewModel = viewModel,
                        userId = userId,
                        navManager = navManager
                    )
                }

                composable(BottomNavItem.Quotes.route) {
                    val viewModel: QuoteViewModel = viewModel()
                    QuotesListScreen(
                        viewModel = viewModel,
                        userId = userId,
                        isCustomer = userRole == "MANAGER",
                        navManager = navManager
                    )
                }

                composable(BottomNavItem.Services.route) {
                    ServiceRequestScreen(
                        viewModel = viewModel(),
                        userId = userId,
                        navManager = navManager
                    )
                }

                // Technician only screens
                composable(BottomNavItem.Bookings.route) {
                    ServiceBookingsScreen(
                        userId = userId,
                        navManager = navManager
                    )
                }

                composable(BottomNavItem.Jobs.route) {
                    // Jobs list screen
                    JobsScreen(
                        userId = userId,
                        navManager = navManager
                    )
                }

                // Settings - Common
                composable(BottomNavItem.Settings.route) {
                    val authViewModel: AuthViewModel = viewModel()
                    SettingsScreen(
                        authViewModel = authViewModel,
                        navManager = navManager,
                        onThemeChange = onThemeChange,
                        currentTheme = themeState
                    )
                }
            }
        }
    }
}