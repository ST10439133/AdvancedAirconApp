package com.prog7314.arcticflow.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.prog7314.arcticflow.data.ArcticFlowDatabase
import com.prog7314.arcticflow.data.entities.TechLocation
import com.prog7314.arcticflow.data.network.LocationTrackingManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ManagerTrackingViewModel(
    private val database: ArcticFlowDatabase,
    private val managerId: String       // = customer UID
) : ViewModel() {

    private val _activeLocations = MutableStateFlow<List<TechLocation>>(emptyList())
    val activeLocations: StateFlow<List<TechLocation>> = _activeLocations.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _debugMessage = MutableStateFlow("")
    val debugMessage: StateFlow<String> = _debugMessage.asStateFlow()

    init {
        startTracking()
    }

    private fun startTracking() {
        viewModelScope.launch {
            try {
                Log.d("ManagerTracking", "=== START managerId=$managerId ===")

                // 1. Load this customer's jobs from Room
                val jobs = try {
                    database.jobDao().getJobsByCustomer(managerId).first()
                } catch (e: Exception) {
                    Log.e("ManagerTracking", "Jobs lookup failed", e)
                    emptyList()
                }

                val technicianIds: Set<String> = jobs
                    .mapNotNull { it.technicianId.takeIf { id -> id.isNotBlank() } }
                    .toSet()

                val jobIds: Set<Int> = jobs.map { it.id }.toSet()

                Log.d("ManagerTracking",
                    "Customer has ${jobs.size} jobs → techs=$technicianIds jobIds=$jobIds")

                _debugMessage.value =
                    "Jobs: ${jobs.size} | Techs: ${technicianIds.size}"

                // 2. Stream Firestore. We pass customerId + jobIds so the
                //    filter is very precise. If techIds is empty, that's OK —
                //    we still filter by customerId + jobId.
                LocationTrackingManager
                    .streamActiveLocations(
                        customerId = managerId,
                        technicianIds = technicianIds.ifEmpty { null },
                        jobIds = jobIds.ifEmpty { null }
                    )
                    .collect { locations ->
                        Log.d("ManagerTracking",
                            "Live update: ${locations.size} active locations")
                        _activeLocations.value = locations
                        _isLoading.value = false
                    }
            } catch (e: Exception) {
                Log.e("ManagerTracking", "Tracking failed", e)
                _isLoading.value = false
            }
        }
    }

    companion object {
        fun Factory(database: ArcticFlowDatabase, managerId: String): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return ManagerTrackingViewModel(database, managerId) as T
                }
            }
    }
}