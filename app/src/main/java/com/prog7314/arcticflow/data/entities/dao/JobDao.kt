package com.prog7314.arcticflow.data.dao

import androidx.room.*
import com.prog7314.arcticflow.data.entities.Job
import com.prog7314.arcticflow.data.entities.JobStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface JobDao {
    @Insert
    suspend fun insertJob(job: Job): Long

    @Update
    suspend fun updateJob(job: Job)

    @Delete
    suspend fun deleteJob(job: Job)

    @Query("SELECT * FROM jobs WHERE customerId = :customerId ORDER BY createdAt DESC")
    fun getJobsByCustomer(customerId: String): Flow<List<Job>>

    @Query("SELECT * FROM jobs WHERE technicianId = :technicianId ORDER BY scheduledDate ASC")
    fun getJobsByTechnician(technicianId: String): Flow<List<Job>>

    @Query("SELECT * FROM jobs WHERE status = :status ORDER BY scheduledDate ASC")
    fun getJobsByStatus(status: JobStatus): Flow<List<Job>>

    @Query("SELECT * FROM jobs WHERE id = :jobId")
    suspend fun getJobById(jobId: Int): Job?

    @Query("UPDATE jobs SET status = :status WHERE id = :jobId")
    suspend fun updateJobStatus(jobId: Int, status: JobStatus)

    @Query("UPDATE jobs SET scheduledDate = :date, notes = :notes WHERE id = :jobId")
    suspend fun updateJobSchedule(jobId: Int, date: Long, notes: String?)

    @Query("UPDATE jobs SET technicianOnWay = :onWay WHERE id = :jobId")
    suspend fun updateTechnicianOnWay(jobId: Int, onWay: Boolean)
}