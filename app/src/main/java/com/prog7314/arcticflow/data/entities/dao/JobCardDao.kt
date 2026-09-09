// app/src/main/java/com/prog7314/arcticflow/data/dao/JobCardDao.kt
package com.prog7314.arcticflow.data.dao

import androidx.room.*
import com.prog7314.arcticflow.data.entities.JobCard
import com.prog7314.arcticflow.data.entities.JobCardStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface JobCardDao {

    @Insert
    suspend fun insertJobCard(jobCard: JobCard): Long

    @Update
    suspend fun updateJobCard(jobCard: JobCard)

    @Delete
    suspend fun deleteJobCard(jobCard: JobCard)

    @Query("SELECT * FROM job_cards WHERE technicianId = :technicianId ORDER BY createdAt DESC")
    fun getJobCardsByTechnician(technicianId: String): Flow<List<JobCard>>

    @Query("SELECT * FROM job_cards WHERE jobId = :jobId")
    suspend fun getJobCardByJobId(jobId: Int): JobCard?

    @Query("SELECT * FROM job_cards WHERE id = :jobCardId")
    suspend fun getJobCardById(jobCardId: Int): JobCard?

    @Query("SELECT * FROM job_cards WHERE status = :status ORDER BY createdAt DESC")
    fun getJobCardsByStatus(status: JobCardStatus): Flow<List<JobCard>>

    @Query("UPDATE job_cards SET status = :status WHERE id = :jobCardId")
    suspend fun updateJobCardStatus(jobCardId: Int, status: JobCardStatus)

    @Query("SELECT * FROM job_cards WHERE technicianId = :technicianId AND status = :status")
    fun getJobCardsByTechnicianAndStatus(technicianId: String, status: JobCardStatus): Flow<List<JobCard>>
}