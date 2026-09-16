// app/src/main/java/com/prog7314/arcticflow/data/network/SupabaseManager.kt
package com.prog7314.arcticflow.data.network

import com.prog7314.arcticflow.BuildConfig
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

    /**
     * Public base URL for product assets (images, PDFs) stored in the
     * "products" bucket on Supabase Storage.
     *
     * Example result:
     *   https://<your-project>.supabase.co/storage/v1/object/public/products/
     *
     * Usage:
     *   productBaseUrl + "Samsung_Inverter_12000.png"
     */
    val productBaseUrl: String
        get() {
            // Normalise: strip any scheme, then strip any leading/trailing slashes
            // so we never end up with a double slash in the final URL.
            val host = client.supabaseUrl
                .removePrefix("https://")
                .removePrefix("http://")
                .trimStart('/')
                .trimEnd('/')

            return "https://$host/storage/v1/object/public/products/"
        }
}