package com.insy7315.advancedaircornapp.data.api

import android.content.Context


object IdMap {

    private const val PREFS = "arcticflow_id_map"

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    // Buildings
    fun putBuilding(context: Context, localId: Int, serverId: Int) {
        prefs(context).edit().putInt("building_$localId", serverId).apply()
    }
    fun getBuilding(context: Context, localId: Int): Int? =
        prefs(context).getInt("building_$localId", -1).takeIf { it > 0 }

    // Service Requests
    fun putRequest(context: Context, localId: Int, serverId: Int) {
        prefs(context).edit().putInt("request_$localId", serverId).apply()
    }
    fun getRequest(context: Context, localId: Int): Int? =
        prefs(context).getInt("request_$localId", -1).takeIf { it > 0 }

    // Quotes
    fun putQuote(context: Context, localId: Int, serverId: Int) {
        prefs(context).edit().putInt("quote_$localId", serverId).apply()
    }
    fun getQuote(context: Context, localId: Int): Int? =
        prefs(context).getInt("quote_$localId", -1).takeIf { it > 0 }

    // Jobs
    fun putJob(context: Context, localId: Int, serverId: Int) {
        prefs(context).edit().putInt("job_$localId", serverId).apply()
    }
    fun getJob(context: Context, localId: Int): Int? =
        prefs(context).getInt("job_$localId", -1).takeIf { it > 0 }
}