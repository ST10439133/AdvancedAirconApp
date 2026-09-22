package com.insy7315.advancedairconapp.data.api

import retrofit2.http.*

// Retrofit interface describing every REST endpoint exposed by
// the ArcticFlow Node.js API. The Authorization header is
// injected globally by ApiClient's OkHttp interceptor, so no
// @Header("Authorization") parameters are needed here.
interface ArcticFlowApi {

    // ---- Users ----
    @POST("api/users/sync")
    suspend fun syncUser(@Body body: UserSyncRequest): UserSyncResponse

    @GET("api/users/me")
    suspend fun getMe(): UserDto

    // Buildings
    @GET("api/buildings")
    suspend fun getBuildings(): List<BuildingDto>

    @POST("api/buildings")
    suspend fun createBuilding(
        @Body body: Map<String, @JvmSuppressWildcards Any?>
    ): BuildingDto

    @DELETE("api/buildings/{id}")
    suspend fun deleteBuilding(@Path("id") id: Int)

    // Service Requests
    @GET("api/requests")
    suspend fun getMyRequests(): List<ServiceRequestDto>

    @GET("api/requests/pending")
    suspend fun getPendingRequests(): List<ServiceRequestDto>

    @GET("api/requests/{id}")
    suspend fun getRequest(@Path("id") id: Int): ServiceRequestDto

    @POST("api/requests")
    suspend fun createRequest(
        @Body body: Map<String, @JvmSuppressWildcards Any?>
    ): ServiceRequestDto

    @PATCH("api/requests/{id}/status")
    suspend fun updateRequestStatus(
        @Path("id") id: Int,
        @Body body: StatusUpdate
    )

    // Quotes
    @GET("api/quotes/customer")
    suspend fun getCustomerQuotes(): List<QuoteDto>

    @GET("api/quotes/technician")
    suspend fun getTechnicianQuotes(): List<QuoteDto>

    @POST("api/quotes")
    suspend fun createQuote(
        @Body body: Map<String, @JvmSuppressWildcards Any?>
    ): QuoteDto

    @PATCH("api/quotes/{id}/status")
    suspend fun updateQuoteStatus(
        @Path("id") id: Int,
        @Body body: StatusUpdate
    )

    // Jobs
    @GET("api/jobs/customer")
    suspend fun getCustomerJobs(): List<JobDto>

    @GET("api/jobs/technician")
    suspend fun getTechnicianJobs(): List<JobDto>

    @POST("api/jobs")
    suspend fun createJob(
        @Body body: Map<String, @JvmSuppressWildcards Any?>
    ): JobDto

    @PATCH("api/jobs/{id}/status")
    suspend fun updateJobStatus(
        @Path("id") id: Int,
        @Body body: StatusUpdate
    )

    @PATCH("api/jobs/{id}/on-way")
    suspend fun setOnWay(
        @Path("id") id: Int,
        @Body body: OnWayUpdate
    )

    // Live Locations
    @GET("api/locations")
    suspend fun getActiveLocations(
        @Query("customerId") customerId: String? = null
    ): List<TechLocationDto>

    @PUT("api/locations/{technicianId}")
    suspend fun updateLocationRaw(
        @Path("technicianId") technicianId: String,
        @Body body: Map<String, @JvmSuppressWildcards Any?>
    )



    @DELETE("api/locations/{technicianId}")
    suspend fun stopTracking(@Path("technicianId") technicianId: String)
}