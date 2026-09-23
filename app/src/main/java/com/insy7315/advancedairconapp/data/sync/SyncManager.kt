package com.insy7315.advancedairconapp.data.sync

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.insy7315.advancedairconapp.data.ArcticFlowDatabase
import com.insy7315.advancedairconapp.data.api.ApiRepository
import com.insy7315.advancedairconapp.data.entities.JobStatus
import com.insy7315.advancedairconapp.data.entities.QuoteStatus
import com.insy7315.advancedairconapp.data.entities.RequestStatus
import com.insy7315.advancedairconapp.data.entities.SyncQueueEntity
import com.insy7315.advancedairconapp.data.network.NetworkMonitor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

object SyncManager {

    private const val TAG = "SyncManager"

    const val TYPE_REQUEST_CREATE = "REQUEST_CREATE"
    const val TYPE_REQUEST_STATUS = "REQUEST_STATUS"
    const val TYPE_QUOTE_CREATE   = "QUOTE_CREATE"
    const val TYPE_QUOTE_STATUS   = "QUOTE_STATUS"
    const val TYPE_JOB_CREATE     = "JOB_CREATE"
    const val TYPE_JOB_STATUS     = "JOB_STATUS"
    const val TYPE_JOB_ON_WAY     = "JOB_ON_WAY"
    const val TYPE_BUILDING_CREATE = "BUILDING_CREATE"

    suspend fun enqueue(
        context: Context,
        type: String,
        localId: Int,
        payload: Map<String, Any?>
    ): Long = withContext(Dispatchers.IO) {
        val db = ArcticFlowDatabase.getDatabase(context)
        val json = Gson().toJson(payload)
        val id = db.syncQueueDao().enqueue(
            SyncQueueEntity(type = type, localId = localId, payloadJson = json)
        )
        Log.d(TAG, "enqueued $type localId=$localId (queueId=$id)")
        id
    }

    suspend fun drain(context: Context): Int = withContext(Dispatchers.IO) {
        if (!NetworkMonitor.isOnline(context)) {
            Log.d(TAG, "drain skipped: offline")
            return@withContext 0
        }

        val db = ArcticFlowDatabase.getDatabase(context)
        val items = db.syncQueueDao().getAllOnce()
        if (items.isEmpty()) return@withContext 0

        Log.d(TAG, "draining ${items.size} queued item(s)")
        var synced = 0

        for (item in items) {
            val ok = try {
                replay(context, item)
            } catch (e: Exception) {
                Log.e(TAG, "replay failed for ${item.type} #${item.localId}", e)
                false
            }
            if (ok) {
                db.syncQueueDao().deleteById(item.id)
                synced++
            } else {
                db.syncQueueDao().markAttempt(item.id, "replay returned false")
            }
        }

        Log.d(TAG, "drain complete: $synced/${items.size} synced")
        synced
    }

    /**
     * Replays a queued item against the API.
     *
     * Returns TRUE if the item should be removed from the queue, FALSE if
     * it should be retried later.
     *
     * Key rule: if the underlying local entity no longer exists (deleted,
     * or never inserted), we DROP the queue item — return true. Otherwise
     * the queue jams forever on items that can never succeed.
     */
    private suspend fun replay(context: Context, item: SyncQueueEntity): Boolean {
        val json = JSONObject(item.payloadJson)
        val db = ArcticFlowDatabase.getDatabase(context)

        return when (item.type) {

            TYPE_BUILDING_CREATE -> {
                val building = db.buildingDao().getBuildingById(item.localId)
                if (building == null) {
                    Log.w(TAG, "drop BUILDING_CREATE: local row ${item.localId} gone")
                    return true
                }
                if (building.serverId != null) return true
                val dto = ApiRepository.pushBuilding(context, building)
                if (dto != null) {
                    db.buildingDao().setServerId(building.id, dto.id)
                    true
                } else false
            }

            TYPE_REQUEST_CREATE -> {
                val request = db.serviceRequestDao().getRequestById(item.localId)
                if (request == null) {
                    Log.w(TAG, "drop REQUEST_CREATE: local row ${item.localId} gone")
                    return true
                }
                if (request.serverId != null) return true

                val building = db.buildingDao().getBuildingById(request.buildingId)
                val dto = ApiRepository.pushServiceRequest(
                    context = context,
                    request = request,
                    buildingName = building?.name,
                    fullAddress = building?.fullAddress,
                    serverBuildingId = building?.serverId
                )
                if (dto != null) {
                    db.serviceRequestDao().setServerId(request.id, dto.id)
                    true
                } else false
            }

            TYPE_REQUEST_STATUS -> {
                val request = db.serviceRequestDao().getRequestById(item.localId)
                if (request == null) {
                    Log.w(TAG, "drop REQUEST_STATUS: local row ${item.localId} gone")
                    return true
                }
                val serverId = request.serverId
                if (serverId == null) {
                    Log.w(TAG, "drop REQUEST_STATUS: local row ${item.localId} has no serverId")
                    return true
                }
                ApiRepository.updateRequestStatus(
                    context, serverId,
                    json.optString("status", RequestStatus.PENDING.name)
                )
            }

            TYPE_QUOTE_CREATE -> {
                val quote = db.quoteDao().getQuoteById(item.localId)
                if (quote == null) {
                    Log.w(TAG, "drop QUOTE_CREATE: local row ${item.localId} gone")
                    return true
                }
                if (quote.serverId != null) return true
                val request = db.serviceRequestDao().getRequestById(quote.requestId)
                val dto = ApiRepository.pushQuote(
                    context = context,
                    quote = quote,
                    serverRequestId = request?.serverId
                )
                if (dto != null) {
                    db.quoteDao().setServerId(quote.id, dto.id)
                    true
                } else false
            }

            TYPE_QUOTE_STATUS -> {
                val quote = db.quoteDao().getQuoteById(item.localId)
                if (quote == null) {
                    Log.w(TAG, "drop QUOTE_STATUS: local row ${item.localId} gone")
                    return true
                }
                val serverId = quote.serverId
                if (serverId == null) {
                    Log.w(TAG, "drop QUOTE_STATUS: local row ${item.localId} has no serverId")
                    return true
                }
                ApiRepository.updateQuoteStatus(
                    context, serverId,
                    json.optString("status", QuoteStatus.PENDING.name)
                )
            }

            TYPE_JOB_CREATE -> {
                val job = db.jobDao().getJobById(item.localId)
                if (job == null) {
                    Log.w(TAG, "drop JOB_CREATE: local row ${item.localId} gone")
                    return true
                }
                if (job.serverId != null) return true
                val quote = db.quoteDao().getQuoteById(job.quoteId)
                val request = db.serviceRequestDao().getRequestById(job.requestId)
                val dto = ApiRepository.pushJob(
                    context = context,
                    job = job,
                    serverQuoteId = quote?.serverId,
                    serverRequestId = request?.serverId
                )
                if (dto != null) {
                    db.jobDao().setServerId(job.id, dto.id)
                    true
                } else false
            }

            TYPE_JOB_STATUS -> {
                val job = db.jobDao().getJobById(item.localId)
                if (job == null) {
                    Log.w(TAG, "drop JOB_STATUS: local row ${item.localId} gone")
                    return true
                }
                val serverId = job.serverId
                if (serverId == null) {
                    Log.w(TAG, "drop JOB_STATUS: local row ${item.localId} has no serverId")
                    return true
                }
                ApiRepository.updateJobStatus(
                    context, serverId,
                    json.optString("status", JobStatus.PENDING.name)
                )
            }

            TYPE_JOB_ON_WAY -> {
                val job = db.jobDao().getJobById(item.localId)
                if (job == null) {
                    Log.w(TAG, "drop JOB_ON_WAY: local row ${item.localId} gone")
                    return true
                }
                val serverId = job.serverId
                if (serverId == null) {
                    Log.w(TAG, "drop JOB_ON_WAY: local row ${item.localId} has no serverId")
                    return true
                }
                ApiRepository.setJobOnWay(
                    context, serverId,
                    json.optBoolean("onWay", false)
                )
            }

            else -> {
                Log.w(TAG, "unknown sync type: ${item.type}")
                true
            }
        }
    }
}