package com.prog7314.arcticflow.data.api

import android.content.Context
import android.util.Log
import com.prog7314.arcticflow.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

// ============================================================
// Singleton manager for Retrofit + the JWT token.
//
// - get(context) returns a cached ArcticFlowApi instance pointed
//   at BuildConfig.API_BASE_URL.
// - saveToken / clearToken persist the JWT issued by the API
//   after POST /api/users/sync so every subsequent call is
//   automatically authenticated.
// - The OkHttp interceptor injects "Authorization: Bearer X"
//   into every outgoing request if a token exists.
// ============================================================
object ApiClient {

    private const val TAG = "ApiClient"
    private const val PREFS = "arcticflow_api"
    private const val KEY_TOKEN = "jwt_token"

    @Volatile
    private var api: ArcticFlowApi? = null

    fun get(context: Context): ArcticFlowApi {
        return api ?: synchronized(this) {
            api ?: build(context).also { api = it }
        }
    }

    private fun build(context: Context): ArcticFlowApi {
        val appContext = context.applicationContext

        val logging = HttpLoggingInterceptor { message ->
            Log.d(TAG, message)
        }.apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .addInterceptor { chain ->
                val token = appContext
                    .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                    .getString(KEY_TOKEN, null)

                val request = if (token != null) {
                    chain.request().newBuilder()
                        .addHeader("Authorization", "Bearer $token")
                        .build()
                } else {
                    chain.request()
                }

                chain.proceed(request)
            }
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        return retrofit.create(ArcticFlowApi::class.java)
    }

    fun saveToken(context: Context, token: String) {
        Log.d(TAG, "Saving JWT (length=${token.length})")
        context.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_TOKEN, token)
            .apply()
    }

    fun clearToken(context: Context) {
        context.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_TOKEN)
            .apply()
    }

    fun hasToken(context: Context): Boolean {
        return context.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .contains(KEY_TOKEN)
    }
}