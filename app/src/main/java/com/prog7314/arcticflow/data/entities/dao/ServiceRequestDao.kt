// app/src/main/java/com/prog7314/arcticflow/data/dao/ServiceRequestDao.kt
package com.prog7314.arcticflow.data.dao

import androidx.room.*
import com.prog7314.arcticflow.data.entities.ServiceRequest
import com.prog7314.arcticflow.data.entities.RequestStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface ServiceRequestDao {
    @Insert
    suspend fun insertRequest(request: ServiceRequest): Long

    @Update
    suspend fun updateRequest(request: ServiceRequest)

    @Delete
    suspend fun deleteRequest(request: ServiceRequest)

    @Query("SELECT * FROM service_requests WHERE userId = :userId ORDER BY createdAt DESC")
    fun getRequestsByUser(userId: String): Flow<List<ServiceRequest>>

    @Query("SELECT * FROM service_requests WHERE buildingId = :buildingId ORDER BY createdAt DESC")
    fun getRequestsByBuilding(buildingId: Int): Flow<List<ServiceRequest>>

    @Query("SELECT * FROM service_requests WHERE id = :requestId")
    suspend fun getRequestById(requestId: Int): ServiceRequest?

    // CRITICAL: Get ALL pending requests - NO userId filter
    @Query("SELECT * FROM service_requests WHERE status = 'PENDING' ORDER BY createdAt DESC")
    fun getPendingRequests(): Flow<List<ServiceRequest>>

    @Query("SELECT * FROM service_requests WHERE status = :status ORDER BY createdAt DESC")
    fun getRequestsByStatus(status: RequestStatus): Flow<List<ServiceRequest>>

    @Query("UPDATE service_requests SET status = :status WHERE id = :requestId")
    suspend fun updateRequestStatus(requestId: Int, status: RequestStatus)
}