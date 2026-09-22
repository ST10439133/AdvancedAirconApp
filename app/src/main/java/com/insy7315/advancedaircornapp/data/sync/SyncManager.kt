package com.insy7315.advancedaircornapp.data.sync

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.insy7315.advancedaircornapp.data.ArcticFlowDatabase
import com.insy7315.advancedaircornapp.data.api.ApiRepository
import com.insy7315.advancedaircornapp.data.api.IdMap
import com.insy7315.advancedaircornapp.data.entities.JobStatus
import com.insy7315.advancedaircornapp.data.entities.QuoteStatus
import com.insy7315.advancedaircornapp.data.entities.RequestStatus
import com.insy7315.advancedaircornapp.data.entities.SyncQueueEntity
import com.insy7315.advancedaircornapp.data.network.NetworkMonitor
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
            val ok = try { replay(context, item) } catch (e: Exception) {
                Log.e(TAG, "replay failed for ${item.type} #${item.localId}", e); false
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

    private suspend fun replay(context: Context, item: SyncQueueEntity): Boolean {
        val json = JSONObject(item.payloadJson)
        val db = ArcticFlowDatabase.getDatabase(context)

        return when (item.type) {

            TYPE_REQUEST_CREATE -> {
                val request = db.serviceRequestDao().getRequestById(item.localId) ?: return false
                val building = db.buildingDao().getBuildingById(request.buildingId)
                val dto = ApiRepository.pushServiceRequest(
                    context = context,
                    request = request,
                    buildingName = building?.name,
                    fullAddress = building?.fullAddress,
                    serverBuildingId = IdMap.getBuilding(context, request.buildingId)
                )
                if (dto != null) { IdMap.putRequest(context, request.id, dto.id); true } else false
            }

            TYPE_REQUEST_STATUS -> ApiRepository.updateRequestStatus(
                context,
                item.localId,
                json.optString("status", RequestStatus.PENDING.name)
            )

            TYPE_QUOTE_CREATE -> {
                val quote = db.quoteDao().getQuoteById(item.localId) ?: return false
                val dto = ApiRepository.pushQuote(
                    context = context,
                    quote = quote,
                    serverRequestId = IdMap.getRequest(context, quote.requestId)
                )
                if (dto != null) { IdMap.putQuote(context, quote.id, dto.id); true } else false
            }

            TYPE_QUOTE_STATUS -> ApiRepository.updateQuoteStatus(
                context,
                item.localId,
                json.optString("status", QuoteStatus.PENDING.name)
            )

            TYPE_JOB_CREATE -> {
                val job = db.jobDao().getJobById(item.localId) ?: return false
                val dto = ApiRepository.pushJob(
                    context = context,
                    job = job,
                    serverQuoteId = IdMap.getQuote(context, job.quoteId),
                    serverRequestId = IdMap.getRequest(context, job.requestId)
                )
                if (dto != null) { IdMap.putJob(context, job.id, dto.id); true } else false
            }

            TYPE_JOB_STATUS -> ApiRepository.updateJobStatus(
                context,
                item.localId,
                json.optString("status", JobStatus.PENDING.name)
            )

            TYPE_JOB_ON_WAY -> ApiRepository.setJobOnWay(
                context,
                item.localId,
                json.optBoolean("onWay", false)
            )

            else -> { Log.w(TAG, "unknown sync type: ${item.type}"); true }
        }
    }
}