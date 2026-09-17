package com.prog7314.arcticflow.viewmodels

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.prog7314.arcticflow.data.api.ApiRepository
import com.prog7314.arcticflow.data.api.TechLocationDto
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

// ============================================================
// Polls GET /api/locations every 5 seconds and exposes the
// list of active technicians as a StateFlow. Replaces the
// previous Firestore-based live-tracking implementation.
//
// Usage (from a Composable):
//   val vm: ManagerTrackingViewModel = viewModel()
//   val techs by vm.technicians.collectAsStateWithLifecycle()
//   LaunchedEffect(Unit) { vm.startPolling(customerId = null) }
//   DisposableEffect(Unit) { onDispose { vm.stopPolling() } }
// ============================================================
class ManagerTrackingViewModel(application: Application) : AndroidViewModel(application) {

    private val TAG = "ManagerTrackingVM"

    private val _technicians = MutableStateFlow<List<TechLocationDto>>(emptyList())
    val technicians: StateFlow<List<TechLocationDto>> = _technicians.asStateFlow()

    private val _lastUpdated = MutableStateFlow(0L)
    val lastUpdated: StateFlow<Long> = _lastUpdated.asStateFlow()

    private val _isPolling = MutableStateFlow(false)
    val isPolling: StateFlow<Boolean> = _isPolling.asStateFlow()

    private var pollJob: Job? = null

    /**
     * Start polling the REST API.
     *
     * @param customerId  when non-null, only returns the technician assigned
     *                    to that customer (used by the customer's tracking screen).
     *                    Pass null for the manager view (all technicians).
     */
    fun startPolling(customerId: String? = null) {
        if (pollJob?.isActive == true) return
        _isPolling.value = true
        pollJob = viewModelScope.launch {
            while (isActive) {
                try {
                    val list = ApiRepository.fetchLocations(getApplication(), customerId)
                    _technicians.value = list
                    _lastUpdated.value = System.currentTimeMillis()
                    Log.d(
                        TAG,
                        "polled ${list.size} active technicians" +
                                if (customerId != null) " for customer=$customerId" else ""
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