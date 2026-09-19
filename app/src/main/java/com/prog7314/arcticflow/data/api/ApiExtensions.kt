package com.prog7314.arcticflow.data.api

import android.util.Log
import retrofit2.HttpException
import java.io.IOException


// safeApiCall - runs a suspend API call and returns null on
// failure. This is what keeps the app resilient when the API
// is unreachable (offline mode). It also logs the failure so
// we can diagnose issues in Logcat.

suspend fun <T> safeApiCall(
    tag: String = "ApiCall",
    block: suspend () -> T
): T? {
    return try {
        block()
    } catch (e: HttpException) {
        Log.w(tag, "HTTP ${e.code()}: ${e.message()}")
        null
    } catch (e: IOException) {
        Log.w(tag, "Network error (offline?): ${e.message}")
        null
    } catch (e: Exception) {
        Log.e(tag, "Unexpected API error: ${e.message}", e)
        null
    }
}