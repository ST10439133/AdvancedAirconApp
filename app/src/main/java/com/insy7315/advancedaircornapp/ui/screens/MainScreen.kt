// app/src/main/java/com/insy7315/advancedaircornapp/ui/screens/MainScreen.kt
package com.insy7315.advancedaircornapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.insy7315.advancedaircornapp.data.ArcticFlowDatabase
import com.insy7315.advancedaircornapp.navigation.NavManager
import com.insy7315.advancedaircornapp.ui.components.BottomNavItem
import com.insy7315.advancedaircornapp.ui.components.OfflineBanner
import com.insy7315.advancedaircornapp.viewmodels.QuoteViewModel

// BRAND TOKENS
private val BabyBlueSoft = Color(0xFFE1F1FB)
private val BabyBlueDeep = Color(0xFF2E7BA6)

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

    val navigateToDashboard: () -> Unit = {
        bottomNavController.navigate(BottomNavItem.Dashboard.route) {
            popUpTo(bottomNavController.graph.startDestinationId) {
                inclusive = false
                saveState = false
            }
            launchSingleTop = true
            restoreState = false
        }
    }

    val navigateToRequests: () -> Unit = {
        bottomNavController.navigate(BottomNavItem.Requests.route) {
            popUpTo(bottomNavController.graph.startDestinationId) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    Scaffold(
        bottomBar = {
            NavigationBar(
                modifier = Modifier.height(76.dp),
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                navItems.forEach { item ->
                    val selected = currentRoute == item.route
                    NavigationBarItem(
                        icon = {
                            Icon(
                                item.icon,
                                item.label,
                                Modifier.size(22.dp)
                            )
                        },
                        label = {
                            Text(
                                item.label,
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1
                            )
                        },
                        selected = selected,
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = BabyBlueDeep,
                            selectedTextColor = BabyBlueDeep,
                            indicatorColor = BabyBlueSoft,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            OfflineBanner()
            Box(Modifier.fillMaxSize()) {
                NavHost(
                    navController = bottomNavController,
                    startDestination = startDestination
                ) {
                    // DASHBOARD
                    composable(BottomNavItem.Dashboard.route) {
                        if (userRole == "MANAGER") {
                            ManagerDashboardScreen(
                                navManager = navManager,
                                userId = userId
                            )
                        } else {
                            TechnicianDashboardScreen(
                                navManager = navManager,
                                userId = userId,
                                onViewRequests = navigateToRequests
                            )
                        }
                    }

                    // MANAGER TABS
                    composable(BottomNavItem.Buildings.route) {
                        BuildingsScreen(
                            viewModel = viewModel(
                                factory = QuoteViewModel.Factory(
                                    database,
                                    context
                                )
                            ),
                            userId = userId,
                            navManager = navManager,
                            onBackToDashboard = navigateToDashboard
                        )
                    }

                    // QUOTES TAB

                    composable(BottomNavItem.Quotes.route) {
                        QuotesListScreen(
                            viewModel = viewModel(
                                factory = QuoteViewModel.Factory(
                                    database,
                                    context
                                )
                            ),
                            userId = userId,
                            isCustomer = (userRole == "MANAGER"),
                            navManager = navManager,
                            onBackToDashboard = navigateToDashboard
                        )
                    }

                    composable(BottomNavItem.Services.route) {
                        ServicesScreen(
                            navManager = navManager,
                            userId = userId,
                            viewModel = viewModel(
                                factory = QuoteViewModel.Factory(
                                    database,
                                    context
                                )
                            ),
                            onBackToDashboard = navigateToDashboard
                        )
                    }

                    composable(BottomNavItem.Products.route) {
                        ProductListScreen(
                            viewModel = viewModel(),
                            navManager = navManager,
                            onBackToDashboard = navigateToDashboard
                        )
                    }

                    // TRACKING TAB
                    composable(BottomNavItem.Tracking.route) {
                        FieldTrackingScreen(
                            navManager = navManager,
                            onBackToDashboard = navigateToDashboard
                        )
                    }

                    // TECHNICIAN TABS
                    composable(BottomNavItem.Requests.route) {
                        PendingRequestsScreen(
                            navManager = navManager,
                            onBackToDashboard = navigateToDashboard
                        )
                    }

                    composable(BottomNavItem.Bookings.route) {
                        ServiceBookingsScreen(
                            userId = userId,
                            navManager = navManager,
                            onBackToDashboard = navigateToDashboard
                        )
                    }

                    composable(BottomNavItem.Jobs.route) {
                        JobsScreen(
                            userId = userId,
                            navManager = navManager,
                            onBackToDashboard = navigateToDashboard
                        )
                    }
                }
            }
        }
    }
}