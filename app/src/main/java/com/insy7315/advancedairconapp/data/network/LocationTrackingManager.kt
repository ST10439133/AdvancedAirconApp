package com.insy7315.advancedairconapp.data.network

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.insy7315.advancedairconapp.data.entities.TechLocation
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

object LocationTrackingManager {

    private const val TAG = "LocationTracking"
    private const val COLLECTION = "tech_locations"

    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }


    // Technician side

    suspend fun updateLocation(location: TechLocation): Boolean {
        if (location.technicianId.isBlank()) {
            Log.e(TAG, "Refusing to write location with blank technicianId")
            return false
        }
        return try {
            firestore.collection(COLLECTION)
                .document(location.technicianId)
                .set(location, SetOptions.merge())
                .await()
            Log.d(TAG, "UPDATED → doc=${location.technicianId} " +
                    "lat=${location.latitude}, lng=${location.longitude}, " +
                    "jobId=${location.jobId}, customerId=${location.customerId}, " +
                    "onMyWay=${location.onMyWay}, status=${location.status}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update location", e)
            false
        }
    }

    /**
     * Writes a COMPLETED marker to Firestore. This is the channel the
     * manager's device uses to learn that a technician finished a job,
     * because the REST backend may filter COMPLETED rows out of
     * GET /api/locations.
     */
    suspend fun markCompleted(
        technicianId: String,
        technicianName: String?,
        jobId: Int?,
        customerId: String?,
        buildingName: String?
    ): Boolean {
        if (technicianId.isBlank()) return false
        return try {
            val payload = mapOf(
                "technicianId" to technicianId,
                "technicianName" to (technicianName ?: "Technician"),
                "latitude" to 0.0,
                "longitude" to 0.0,
                "jobId" to (jobId ?: 0),
                "customerId" to (customerId ?: ""),
                "buildingName" to (buildingName ?: ""),
                "destinationLatitude" to 0.0,
                "destinationLongitude" to 0.0,
                "isOnMyWay" to false,
                "onMyWay" to false,
                "lastUpdated" to System.currentTimeMillis(),
                "status" to "COMPLETED",
                "completedAt" to System.currentTimeMillis()
            )
            firestore.collection(COLLECTION)
                .document(technicianId)
                .set(payload, SetOptions.merge())
                .await()
            Log.d(TAG, "MARKED COMPLETED → doc=$technicianId jobId=$jobId")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to mark completed", e)
            false
        }
    }

    suspend fun stopTracking(technicianId: String): Boolean {
        if (technicianId.isBlank()) return false
        return try {
            firestore.collection(COLLECTION)
                .document(technicianId)
                .set(
                    mapOf(
                        "isOnMyWay" to false,
                        "status" to "idle",
                        "lastUpdated" to System.currentTimeMillis()
                    ),
                    SetOptions.merge()
                )
                .await()
            Log.d(TAG, "STOPPED tracking for $technicianId")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop tracking", e)
            false
        }
    }

    suspend fun clearLocation(technicianId: String): Boolean {
        if (technicianId.isBlank()) return false
        return try {
            firestore.collection(COLLECTION)
                .document(technicianId)
                .delete()
                .await()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear location", e)
            false
        }
    }


    // READ — Customer side

    fun streamActiveLocations(
        customerId: String? = null,
        technicianIds: Set<String>? = null,
        jobIds: Set<Int>? = null
    ): Flow<List<TechLocation>> = callbackFlow {

        Log.d(TAG, "Starting listener → customerId=$customerId, " +
                "techs=$technicianIds, jobs=$jobIds")

        val listener = firestore.collection(COLLECTION)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Snapshot listener error", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                if (snapshot == null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                Log.d(TAG, "Firestore emitted ${snapshot.documents.size} total docs")

                val all = snapshot.documents.mapNotNull { doc ->
                    try {
                        val loc = doc.toObject(TechLocation::class.java)
                        Log.d(TAG, "  doc=${doc.id} → techId=${loc?.technicianId} " +
                                "jobId=${loc?.jobId} onMyWay=${loc?.onMyWay} " +
                                "custId=${loc?.customerId} status=${loc?.status}")
                        loc
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to parse doc ${doc.id}", e)
                        null
                    }
                }

                val active = all.filter { it.onMyWay }

                val filtered = active.filter { loc ->
                    val matchesCustomer =
                        customerId == null || loc.customerId == customerId
                    val matchesTech =
                        technicianIds == null || loc.technicianId in technicianIds
                    val matchesJob =
                        jobIds == null || loc.jobId in jobIds

                    val keep = matchesCustomer && matchesTech && matchesJob
                    Log.d(TAG, "  filter tech=${loc.technicianId}: " +
                            "cust=$matchesCustomer tech=$matchesTech job=$matchesJob → $keep")
                    keep
                }

                Log.d(TAG, "Emitting ${filtered.size} active locations")
                trySend(filtered)
            }

        awaitClose {
            Log.d(TAG, "Removing listener")
            listener.remove()
        }
    }

    suspend fun fetchCompleted(): List<TechLocation> {
        return try {
            val snapshot = firestore.collection(COLLECTION)
                .get()
                .await()
            val list = snapshot.documents.mapNotNull { doc ->
                try {
                    val loc = doc.toObject(TechLocation::class.java)
                    if (loc != null && loc.status.equals("COMPLETED", ignoreCase = true)) {
                        loc
                    } else null
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to parse completed doc ${doc.id}", e)
                    null
                }
            }
            Log.d(TAG, "fetchCompleted: ${list.size} completed docs")
            list
        } catch (e: Exception) {
            Log.w(TAG, "fetchCompleted failed", e)
            emptyList()
        }
    }

    suspend fun getLocationOnce(technicianId: String): TechLocation? {
        return try {
            val doc = firestore.collection(COLLECTION)
                .document(technicianId)
                .get()
                .await()
            doc.toObject(TechLocation::class.java)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch location once", e)
            null
        }
    }
}