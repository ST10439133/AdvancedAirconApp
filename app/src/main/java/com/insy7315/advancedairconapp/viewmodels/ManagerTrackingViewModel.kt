// app/src/main/java/com/insy7315/advancedairconapp/viewmodels/ManagerTrackingViewModel.kt
package com.insy7315.advancedairconapp.viewmodels

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
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

    /**
     * Anything older than this is considered stale and hidden from the map.
     * 2 minutes is generous — the service pushes every 10s.
     */
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
            while (isActive) {
                try {
                    val raw = ApiRepository.fetchLocations(getApplication(), customerId)
                    val now = System.currentTimeMillis()
                    val active = raw.filter { loc ->
                        val age = now - loc.lastUpdated
                        val fresh = age in 0..STALE_AFTER_MS
                        if (!fresh) {
                            Log.d(
                                TAG,
                                "filtering out stale tech=${loc.technicianId} " +
                                        "ageMs=$age"
                            )
                        }
                        fresh
                    }
                    _technicians.value = active
                    _lastUpdated.value = now
                    Log.d(
                        TAG,
                        "polled ${raw.size} raw → ${active.size} fresh" +
                                if (customerId != null) " (customer=$customerId)" else ""
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