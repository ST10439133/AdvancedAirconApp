// app/src/main/java/com/prog7314/arcticflow/ui/components/BottomNavItems.kt
package com.prog7314.arcticflow.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class BottomNavItem(
    val route: String,
    val icon: ImageVector,
    val label: String
) {
    // Common
    object Dashboard : BottomNavItem(
        route = "dashboard",
        icon = Icons.Default.Dashboard,
        label = "Dashboard"
    )

    // Manager Tabs
    object Buildings : BottomNavItem(
        route = "buildings",
        icon = Icons.Default.Business,
        label = "Buildings"
    )

    object Quotes : BottomNavItem(
        route = "quotes",
        icon = Icons.Default.Receipt,
        label = "Quotes"
    )

    object Services : BottomNavItem(
        route = "services",
        icon = Icons.Default.Build,
        label = "Services"
    )

    // Technician Tabs
    object Bookings : BottomNavItem(
        route = "bookings",
        icon = Icons.Default.Book,
        label = "Bookings"
    )

    object Jobs : BottomNavItem(
        route = "jobs",
        icon = Icons.Default.Work,
        label = "Jobs"
    )

    companion object {
        fun getManagerItems(): List<BottomNavItem> = listOf(
            Dashboard,
            Buildings,
            Quotes,
            Services
        )

        fun getTechnicianItems(): List<BottomNavItem> = listOf(
            Dashboard,
            Quotes,
            Bookings,
            Jobs
        )
    }
}