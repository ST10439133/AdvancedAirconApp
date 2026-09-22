package com.insy7315.advancedairconapp.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.insy7315.advancedairconapp.data.ArcticFlowDatabase
import com.insy7315.advancedairconapp.data.entities.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class JobCardViewModel(
    private val database: ArcticFlowDatabase
) : ViewModel() {

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
            e.printStackTrace()
            0L
        }
    }

    suspend fun submitJobCard(jobCardId: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                database.jobCardDao().updateJobCardStatus(
                    jobCardId.toInt(), JobCardStatus.SUBMITTED
                )
                val jc = database.jobCardDao().getJobCardById(jobCardId.toInt())
                jc?.let {
                    database.jobDao().updateJobStatus(it.jobId, JobStatus.COMPLETED)
                }
                _submitSuccess.value = true
            } catch (e: Exception) {
                _submitSuccess.value = false
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun resetSubmitState() { _submitSuccess.value = false }

    companion object {
        fun Factory(database: ArcticFlowDatabase): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(JobCardViewModel::class.java)) {
                        return JobCardViewModel(database) as T
                    }
                    throw IllegalArgumentException("Unknown ViewModel class")
                }
            }
    }
}