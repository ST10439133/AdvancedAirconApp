// app/src/main/java/com/prog7314/arcticflow/navigation/NavManager.kt
package com.prog7314.arcticflow.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController

@Composable
fun rememberNavManager(
    navController: NavHostController = rememberNavController()
): NavManager {
    return remember(navController) { NavManager(navController) }
}

class NavManager(
    val navController: NavHostController
) {

    sealed class Destination(val route: String) {
        object Login : Destination("login")
        object Register : Destination("register")
        object Main : Destination("main")
        object ProductList : Destination("product_list")
        object ManagerDashboard : Destination("manager_dashboard")
        object TechnicianDashboard : Destination("technician_dashboard")
        object Settings : Destination("settings")
        object Profile : Destination("profile")
        object EditProfile : Destination("edit_profile")
        object AddBuilding : Destination("add_building")
        object ServiceRequest : Destination("service_request")
        object CreateQuote : Destination("create_quote/{requestId}") {
            fun passRequestId(requestId: Int) = "create_quote/$requestId"
        }
        object MyQuotes : Destination("my_quotes")
        object PendingRequests : Destination("pending_requests")
        object Notifications : Destination("notifications")
        object CreateJobCard : Destination("create_job_card/{jobId}") {
            fun passJobId(jobId: Int) = "create_job_card/$jobId"
        }
        object ServiceBookings : Destination("service_bookings")
        object BTUCalculator : Destination("btu_calculator")
        object FieldTracking : Destination("field_tracking")
        object ProductsList : Destination("products_list")
        object QuoteHistory : Destination("quote_history")
        object AddBuildings : Destination("add_buildings")
        object JobDetails : Destination("job_details/{jobId}") {
            fun passJobId(jobId: Int) = "job_details/$jobId"
        }
    }

    fun navigateToLogin() {
        navController.navigate(Destination.Login.route) {
            popUpTo(0) { inclusive = true }
        }
    }

    fun navigateToRegister() {
        navController.navigate(Destination.Register.route)
    }

    fun navigateToMain() {
        navController.navigate(Destination.Main.route) {
            popUpTo(0) { inclusive = true }
            launchSingleTop = true
        }
    }

    fun navigateToProductList() {
        navController.navigate(Destination.ProductList.route) {
            popUpTo(0) { inclusive = true }
        }
    }

    fun navigateToManagerDashboard() {
        navController.navigate(Destination.ManagerDashboard.route) {
            popUpTo(0) { inclusive = true }
            launchSingleTop = true
        }
    }

    fun navigateToTechnicianDashboard() {
        navController.navigate(Destination.TechnicianDashboard.route) {
            popUpTo(0) { inclusive = true }
            launchSingleTop = true
        }
    }

    fun navigateToSettings() {
        navController.navigate(Destination.Settings.route)
    }

    fun navigateToProfile() {
        navController.navigate(Destination.Profile.route)
    }

    fun navigateToEditProfile() {
        navController.navigate(Destination.EditProfile.route)
    }

    fun navigateToAddBuilding() {
        navController.navigate(Destination.AddBuilding.route)
    }

    fun navigateToServiceRequest() {
        navController.navigate(Destination.ServiceRequest.route)
    }

    fun navigateToCreateQuote(requestId: Int) {
        navController.navigate(Destination.CreateQuote.passRequestId(requestId))
    }

    fun navigateToMyQuotes() {
        navController.navigate(Destination.MyQuotes.route)
    }

    fun navigateToPendingRequests() {
        navController.navigate(Destination.PendingRequests.route)
    }

    fun navigateToNotifications() {
        navController.navigate(Destination.Notifications.route)
    }

    fun navigateToCreateJobCard(jobId: Int) {
        navController.navigate(Destination.CreateJobCard.passJobId(jobId))
    }

    fun navigateToServiceBookings() {
        navController.navigate(Destination.ServiceBookings.route)
    }

    fun navigateToBTUCalculator() {
        navController.navigate(Destination.BTUCalculator.route)
    }

    fun navigateToFieldTracking() {
        navController.navigate(Destination.FieldTracking.route)
    }

    fun navigateToProductsList() {
        navController.navigate(Destination.ProductsList.route)
    }

    fun navigateToQuoteHistory() {
        navController.navigate(Destination.QuoteHistory.route)
    }

    fun navigateToAddBuildings() {
        navController.navigate(Destination.AddBuildings.route)
    }

    fun navigateToJobDetails(jobId: Int) {
        navController.navigate(Destination.JobDetails.passJobId(jobId))
    }

    fun navigateBack() {
        navController.popBackStack()
    }

    fun navigateBackTo(destination: Destination) {
        navController.popBackStack(destination.route, inclusive = false)
    }

    fun clearBackStackAndNavigateTo(destination: Destination) {
        navController.navigate(destination.route) {
            popUpTo(0) { inclusive = true }
        }
    }
}