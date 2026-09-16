// app/src/main/java/com/prog7314/arcticflow/MainActivity.kt
package com.prog7314.arcticflow

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.prog7314.arcticflow.navigation.NavGraph as AppNavGraph
import com.prog7314.arcticflow.navigation.rememberNavManager
import com.prog7314.arcticflow.ui.theme.ArcticFlowTheme
import com.prog7314.arcticflow.ui.theme.ThemeManager
import com.prog7314.arcticflow.ui.theme.ThemeState
import com.prog7314.arcticflow.utils.LocaleManager
import com.google.firebase.FirebaseApp
import com.prog7314.arcticflow.data.ArcticFlowDatabase
import com.prog7314.arcticflow.data.SampleData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val themeManager by lazy { ThemeManager(this) }

    override fun attachBaseContext(newBase: Context) {
        val localeManager = LocaleManager(newBase)
        val locale = localeManager.getCurrentLocale()
        val config = Configuration(newBase.resources.configuration)
        config.setLocale(locale)
        val context = newBase.createConfigurationContext(config)
        super.attachBaseContext(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            FirebaseApp.initializeApp(this)
            Log.d("MainActivity", "Firebase initialized successfully")
        } catch (e: Exception) {
            Log.e("MainActivity", "Firebase initialization failed", e)
        }

        // Sync sample data (inserts new + updates changed fields)
        syncSampleData()

        setContent {
            val themeState = remember { mutableStateOf(themeManager.getThemeState()) }

            val navController = rememberNavController()
            val navManager = rememberNavManager(navController)

            ArcticFlowTheme(
                themeState = themeState.value,
                content = {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        AppNavGraph(
                            navController = navController,
                            navManager = navManager,
                            themeState = themeState.value,
                            onThemeChange = { newTheme: ThemeState ->
                                themeState.value = newTheme
                                themeManager.saveThemeState(newTheme)
                            }
                        )
                    }
                }
            )
        }
    }

    /**
     * Ensures the `products` table always mirrors [SampleData.getSampleProducts()]:
     *
     *  - Inserts products that don't exist in the DB yet (matched by name).
     *  - Updates existing rows when their fields have changed (imagePath,
     *    price, btu, rating, etc.).
     *  - Leaves the user's `isFavorite` flag untouched.
     *
     * Runs on a background dispatcher, so it never blocks the UI.
     */
    private fun syncSampleData() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val database = ArcticFlowDatabase.getDatabase(applicationContext)
                val productDao = database.productDao()

                val existingProducts = productDao.getAllProductsOnce()
                val existingByName = existingProducts.associateBy { it.name }

                val sampleProducts = SampleData.getSampleProducts()

                var insertedCount = 0
                var updatedCount = 0

                sampleProducts.forEach { sample ->
                    val existing = existingByName[sample.name]

                    if (existing == null) {
                        // Insert brand new product (keep sample's isFavorite)
                        productDao.insertProduct(sample)
                        insertedCount++
                    } else {
                        // Update fields that may have changed, but preserve the
                        // user's favourite choice and the DB-assigned id.
                        val merged = existing.copy(
                            brand = sample.brand,
                            model = sample.model,
                            btu = sample.btu,
                            price = sample.price,
                            description = sample.description,
                            imagePath = sample.imagePath,
                            brochurePath = sample.brochurePath,
                            warrantyPath = sample.warrantyPath,
                            rating = sample.rating
                            // NOTE: isFavorite intentionally NOT copied — user choice wins
                        )

                        if (merged != existing) {
                            productDao.updateProduct(merged)
                            updatedCount++
                        }
                    }
                }

                Log.d(
                    "MainActivity",
                    "Sample data sync complete: $insertedCount inserted, $updatedCount updated, " +
                            "${sampleProducts.size} total in SampleData"
                )
            } catch (e: Exception) {
                Log.e("MainActivity", "Error syncing sample data", e)
            }
        }
    }
}