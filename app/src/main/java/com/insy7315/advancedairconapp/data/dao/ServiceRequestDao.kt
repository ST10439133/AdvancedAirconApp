package com.insy7315.advancedairconapp.data.dao

import androidx.room.*
import com.insy7315.advancedairconapp.data.entities.RequestStatus
import com.insy7315.advancedairconapp.data.entities.ServiceRequest
import kotlinx.coroutines.flow.Flow

@Dao
interface ServiceRequestDao {

    @Insert
    suspend fun insertRequest(request: ServiceRequest): Long

    @Update
    suspend fun updateRequestFull(request: ServiceRequest)

    @Delete
    suspend fun deleteRequest(request: ServiceRequest)

    @Query("SELECT * FROM service_requests WHERE userId = :userId ORDER BY createdAt DESC")
    fun getRequestsByUser(userId: String): Flow<List<ServiceRequest>>

    @Query("SELECT * FROM service_requests WHERE buildingId = :buildingId ORDER BY createdAt DESC")
    fun getRequestsByBuilding(buildingId: Int): Flow<List<ServiceRequest>>

    @Query("SELECT * FROM service_requests WHERE id = :requestId")
    suspend fun getRequestById(requestId: Int): ServiceRequest?

    @Query("SELECT * FROM service_requests WHERE serverId = :serverId LIMIT 1")
    suspend fun getRequestByServerId(serverId: Int): ServiceRequest?

    // Pending queue — technicians only
    @Query("SELECT * FROM service_requests WHERE status = 'PENDING' ORDER BY createdAt DESC")
    fun getPendingRequests(): Flow<List<ServiceRequest>>

    @Query("SELECT * FROM service_requests WHERE status = :status ORDER BY createdAt DESC")
    fun getRequestsByStatus(status: RequestStatus): Flow<List<ServiceRequest>>

    // NEW — declined-by-technician queue, seen by the manager
    @Query("SELECT * FROM service_requests WHERE status = 'DECLINED_BY_TECH' ORDER BY declinedAt DESC")
    fun getDeclinedByTechnicianRequests(): Flow<List<ServiceRequest>>

    @Query("""
        SELECT * FROM service_requests
        WHERE userId = :userId AND status = 'DECLINED_BY_TECH'
        ORDER BY declinedAt DESC
    """)
    fun getDeclinedByTechnicianRequestsForManager(userId: String): Flow<List<ServiceRequest>>

    @Query("UPDATE service_requests SET status = :status, updatedAt = :updatedAt WHERE id = :requestId")
    suspend fun updateRequestStatus(
        requestId: Int,
        status: RequestStatus,
        updatedAt: Long = System.currentTimeMillis()
    )

    // NEW — decline with reason
    @Query("""
        UPDATE service_requests
        SET status = 'DECLINED_BY_TECH',
            declineReason = :reason,
            declinedBy = 'TECHNICIAN',
            declinedAt = :declinedAt,
            updatedAt = :declinedAt
        WHERE id = :requestId
    """)
    suspend fun declineByTechnician(
        requestId: Int,
        reason: String,
        declinedAt: Long = System.currentTimeMillis()
    )

    // NEW — reopen a declined request
    @Query("""
        UPDATE service_requests
        SET status = 'PENDING',
            declineReason = NULL,
            declinedBy = NULL,
            declinedAt = NULL,
            updatedAt = :updatedAt
        WHERE id = :requestId
    """)
    suspend fun reopenRequest(
        requestId: Int,
        updatedAt: Long = System.currentTimeMillis()
    )

    @Query("UPDATE service_requests SET serverId = :serverId WHERE id = :localId")
    suspend fun setServerId(localId: Int, serverId: Int)
}