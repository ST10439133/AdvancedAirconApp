//IEEE Xplore. 2024. Real-Time GPS Tracking and Geofencing System for Fleet Management Using Cloud Services. [Online]. Available at: https://ieeexplore.ieee.org/document/10533567 [Accessed: 5 October 2026].
//IEEE Xplore. 2025. Hybrid Cloud Architecture for Real-Time Asset Tracking: Combining Firestore and REST Backends. [Online]. Available at: https://ieeexplore.ieee.org/document/10890348 [Accessed: 5 October 2026].

package com.insy7315.advancedairconapp.viewmodels

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.insy7315.advancedairconapp.data.api.TechLocationDto
import com.insy7315.advancedairconapp.data.network.LocationTrackingManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ManagerTrackingViewModel(application: Application) : AndroidViewModel(application) {

    private val tag = "ManagerTrackingVM"

    private val _technicians = MutableStateFlow<List<TechLocationDto>>(emptyList())
    val technicians: StateFlow<List<TechLocationDto>> = _technicians.asStateFlow()

    private val _lastUpdated = MutableStateFlow(0L)
    val lastUpdated: StateFlow<Long> = _lastUpdated.asStateFlow()

    private val _isPolling = MutableStateFlow(false)
    val isPolling: StateFlow<Boolean> = _isPolling.asStateFlow()

    private var pollJob: Job? = null

    fun startPolling(customerId: String? = null) {
        if (pollJob?.isActive == true) return
        _isPolling.value = true

        pollJob = viewModelScope.launch {
            try {
                // Listen directly to Firestore for live technician updates
                LocationTrackingManager.streamActiveLocations(customerId).collect { firestoreTechs ->

                    Log.d(tag, "Firestore emitted ${firestoreTechs.size} live technicians")

                    // Map the Firestore entities to the DTO the UI expects
                    val mappedTechs = firestoreTechs.map { loc ->
                        TechLocationDto(
                            technicianId = loc.technicianId,
                            technicianName = loc.technicianName,
                            latitude = loc.latitude,
                            longitude = loc.longitude,
                            jobId = loc.jobId.takeIf { it != 0 },
                            customerId = loc.customerId.takeIf { it.isNotBlank() },
                            buildingName = loc.buildingName.takeIf { it.isNotBlank() },
                            isOnMyWay = loc.onMyWay,
                            lastUpdated = loc.lastUpdated,
                            status = loc.status,
                            destinationLatitude = loc.destinationLatitude,
                            destinationLongitude = loc.destinationLongitude
                        )
                    }

                    _technicians.value = mappedTechs
                    _lastUpdated.value = System.currentTimeMillis()
                }
            } catch (e: Exception) {
                Log.e(tag, "Firestore stream failed", e)
            }
        }
    }

    fun stopPolling() {
        pollJob?.cancel()
        pollJob = null
        _isPolling.value = false
    }

    override fun onCleared() {
        stopPolling()
        super.onCleared()
    }
}