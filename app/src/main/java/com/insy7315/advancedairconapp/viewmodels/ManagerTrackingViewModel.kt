// app/src/main/java/com/insy7315/advancedairconapp/viewmodels/ManagerTrackingViewModel.kt
package com.insy7315.advancedairconapp.viewmodels

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.core.content.edit
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
import org.json.JSONArray
import org.json.JSONObject

class ManagerTrackingViewModel(application: Application) : AndroidViewModel(application) {

    private val tag = "ManagerTrackingVM"

    /** Anything not COMPLETED goes stale after 2 minutes. */
    private val staleAfterMs = 2L * 60L * 1000L

    /** COMPLETED markers stay on the map this long, then drop off. */
    private val completedLingerMs = 30L * 60L * 1000L

    private val _technicians = MutableStateFlow<List<TechLocationDto>>(emptyList())
    val technicians: StateFlow<List<TechLocationDto>> = _technicians.asStateFlow()

    private val _lastUpdated = MutableStateFlow(0L)
    val lastUpdated: StateFlow<Long> = _lastUpdated.asStateFlow()

    private val _isPolling = MutableStateFlow(false)
    val isPolling: StateFlow<Boolean> = _isPolling.asStateFlow()

    private var pollJob: Job? = null

    // ============================================================
    // Local sticky cache for COMPLETED techs.
    //
    // The backend may filter completed rows out of GET /api/locations.
    // To keep the green "Completed" marker visible we remember them
    // here and re-attach them on every poll until they age out.
    //
    // Stored in SharedPreferences as JSON so it also survives a cold start.
    // ============================================================
    private val prefs by lazy {
        getApplication<Application>()
            .getSharedPreferences("arcticflow_tracking_cache", Context.MODE_PRIVATE)
    }

    private fun loadCompletedCache(): MutableMap<String, CachedCompleted> {
        val map = mutableMapOf<String, CachedCompleted>()
        val raw = prefs.getString(KEY_COMPLETED, null) ?: return map

        try {
            val arr = JSONArray(raw)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val id = obj.optString("technicianId")
                if (id.isBlank()) continue

                val dto = TechLocationDto(
                    technicianId = id,
                    technicianName = obj.optString("technicianName"),
                    latitude = obj.optDouble("latitude", 0.0),
                    longitude = obj.optDouble("longitude", 0.0),
                    jobId = obj.optInt("jobId", 0).takeIf { it != 0 },
                    customerId = obj.optString("customerId").takeIf { it.isNotBlank() },
                    buildingName = obj.optString("buildingName").takeIf { it.isNotBlank() },
                    isOnMyWay = false,
                    lastUpdated = obj.optLong("lastUpdated", System.currentTimeMillis()),
                    status = "COMPLETED",
                    destinationLatitude = obj.optDouble("destinationLatitude", 0.0),
                    destinationLongitude = obj.optDouble("destinationLongitude", 0.0)
                )

                map[id] = CachedCompleted(
                    dto = dto,
                    markedCompletedAt = obj.optLong(
                        "markedCompletedAt",
                        System.currentTimeMillis()
                    )
                )
            }
        } catch (e: Exception) {
            Log.w(tag, "Failed to parse completed cache", e)
        }

        return map
    }

    private fun saveCompletedCache(map: Map<String, CachedCompleted>) {
        try {
            val arr = JSONArray()
            for ((_, v) in map) {
                val o = JSONObject()
                o.put("technicianId", v.dto.technicianId)
                o.put("technicianName", v.dto.technicianName ?: "")
                o.put("latitude", v.dto.latitude)
                o.put("longitude", v.dto.longitude)
                o.put("jobId", v.dto.jobId ?: 0)
                o.put("customerId", v.dto.customerId ?: "")
                o.put("buildingName", v.dto.buildingName ?: "")
                o.put("lastUpdated", v.dto.lastUpdated)
                o.put("destinationLatitude", v.dto.destinationLatitude)
                o.put("destinationLongitude", v.dto.destinationLongitude)
                o.put("markedCompletedAt", v.markedCompletedAt)
                arr.put(o)
            }
            prefs.edit { putString(KEY_COMPLETED, arr.toString()) }
        } catch (e: Exception) {
            Log.w(tag, "Failed to save completed cache", e)
        }
    }

    /**
     * Public entry point so the tech-side can flip the cache when a job
     * card is submitted.
     */
    fun markTechnicianCompleted(dto: TechLocationDto) {
        val map = loadCompletedCache()
        map[dto.technicianId] = CachedCompleted(
            dto = dto.copy(status = "COMPLETED", isOnMyWay = false),
            markedCompletedAt = System.currentTimeMillis()
        )
        saveCompletedCache(map)
        Log.d(tag, "Cached COMPLETED for ${dto.technicianId}")
    }

    /** Remove a tech from the completed cache (e.g. when they start a new job). */
    fun clearCompletedCacheFor(technicianId: String) {
        val map = loadCompletedCache()
        if (map.remove(technicianId) != null) {
            saveCompletedCache(map)
            Log.d(tag, "Cleared COMPLETED cache for $technicianId")
        }
    }

    fun startPolling(customerId: String? = null) {
        if (pollJob?.isActive == true) return
        _isPolling.value = true

        pollJob = viewModelScope.launch {
            val db = ArcticFlowDatabase.getDatabase(getApplication())

            // Prime the completed cache once at start.
            val completedCache = loadCompletedCache()

            while (isActive) {
                try {
                    val raw = ApiRepository.fetchLocations(getApplication(), customerId)
                    val now = System.currentTimeMillis()

                    // 1. Resolve destination coords locally for any tech
                    //    the server didn't enrich.
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
                                Log.w(
                                    tag,
                                    "Building lookup failed for '${loc.buildingName}'",
                                    e
                                )
                                null
                            }
                            if (match != null) {
                                loc.copy(
                                    destinationLatitude = match.latitude,
                                    destinationLongitude = match.longitude
                                )
                            } else loc
                        }
                    }

                    // 2. Any tech the server now reports as COMPLETED gets
                    //    added to / refreshed in our local sticky cache.
                    enriched.forEach { loc ->
                        if (loc.status.equals("COMPLETED", ignoreCase = true)) {
                            completedCache[loc.technicianId] = CachedCompleted(
                                dto = loc.copy(status = "COMPLETED", isOnMyWay = false),
                                markedCompletedAt = now
                            )
                        }
                    }

                    // 3. Any tech the server now reports as NOT completed
                    //    (i.e. they came back on the way to another job) is
                    //    removed from the cache.
                    val serverIds = enriched.map { it.technicianId }.toSet()
                    val staleCompletedKeys = mutableListOf<String>()
                    for ((techId, cached) in completedCache) {
                        val serverSaysCompleted =
                            enriched.firstOrNull { it.technicianId == techId }
                                ?.status
                                ?.equals("COMPLETED", ignoreCase = true)
                                ?: false
                        if (techId in serverIds && !serverSaysCompleted && cached.dto.status
                                .equals("COMPLETED", ignoreCase = true)
                        ) {
                            // Server is now returning this tech with a
                            // non-COMPLETED status — they're on a new job.
                            staleCompletedKeys.add(techId)
                        }
                    }
                    staleCompletedKeys.forEach { completedCache.remove(it) }

                    // 4. Expire cached entries older than COMPLETED_LINGER_MS.
                    val expiredKeys = mutableListOf<String>()
                    for ((techId, cached) in completedCache) {
                        if ((now - cached.markedCompletedAt) > completedLingerMs) {
                            expiredKeys.add(techId)
                        }
                    }
                    expiredKeys.forEach { completedCache.remove(it) }

                    // 5. Compute the "live" set the server gave us,
                    //    excluding anything the server still calls COMPLETED
                    //    (they'll come from the cache).
                    val liveFromServer = enriched.filter { loc ->
                        val isCompleted =
                            loc.status.equals("COMPLETED", ignoreCase = true)
                        if (isCompleted) return@filter false

                        val hasDestination =
                            loc.destinationLatitude != 0.0 &&
                                    loc.destinationLongitude != 0.0
                        if (!hasDestination) {
                            true
                        } else {
                            val age = now - loc.lastUpdated
                            age in 0..staleAfterMs
                        }
                    }

                    // 6. Merge live-from-server + cached-completed.
                    val merged = (liveFromServer + completedCache.values.map { it.dto })
                        .distinctBy { it.technicianId }

                    _technicians.value = merged
                    _lastUpdated.value = now

                    saveCompletedCache(completedCache)

                    Log.d(
                        tag,
                        "polled ${raw.size} raw → ${liveFromServer.size} live + " +
                                "${completedCache.size} cached-completed = ${merged.size} shown"
                    )
                } catch (e: Exception) {
                    Log.w(tag, "poll failed: ${e.message}")
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

    private data class CachedCompleted(
        val dto: TechLocationDto,
        val markedCompletedAt: Long
    )

    companion object {
        private const val KEY_COMPLETED = "completed_techs"

        /**
         * Static helper so `JobCardViewModel` (which doesn't have this VM
         * instance) can still drop a completed tech into the cache.
         * Call this right after pushing COMPLETED.
         */
        fun cacheCompletedTechnician(context: Context, dto: TechLocationDto) {
            val prefs = context.applicationContext
                .getSharedPreferences("arcticflow_tracking_cache", Context.MODE_PRIVATE)

            val existing = prefs.getString(KEY_COMPLETED, null)
            val arr = try {
                if (existing.isNullOrBlank()) JSONArray() else JSONArray(existing)
            } catch (_: Exception) {
                JSONArray()
            }

            // Remove any prior entry for this tech
            val cleaned = JSONArray()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                if (o.optString("technicianId") != dto.technicianId) cleaned.put(o)
            }

            val o = JSONObject().apply {
                put("technicianId", dto.technicianId)
                put("technicianName", dto.technicianName ?: "")
                put("latitude", dto.latitude)
                put("longitude", dto.longitude)
                put("jobId", dto.jobId ?: 0)
                put("customerId", dto.customerId ?: "")
                put("buildingName", dto.buildingName ?: "")
                put("lastUpdated", System.currentTimeMillis())
                put("destinationLatitude", dto.destinationLatitude)
                put("destinationLongitude", dto.destinationLongitude)
                put("markedCompletedAt", System.currentTimeMillis())
            }
            cleaned.put(o)

            prefs.edit { putString(KEY_COMPLETED, cleaned.toString()) }
            Log.d("ManagerTrackingVM", "Static cache: COMPLETED ${dto.technicianId}")
        }
    }
}