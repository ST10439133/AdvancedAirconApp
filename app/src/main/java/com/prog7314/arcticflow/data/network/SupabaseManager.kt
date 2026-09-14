// app/src/main/java/com/prog7314/arcticflow/data/network/SupabaseManager.kt
package com.prog7314.arcticflow.data.network

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.storage.Storage

object SupabaseManager {

    private const val SUPABASE_URL = "https://sxmpfwcneojlrguvrynl.supabase.co"

    private const val SUPABASE_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InN4bXBmd2NuZW9qbHJndXZyeW5sIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODkzODg0NzEsImV4cCI6MjEwNDk2NDQ3MX0.JDdLJsKvYhb-K4eRJYW7uPHvaxZGknd2XinYxZ-J5Uc"

    val client: SupabaseClient = createSupabaseClient(
        supabaseUrl = SUPABASE_URL,
        supabaseKey = SUPABASE_KEY
    ) {
        install(Storage)
    }
}