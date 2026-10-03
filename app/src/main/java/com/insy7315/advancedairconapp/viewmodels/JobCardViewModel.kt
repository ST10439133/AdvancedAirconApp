package com.insy7315.advancedairconapp.viewmodels

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.insy7315.advancedairconapp.data.ArcticFlowDatabase
import com.insy7315.advancedairconapp.data.api.ApiClient
import com.insy7315.advancedairconapp.data.api.ApiRepository
import com.insy7315.advancedairconapp.data.api.TechLocationDto
import com.insy7315.advancedairconapp.data.entities.*
import com.insy7315.advancedairconapp.data.network.LocationTrackingManager
import com.insy7315.advancedairconapp.data.network.NetworkMonitor
import com.insy7315.advancedairconapp.services.TechLocationService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class JobCardViewModel(
    private val database: ArcticFlowDatabase,
    private val appContext: Context
) : ViewModel() {

    private val TAG = "JobCardViewModel"

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _submitSuccess = MutableStateFlow(false)
    val submitSuccess: StateFlow<Boolean> = _submitSuccess.asStateFlow()

    suspend fun getJobById(jobId: Int): Job? =
        database.jobDao().getJobById(jobId)

    suspend fun getJobCardByJobId(jobId: Int): JobCard? =
        database.jobCardDao().getJobCardByJobId(jobId)

    fun getJobCardsByTechnician(technicianId: String): Flow<List<JobCard>> =
        database.jobCardDao().getJobCardsByTechnician(technicianId)

    suspend fun saveJobCard(
        jobId: Int,
        technicianId: String,
        buildingName: String,
        workSummary: String,
        partsUsed: List<PartItem>,
        startTime: Long?,
        endTime: Long?,
        additionalNotes: String,
        photoPaths: List<String>
    ): Long {
        return try {
            val existing = database.jobCardDao().getJobCardByJobId(jobId)
            if (existing != null) {
                val updated = existing.copy(
                    workSummary = workSummary,
                    partsUsed = partsUsed,
                    startTime = startTime,
                    endTime = endTime,
                    additionalNotes = additionalNotes,
                    photoPaths = photoPaths,
                    status = JobCardStatus.DRAFT
                )
                database.jobCardDao().updateJobCard(updated)
                existing.id.toLong()
            } else {
                val jobCard = JobCard(
                    jobId = jobId,
                    technicianId = technicianId,
                    buildingName = buildingName,
                    workSummary = workSummary,
                    partsUsed = partsUsed,
                    startTime = startTime,
                    endTime = endTime,
                    additionalNotes = additionalNotes,
                    photoPaths = photoPaths,
                    status = JobCardStatus.DRAFT
                )
                database.jobCardDao().insertJobCard(jobCard)
            }
        } catch (e: Exception) {
            Log.e(TAG, "saveJobCard failed", e)
            0L
        }
    }

    /**
     * Full submit flow.
     *
     * Runs in the caller's scope so the screen can await it before
     * navigating back (viewModelScope would be cancelled otherwise).
     *
     * Order of operations:
     *   1. Mark the job card SUBMITTED (local).
     *   2. Mark the job COMPLETED (local).
     *   3. Stop the local foreground tracking service.
     *   4. Write a COMPLETED marker to Firestore — this is what the
     *      manager's device reads to render the green pin.
     *   5. Push COMPLETED to the REST API (status, on-way off, stopTracking).
     *   6. Prime the local sticky cache (so the tech's own device
     *      shows green if they ever open the tracking screen).
     */
    suspend fun submitJobCardAndWait(jobCardId: Long) {
        _isLoading.value = true
        try {
            Log.d(TAG, "submitJobCardAndWait: start for cardId=$jobCardId")

            // 1. Mark the job card as SUBMITTED
            database.jobCardDao().updateJobCardStatus(
                jobCardId.toInt(), JobCardStatus.SUBMITTED
            )
            Log.d(TAG, "submitJobCardAndWait: card marked SUBMITTED")

            val jc = database.jobCardDao().getJobCardById(jobCardId.toInt())
            if (jc == null) {
                Log.w(TAG, "submitJobCardAndWait: card $jobCardId not found")
                _submitSuccess.value = false
                return
            }

            // 2. Mark the job as COMPLETED locally
            database.jobDao().updateJobStatus(jc.jobId, JobStatus.COMPLETED)
            Log.d(TAG, "submitJobCardAndWait: job ${jc.jobId} marked COMPLETED")

            val job = database.jobDao().getJobById(jc.jobId)
            if (job == null) {
                Log.w(TAG, "submitJobCardAndWait: job ${jc.jobId} not found")
                _submitSuccess.value = true
                return
            }
            Log.d(
                TAG,
                "submitJobCardAndWait: job loaded, tech=${job.technicianId}, " +
                        "serverId=${job.serverId}"
            )

            // 3. Stop the LOCAL foreground tracking service FIRST.
            try {
                TechLocationService.stop(appContext)
                Log.d(TAG, "submitJobCardAndWait: TechLocationService.stop() called")
            } catch (e: Exception) {
                Log.w(TAG, "submitJobCardAndWait: stop service failed", e)
            }

            // 4. Firestore: write an explicit COMPLETED marker.
            //    Using markCompleted() writes a clean payload with
            //    status=COMPLETED and isOnMyWay=false, which the
            //    manager's device can reliably read.
            try {
                val ok = LocationTrackingManager.markCompleted(
                    technicianId = job.technicianId,
                    technicianName = "Technician",
                    jobId = job.id,
                    customerId = job.customerId,
                    buildingName = job.buildingName
                )
                Log.d(TAG, "submitJobCardAndWait: firestore markCompleted=$ok")
            } catch (e: Exception) {
                Log.w(TAG, "submitJobCardAndWait: firestore markCompleted threw", e)
            }

            // 5. REST: push COMPLETED, clear on-way, delete tracking row
            if (NetworkMonitor.isOnline(appContext) && ApiClient.hasToken(appContext)) {
                job.serverId?.let { sid ->
                    try {
                        ApiRepository.updateJobStatus(appContext, sid, "COMPLETED")
                        Log.d(TAG, "submitJobCardAndWait: REST updateJobStatus ok")
                    } catch (e: Exception) {
                        Log.w(TAG, "submitJobCardAndWait: REST updateJobStatus failed", e)
                    }
                    try {
                        ApiRepository.setJobOnWay(appContext, sid, false)
                        Log.d(TAG, "submitJobCardAndWait: REST setJobOnWay ok")
                    } catch (e: Exception) {
                        Log.w(TAG, "submitJobCardAndWait: REST setJobOnWay failed", e)
                    }
                }

                try {
                    ApiRepository.stopTracking(appContext, job.technicianId)
                    Log.d(TAG, "submitJobCardAndWait: REST stopTracking ok")
                } catch (e: Exception) {
                    Log.w(TAG, "submitJobCardAndWait: REST stopTracking failed", e)
                }
            } else {
                Log.w(TAG, "submitJobCardAndWait: offline or no token — REST skipped")
            }

            // 6. Prime the local sticky cache for this device.
            try {
                ManagerTrackingViewModel.cacheCompletedTechnician(
                    appContext,
                    TechLocationDto(
                        technicianId = job.technicianId,
                        technicianName = "Technician",
                        latitude = 0.0,
                        longitude = 0.0,
                        jobId = job.id,
                        customerId = job.customerId,
                        buildingName = job.buildingName,
                        isOnMyWay = false,
                        lastUpdated = System.currentTimeMillis(),
                        status = "COMPLETED",
                        destinationLatitude = 0.0,
                        destinationLongitude = 0.0
                    )
                )
                Log.d(TAG, "submitJobCardAndWait: local cache primed")
            } catch (e: Exception) {
                Log.w(TAG, "submitJobCardAndWait: local cache prime failed", e)
            }

            _submitSuccess.value = true
            Log.d(TAG, "submitJobCardAndWait: done")
        } catch (e: Exception) {
            Log.e(TAG, "submitJobCardAndWait failed", e)
            _submitSuccess.value = false
        } finally {
            _isLoading.value = false
        }
    }

    /**
     * Legacy fire-and-forget submit. Do NOT call from the UI.
     */
    suspend fun submitJobCard(jobCardId: Long) {
        viewModelScope.launch {
            submitJobCardAndWait(jobCardId)
        }
    }

    fun resetSubmitState() { _submitSuccess.value = false }

    companion object {
        fun Factory(
            database: ArcticFlowDatabase,
            context: Context
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(JobCardViewModel::class.java)) {
                        return JobCardViewModel(
                            database,
                            context.applicationContext
                        ) as T
                    }
                    throw IllegalArgumentException("Unknown ViewModel class")
                }
            }
    }
}