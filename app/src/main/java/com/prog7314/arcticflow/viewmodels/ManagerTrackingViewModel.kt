// app/src/main/java/com/prog7314/arcticflow/viewmodels/ManagerTrackingViewModel.kt
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
    private val managerId: String
) : ViewModel() {

    private val _activeLocations = MutableStateFlow<List<TechLocation>>(emptyList())
    val activeLocations: StateFlow<List<TechLocation>> = _activeLocations.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        startTracking()
    }

    private fun startTracking() {
        viewModelScope.launch {
            try {
                Log.d("ManagerTracking", "Starting for manager=$managerId")

                // 1. Try to find the technician IDs from the manager's jobs (Room DB)
                val jobs = try {
                    database.jobDao().getJobsByCustomer(managerId).first()
                } catch (e: Exception) {
                    Log.e("ManagerTracking", "Jobs lookup failed", e)
                    emptyList()
                }

                val technicianIds: Set<String> = jobs
                    .mapNotNull { it.technicianId.takeIf { id -> id.isNotBlank() } }
                    .toSet()

                Log.d("ManagerTracking",
                    "Manager has ${jobs.size} jobs, " +
                            "found ${technicianIds.size} technicians: $technicianIds")

                // 2. Stream Firestore locations
                //    If we found specific technician IDs, filter by them.
                //    Otherwise, stream ALL active locations (so testing works even without jobs).
                val filter = if (technicianIds.isNotEmpty()) technicianIds else null

                LocationTrackingManager.streamActiveLocations(filter)
                    .collect { locations ->
                        Log.d("ManagerTracking", "Received ${locations.size} locations")
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