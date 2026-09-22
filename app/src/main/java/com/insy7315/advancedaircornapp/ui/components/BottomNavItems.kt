// app/src/main/java/com/insy7315/advancedaircornapp/ui/components/BottomNavItems.kt
package com.insy7315.advancedaircornapp.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class BottomNavItem(
    val route: String,
    val icon: ImageVector,
    val label: String
) {
    object Dashboard : BottomNavItem("dashboard", Icons.Default.Dashboard, "Dashboard")
    object Buildings : BottomNavItem("buildings", Icons.Default.Business, "Buildings")
    object Quotes : BottomNavItem("quotes", Icons.Default.Receipt, "Quotes")
    object Services : BottomNavItem("services", Icons.Default.Build, "Services")
    object Products : BottomNavItem("products", Icons.Default.Inventory2, "Products")
    object Tracking : BottomNavItem("tracking", Icons.Default.LocationOn, "Tracking")   // ← NEW

    object Requests : BottomNavItem("requests", Icons.Default.Inbox, "Requests")
    object Bookings : BottomNavItem("bookings", Icons.Default.Book, "Bookings")
    object Jobs : BottomNavItem("jobs", Icons.Default.Work, "Jobs")

    companion object {
        fun getManagerItems(): List<BottomNavItem> =
            listOf(Dashboard, Buildings, Quotes, Services, Products, Tracking)

        fun getTechnicianItems(): List<BottomNavItem> =
            listOf(Dashboard, Requests, Quotes, Bookings, Jobs)
    }
}