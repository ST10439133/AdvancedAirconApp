package com.prog7314.arcticflow.data.repository

import com.prog7314.arcticflow.data.entities.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class DashboardRepository {

    private val _stats = MutableStateFlow(
        DashboardStats(
            totalBuildings = 12,
            totalUnits = 45,
            activeAlerts = 3,
            pendingMaintenance = 8,
            completedJobs = 156,
            technicianCount = 6
        )
    )
    val stats: StateFlow<DashboardStats> = _stats.asStateFlow()

    private val _alerts = MutableStateFlow<List<Alert>>(getSampleAlerts())
    val alerts: StateFlow<List<Alert>> = _alerts.asStateFlow()

    private val _maintenanceJobs = MutableStateFlow<List<MaintenanceJob>>(getSampleJobs())
    val maintenanceJobs: StateFlow<List<MaintenanceJob>> = _maintenanceJobs.asStateFlow()

    private val _technicians = MutableStateFlow<List<Technician>>(getSampleTechnicians())
    val technicians: StateFlow<List<Technician>> = _technicians.asStateFlow()


    private val _buildings = MutableStateFlow<List<DashboardBuilding>>(getSampleBuildings())
    val buildings: StateFlow<List<DashboardBuilding>> = _buildings.asStateFlow()

    fun updateJobStatus(jobId: String, status: JobStatus) {
        val currentJobs = _maintenanceJobs.value
        val updatedJobs = currentJobs.map { job ->
            if (job.id == jobId) {
                job.copy(
                    status = status,
                    completedDate = if (status == JobStatus.COMPLETED) System.currentTimeMillis() else job.completedDate
                )
            } else {
                job
            }
        }
        _maintenanceJobs.value = updatedJobs
        updateStats()
    }

    fun assignJob(jobId: String, technicianId: String) {
        val currentJobs = _maintenanceJobs.value
        val updatedJobs = currentJobs.map { job ->
            if (job.id == jobId) {
                job.copy(
                    assignedTo = technicianId,
                    status = JobStatus.ASSIGNED
                )
            } else {
                job
            }
        }
        _maintenanceJobs.value = updatedJobs
        updateStats()
    }

    fun resolveAlert(alertId: String) {
        val currentAlerts = _alerts.value
        val updatedAlerts = currentAlerts.map { alert ->
            if (alert.id == alertId) {
                alert.copy(status = AlertStatus.RESOLVED)
            } else {
                alert
            }
        }
        _alerts.value = updatedAlerts
        updateStats()
    }

    private fun updateStats() {
        val jobs = _maintenanceJobs.value
        val alerts = _alerts.value

        _stats.value = _stats.value.copy(
            activeAlerts = alerts.count { it.status == AlertStatus.PENDING || it.status == AlertStatus.IN_PROGRESS },
            pendingMaintenance = jobs.count { it.status == JobStatus.PENDING || it.status == JobStatus.ASSIGNED },
            completedJobs = jobs.count { it.status == JobStatus.COMPLETED }
        )
    }

    private fun getSampleAlerts(): List<Alert> = listOf(
        Alert(
            id = "A001",
            buildingName = "Sandton Office Tower",
            issueType = "Compressor Failure",
            severity = AlertSeverity.CRITICAL,
            timestamp = System.currentTimeMillis() - 1800000,
            status = AlertStatus.PENDING
        ),
        Alert(
            id = "A002",
            buildingName = "Rosebank Mall",
            issueType = "Refrigerant Leak",
            severity = AlertSeverity.HIGH,
            timestamp = System.currentTimeMillis() - 3600000,
            status = AlertStatus.IN_PROGRESS
        ),
        Alert(
            id = "A003",
            buildingName = "Midrand Data Center",
            issueType = "Filter Replacement",
            severity = AlertSeverity.MEDIUM,
            timestamp = System.currentTimeMillis() - 7200000,
            status = AlertStatus.PENDING
        )
    )

    private fun getSampleJobs(): List<MaintenanceJob> = listOf(
        MaintenanceJob(
            id = "J001",
            buildingName = "Sandton Office Tower",
            unitNumber = "Unit 4B",
            issueType = "Compressor Replacement",
            priority = JobPriority.URGENT,
            status = JobStatus.PENDING,
            scheduledDate = System.currentTimeMillis() + 86400000,
            description = "Compressor not starting, tripping circuit breaker"
        ),
        MaintenanceJob(
            id = "J002",
            buildingName = "Rosebank Mall",
            unitNumber = "Unit 12A",
            issueType = "Gas Leak",
            priority = JobPriority.HIGH,
            assignedTo = "T001",
            status = JobStatus.ASSIGNED,
            scheduledDate = System.currentTimeMillis() + 172800000,
            description = "Refrigerant leak detected in unit 12A"
        ),
        MaintenanceJob(
            id = "J003",
            buildingName = "Midrand Data Center",
            unitNumber = "Unit 7C",
            issueType = "Filter Change",
            priority = JobPriority.MEDIUM,
            assignedTo = "T003",
            status = JobStatus.IN_PROGRESS,
            scheduledDate = System.currentTimeMillis(),
            description = "Regular filter maintenance"
        ),
        MaintenanceJob(
            id = "J004",
            buildingName = "Fourways Office Park",
            unitNumber = "Unit 2A",
            issueType = "Thermostat Calibration",
            priority = JobPriority.LOW,
            status = JobStatus.PENDING,
            scheduledDate = System.currentTimeMillis() + 259200000,
            description = "Temperature readings inconsistent"
        ),
        MaintenanceJob(
            id = "J005",
            buildingName = "Sandton Office Tower",
            unitNumber = "Unit 8F",
            issueType = "Fan Motor Replacement",
            priority = JobPriority.HIGH,
            assignedTo = "T002",
            status = JobStatus.ASSIGNED,
            scheduledDate = System.currentTimeMillis() + 86400000,
            description = "Fan motor making unusual noise"
        )
    )

    private fun getSampleTechnicians(): List<Technician> = listOf(
        Technician(
            id = "T001",
            name = "John Doe",
            email = "john.doe@arcticflow.com",
            phone = "+27123456789",
            specialization = "Commercial HVAC",
            isAvailable = true,
            rating = 4.8f
        ),
        Technician(
            id = "T002",
            name = "Jane Smith",
            email = "jane.smith@arcticflow.com",
            phone = "+27123456790",
            specialization = "Residential HVAC",
            isAvailable = false,
            rating = 4.6f
        ),
        Technician(
            id = "T003",
            name = "Mike Johnson",
            email = "mike.johnson@arcticflow.com",
            phone = "+27123456791",
            specialization = "Industrial HVAC",
            isAvailable = true,
            rating = 4.9f
        )
    )

    private fun getSampleBuildings(): List<DashboardBuilding> = listOf(
        DashboardBuilding(
            id = "B001",
            name = "Sandton Office Tower",
            address = "123 Sandton Drive, Sandton, Johannesburg",
            floors = 15,
            units = 45,
            status = BuildingStatus.ACTIVE
        ),
        DashboardBuilding(
            id = "B002",
            name = "Rosebank Mall",
            address = "456 Rosebank Road, Rosebank, Johannesburg",
            floors = 5,
            units = 30,
            status = BuildingStatus.ACTIVE
        ),
        DashboardBuilding(
            id = "B003",
            name = "Midrand Data Center",
            address = "789 Data Street, Midrand, Johannesburg",
            floors = 3,
            units = 12,
            status = BuildingStatus.ACTIVE
        ),
        DashboardBuilding(
            id = "B004",
            name = "Fourways Office Park",
            address = "101 Fourways Blvd, Fourways, Johannesburg",
            floors = 8,
            units = 24,
            status = BuildingStatus.ACTIVE
        )
    )
}