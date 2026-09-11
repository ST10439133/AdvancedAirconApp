// app/src/main/java/com/prog7314/arcticflow/ui/screens/MainScreen.kt
package com.prog7314.arcticflow.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.prog7314.arcticflow.data.ArcticFlowDatabase
import com.prog7314.arcticflow.navigation.NavManager
import com.prog7314.arcticflow.ui.components.BottomNavItem
import com.prog7314.arcticflow.viewmodels.QuoteViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    userId: String,
    userRole: String,
    navManager: NavManager
) {
    val context = LocalContext.current
    val database = ArcticFlowDatabase.getDatabase(context)
    val bottomNavController = rememberNavController()

    val navBackStackEntry by bottomNavController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val navItems = if (userRole == "MANAGER") BottomNavItem.getManagerItems()
    else BottomNavItem.getTechnicianItems()

    val startDestination = BottomNavItem.Dashboard.route

    Scaffold(
        bottomBar = {
            NavigationBar(
                modifier = Modifier.height(64.dp),
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                navItems.forEach { item ->
                    NavigationBarItem(
                        icon = { Icon(item.icon, item.label, Modifier.size(24.dp)) },
                        label = {
                            Text(item.label,
                                style = MaterialTheme.typography.labelSmall)
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
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            NavHost(
                navController = bottomNavController,
                startDestination = startDestination
            ) {
                // ============ DASHBOARD ============
                composable(BottomNavItem.Dashboard.route) {
                    if (userRole == "MANAGER") {
                        ManagerDashboardScreen(
                            navManager = navManager,
                            userId = userId
                        )
                    } else {
                        TechnicianDashboardScreen(
                            navManager = navManager,
                            userId = userId
                        )
                    }
                }

                // ============ MANAGER TABS ============
                composable(BottomNavItem.Buildings.route) {
                    BuildingsScreen(
                        viewModel = viewModel(factory = QuoteViewModel.Factory(database)),
                        userId = userId,
                        navManager = navManager
                    )
                }

                composable(BottomNavItem.Quotes.route) {
                    QuotesListScreen(
                        viewModel = viewModel(factory = QuoteViewModel.Factory(database)),
                        userId = userId,
                        isCustomer = true,
                        navManager = navManager
                    )
                }

                composable(BottomNavItem.Services.route) {
                    ServicesScreen(
                        navManager = navManager,
                        userId = userId,
                        viewModel = viewModel(factory = QuoteViewModel.Factory(database))
                    )
                }

                // ============ TECHNICIAN TABS ============
                composable(BottomNavItem.Requests.route) {
                    PendingRequestsScreen(navManager = navManager)
                }

                composable(BottomNavItem.Bookings.route) {
                    ServiceBookingsScreen(
                        userId = userId,
                        navManager = navManager
                    )
                }

                composable(BottomNavItem.Jobs.route) {
                    JobsScreen(
                        userId = userId,
                        navManager = navManager
                    )
                }
            }
        }
    }
}