//IEEE Xplore. 2025. Advanced Data Collection and Visualization Techniques for Smart Building Management Systems. [Online]. Available at: https://ieeexplore.ieee.org/document/11029104 [Accessed: 5 October 2026].
// IEEE Xplore. 2024. Research on the Application of Digital Twin Technology in Intelligent Building Operation and Maintenance Management. [Online]. Available at: https://ieeexplore.ieee.org/document/10737251 [Accessed: 5 October 2026].
// IEEE Xplore. 2025. AI-Driven HVAC Energy Management Framework Using Digital Twin Technology for Commercial Buildings. [Online]. Available at: https://ieeexplore.ieee.org/document/10984721 [Accessed: 5 October 2026].

package com.insy7315.advancedairconapp.data.entities

data class DashboardStats(
    val totalBuildings: Int = 0,
    val totalUnits: Int = 0,
    val activeAlerts: Int = 0,
    val pendingMaintenance: Int = 0,
    val completedJobs: Int = 0,
    val technicianCount: Int = 0
)

data class Alert(
    val id: String,
    val buildingName: String,
    val issueType: String,
    val severity: AlertSeverity,
    val timestamp: Long,
    val status: AlertStatus = AlertStatus.PENDING
)

data class MaintenanceJob(
    val id: String,
    val buildingName: String,
    val unitNumber: String,
    val issueType: String,
    val priority: JobPriority,
    val assignedTo: String? = null,
    val status: JobStatus,
    val scheduledDate: Long? = null,
    val completedDate: Long? = null,
    val description: String = ""
)

data class Technician(
    val id: String,
    val name: String,
    val email: String,
    val phone: String? = null,
    val specialization: String? = null,
    val isAvailable: Boolean = true,
    val currentLocation: String? = null,
    val rating: Float = 0f
)

data class DashboardBuilding(
    val id: String,
    val name: String,
    val address: String,
    val floors: Int = 0,
    val units: Int = 0,
    val status: BuildingStatus = BuildingStatus.ACTIVE
)

enum class AlertSeverity {
    CRITICAL,
    HIGH,
    MEDIUM,
    LOW
}

enum class AlertStatus {
    PENDING,
    IN_PROGRESS,
    RESOLVED
}

enum class JobPriority {
    URGENT,
    HIGH,
    MEDIUM,
    LOW
}


enum class BuildingStatus {
    ACTIVE,
    MAINTENANCE,
    INACTIVE
}