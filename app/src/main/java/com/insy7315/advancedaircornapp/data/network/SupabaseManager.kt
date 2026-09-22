// app/src/main/java/com/insy7315/advancedaircornapp/data/network/SupabaseManager.kt
package com.insy7315.advancedaircornapp.data.network

import com.insy7315.advancedaircornapp.BuildConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.storage.Storage

object SupabaseManager {

    private val SUPABASE_URL: String = BuildConfig.SUPABASE_URL
    private val SUPABASE_KEY: String = BuildConfig.SUPABASE_ANON_KEY

    val client: SupabaseClient = createSupabaseClient(
        supabaseUrl = SUPABASE_URL,
        supabaseKey = SUPABASE_KEY
    ) {
        install(Storage)
    }


    val productBaseUrl: String
        get() {

            val host = client.supabaseUrl
                .removePrefix("https://")
                .removePrefix("http://")
                .trimStart('/')
                .trimEnd('/')

            return "https://$host/storage/v1/object/public/products/"
        }
}