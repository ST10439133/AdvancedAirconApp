package com.insy7315.advancedairconapp.viewmodels

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.insy7315.advancedairconapp.data.ArcticFlowDatabase
import com.insy7315.advancedairconapp.data.api.ApiRepository
import com.insy7315.advancedairconapp.data.api.TechLocationDto
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class ManagerTrackingViewModel(application: Application) : AndroidViewModel(application) {

    private val TAG = "ManagerTrackingVM"
    private val STALE_AFTER_MS = 2L * 60L * 1000L

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
            val db = ArcticFlowDatabase.getDatabase(getApplication())
            while (isActive) {
                try {
                    val raw = ApiRepository.fetchLocations(getApplication(), customerId)
                    val now = System.currentTimeMillis()

                    // Resolve destination coords locally from the Room DB
                    // by matching the building name.
                    val enriched = raw.map { loc ->
                        if (loc.destinationLatitude != 0.0 ||
                            loc.destinationLongitude != 0.0 ||
                            loc.buildingName.isNullOrBlank()
                        ) {
                            loc
                        } else {
                            val match = try {
                                db.buildingDao().findByNameWithCoords(loc.buildingName)
                            } catch (e: Exception) {
                                Log.w(TAG, "Building lookup failed for '${loc.buildingName}'", e)
                                null
                            }
                            if (match != null) {
                                Log.d(
                                    TAG,
                                    "Resolved dest for '${loc.buildingName}' → " +
                                            "${match.latitude}, ${match.longitude}"
                                )
                                loc.copy(
                                    destinationLatitude = match.latitude,
                                    destinationLongitude = match.longitude
                                )
                            } else {
                                Log.d(
                                    TAG,
                                    "No building row for '${loc.buildingName}' — " +
                                            "keeping tech with unknown destination"
                                )
                                loc
                            }
                        }
                    }

                    // Keep a technician if EITHER:
                    //   - their ping is fresh (< 2 min old), OR
                    //   - we couldn't resolve their destination (so we still
                    //     want to show the blue pin even without the green
                    //     house / dashed line).
                    val active = enriched.filter { loc ->
                        val hasDestination =
                            loc.destinationLatitude != 0.0 && loc.destinationLongitude != 0.0
                        if (!hasDestination) {
                            true
                        } else {
                            val age = now - loc.lastUpdated
                            age in 0..STALE_AFTER_MS
                        }
                    }

                    _technicians.value = active
                    _lastUpdated.value = now
                    Log.d(
                        TAG,
                        "polled ${raw.size} raw → ${active.size} fresh"
                    )
                } catch (e: Exception) {
                    Log.w(TAG, "poll failed: ${e.message}")
                }
                delay(5_000L)
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