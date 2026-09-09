package com.prog7314.arcticflow.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.prog7314.arcticflow.data.entities.*
import com.prog7314.arcticflow.data.repository.DashboardRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ManagerDashboardViewModel(
    private val repository: DashboardRepository = DashboardRepository()
) : ViewModel() {

    val stats = repository.stats
    val alerts = repository.alerts
    val maintenanceJobs = repository.maintenanceJobs
    val technicians = repository.technicians
    val buildings = repository.buildings

    private val _selectedJobId = MutableStateFlow<String?>(null)
    val selectedJobId: StateFlow<String?> = _selectedJobId.asStateFlow()

    private val _selectedAlertId = MutableStateFlow<String?>(null)
    val selectedAlertId: StateFlow<String?> = _selectedAlertId.asStateFlow()

    private val _showJobDetails = MutableStateFlow(false)
    val showJobDetails: StateFlow<Boolean> = _showJobDetails.asStateFlow()

    private val _showAlertDetails = MutableStateFlow(false)
    val showAlertDetails: StateFlow<Boolean> = _showAlertDetails.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun assignJobToTechnician(jobId: String, technicianId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.assignJob(jobId, technicianId)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun resolveAlert(alertId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.resolveAlert(alertId)
            } finally {
                _isLoading.value = false
            }
        }
    }

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

    fun selectJob(jobId: String?) {
        _selectedJobId.value = jobId
        _showJobDetails.value = jobId != null
    }

    fun selectAlert(alertId: String?) {
        _selectedAlertId.value = alertId
        _showAlertDetails.value = alertId != null
    }

    fun dismissDetails() {
        _showJobDetails.value = false
        _showAlertDetails.value = false
        _selectedJobId.value = null
        _selectedAlertId.value = null
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(ManagerDashboardViewModel::class.java)) {
                    return ManagerDashboardViewModel() as T
                }
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }
    }
}