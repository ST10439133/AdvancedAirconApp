// app/src/main/java/com/prog7314/arcticflow/data/network/LocationTrackingManager.kt
package com.prog7314.arcticflow.data.network

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.prog7314.arcticflow.data.entities.TechLocation
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Handles reading and writing technician locations to Firestore.
 *
 * Firestore structure:
 *   tech_locations/{technicianId}
 *     - technicianId: String
 *     - technicianName: String
 *     - latitude: Double
 *     - longitude: Double
 *     - jobId: Int
 *     - buildingName: String
 *     - isOnMyWay: Boolean
 *     - lastUpdated: Long
 *     - status: String
 */
object LocationTrackingManager {

    private const val TAG = "LocationTracking"
    private const val COLLECTION = "tech_locations"

    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    // ============================================================
    // WRITE — Used by the Technician side
    // ============================================================

    /**
     * Sends a location update to Firestore.
     * Called every 15-30 seconds while the technician is on the way.
     */
    suspend fun updateLocation(location: TechLocation): Boolean {
        return try {
            firestore.collection(COLLECTION)
                .document(location.technicianId)
                .set(location)
                .await()
            Log.d(TAG, "Updated location for ${location.technicianId} " +
                    "(${location.latitude}, ${location.longitude})")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update location", e)
            false
        }
    }

    /**
     * Marks the technician as no longer tracking.
     * Called when "On My Way" is toggled off or the job is completed.
     */
    suspend fun stopTracking(technicianId: String): Boolean {
        return try {
            firestore.collection(COLLECTION)
                .document(technicianId)
                .set(
                    mapOf(
                        "isOnMyWay" to false,
                        "status" to "idle",
                        "lastUpdated" to System.currentTimeMillis()
                    ),
                    com.google.firebase.firestore.SetOptions.merge()   // creates if missing
                )
                .await()
            Log.d(TAG, "Stopped tracking for $technicianId")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop tracking", e)
            false
        }
    }

    /**
     * Fully removes the technician's location doc.
     * Useful for cleanup when they sign out.
     */
    suspend fun clearLocation(technicianId: String): Boolean {
        return try {
            firestore.collection(COLLECTION)
                .document(technicianId)
                .delete()
                .await()
            Log.d(TAG, "Cleared location for $technicianId")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear location", e)
            false
        }
    }

    // ============================================================
    // READ — Used by the Manager side
    // ============================================================

    /**
     * Streams live locations of all technicians currently "on the way".
     * Any change on Firestore is pushed to this Flow in real time.
     *
     * @param technicianIds  Only track these specific technician IDs.
     *                       Pass `null` to track every active technician.
     */
    fun streamActiveLocations(technicianIds: Set<String>? = null): Flow<List<TechLocation>> =
        callbackFlow {
            val query = firestore.collection(COLLECTION)
                .whereEqualTo("isOnMyWay", true)

            val listener = query.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Snapshot listener error", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val locations = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(TechLocation::class.java)
                }?.filter { location ->
                    // If we have a whitelist, apply it
                    technicianIds == null || location.technicianId in technicianIds
                } ?: emptyList()

                Log.d(TAG, "Streamed ${locations.size} active locations")
                trySend(locations)
            }

            awaitClose { listener.remove() }
        }

    /**
     * One-shot fetch of a single technician's last known location.
     * Useful for showing a marker when the stream is not yet connected.
     */
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