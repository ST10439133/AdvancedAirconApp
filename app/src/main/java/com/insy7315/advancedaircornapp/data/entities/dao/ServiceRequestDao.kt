// app/src/main/java/com/insy7315/advancedaircornapp/data/dao/ServiceRequestDao.kt
package com.insy7315.advancedaircornapp.data.dao

import androidx.room.*
import com.insy7315.advancedaircornapp.data.entities.ServiceRequest
import com.insy7315.advancedaircornapp.data.entities.RequestStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface ServiceRequestDao {

    // INSERT
    @Insert
    suspend fun insertRequest(request: ServiceRequest): Long

    // UPDATE
    // Full update used by Manager's Edit dialog
    @Update
    suspend fun updateRequestFull(request: ServiceRequest)

    // DELETE
    // Used by Manager to delete a request
    @Delete
    suspend fun deleteRequest(request: ServiceRequest)

    //  QUERIES
    // Manager's own requests (only theirs)
    @Query("SELECT * FROM service_requests WHERE userId = :userId ORDER BY createdAt DESC")
    fun getRequestsByUser(userId: String): Flow<List<ServiceRequest>>

    // Requests tied to a specific building
    @Query("SELECT * FROM service_requests WHERE buildingId = :buildingId ORDER BY createdAt DESC")
    fun getRequestsByBuilding(buildingId: Int): Flow<List<ServiceRequest>>

    // Single request lookup
    @Query("SELECT * FROM service_requests WHERE id = :requestId")
    suspend fun getRequestById(requestId: Int): ServiceRequest?

    // Get ALL pending requests
    @Query("SELECT * FROM service_requests WHERE status = :status ORDER BY createdAt DESC")
    fun getPendingRequests(status: RequestStatus = RequestStatus.PENDING): Flow<List<ServiceRequest>>

    // Filter by any status
    @Query("SELECT * FROM service_requests WHERE status = :status ORDER BY createdAt DESC")
    fun getRequestsByStatus(status: RequestStatus): Flow<List<ServiceRequest>>

    // Quick status update
    @Query("UPDATE service_requests SET status = :status WHERE id = :requestId")
    suspend fun updateRequestStatus(requestId: Int, status: RequestStatus)
}