// app/src/main/java/com/prog7314/arcticflow/viewmodels/JobCardViewModel.kt
package com.prog7314.arcticflow.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.prog7314.arcticflow.data.ArcticFlowDatabase
import com.prog7314.arcticflow.data.entities.Job
import com.prog7314.arcticflow.data.entities.JobCard
import com.prog7314.arcticflow.data.entities.JobCardStatus
import com.prog7314.arcticflow.data.entities.JobStatus
import com.prog7314.arcticflow.data.entities.PartItem
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

    suspend fun getJobById(jobId: Int): Job? {
        return database.jobDao().getJobById(jobId)
    }

    suspend fun getJobCardByJobId(jobId: Int): JobCard? {
        return database.jobCardDao().getJobCardByJobId(jobId)
    }

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
    ): Long {  // Changed to return Long
        return try {
            // Check if job card already exists for this job
            val existing = database.jobCardDao().getJobCardByJobId(jobId)

            if (existing != null) {
                // Update existing job card
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
                existing.id.toLong()  // Return existing ID as Long
            } else {
                // Create new job card
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
                database.jobCardDao().insertJobCard(jobCard)  // This returns Long
            }
        } catch (e: Exception) {
            e.printStackTrace()
            0L
        }
    }

    suspend fun submitJobCard(jobCardId: Long) {  // Changed to Long
        viewModelScope.launch {
            _isLoading.value = true
            try {
                database.jobCardDao().updateJobCardStatus(jobCardId.toInt(), JobCardStatus.SUBMITTED)

                // Also update the job status to COMPLETED
                val jobCard = database.jobCardDao().getJobCardById(jobCardId.toInt())
                jobCard?.let {
                    database.jobDao().updateJobStatus(jobCard.jobId, JobStatus.COMPLETED)
                }
                _submitSuccess.value = true
            } catch (e: Exception) {
                _submitSuccess.value = false
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun resetSubmitState() {
        _submitSuccess.value = false
    }

    companion object {
        fun Factory(database: ArcticFlowDatabase): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
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
}