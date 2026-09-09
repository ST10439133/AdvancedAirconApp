package com.prog7314.arcticflow.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.prog7314.arcticflow.data.entities.*
import com.prog7314.arcticflow.data.repository.DashboardRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class TechnicianDashboardViewModel(
    private val repository: DashboardRepository = DashboardRepository(),
    private val technicianId: String
) : ViewModel() {

    // All jobs assigned to this technician
    val myJobs = repository.maintenanceJobs
        .map { jobs -> jobs.filter { it.assignedTo == technicianId } }

    val stats = repository.stats

    // Technician's own stats
    val myStats = myJobs.map { jobs ->
        DashboardStats(
            totalBuildings = jobs.map { it.buildingName }.distinct().size,
            totalUnits = jobs.size,
            activeAlerts = 0,
            pendingMaintenance = jobs.count { it.status == JobStatus.PENDING || it.status == JobStatus.ASSIGNED },
            completedJobs = jobs.count { it.status == JobStatus.COMPLETED },
            technicianCount = 0
        )
    }

    val allTechnicians = repository.technicians

    private val _selectedJobId = MutableStateFlow<String?>(null)
    val selectedJobId: StateFlow<String?> = _selectedJobId.asStateFlow()

    private val _showJobDetails = MutableStateFlow(false)
    val showJobDetails: StateFlow<Boolean> = _showJobDetails.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isAvailable = MutableStateFlow(true)
    val isAvailable: StateFlow<Boolean> = _isAvailable.asStateFlow()

    fun updateJobStatus(jobId: String, status: JobStatus) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.updateJobStatus(jobId, status)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun toggleAvailability() {
        _isAvailable.value = !_isAvailable.value
    }

    fun selectJob(jobId: String?) {
        _selectedJobId.value = jobId
        _showJobDetails.value = jobId != null
    }

    fun dismissDetails() {
        _showJobDetails.value = false
        _selectedJobId.value = null
    }

    companion object {
        fun Factory(technicianId: String): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(TechnicianDashboardViewModel::class.java)) {
                        return TechnicianDashboardViewModel(
                            repository = DashboardRepository(),
                            technicianId = technicianId
                        ) as T
                    }
                    throw IllegalArgumentException("Unknown ViewModel class")
                }
            }
        }
    }
}