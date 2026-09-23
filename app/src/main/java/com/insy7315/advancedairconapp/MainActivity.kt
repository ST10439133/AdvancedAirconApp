// app/src/main/java/com/insy7315/advancedairconapp/MainActivity.kt
package com.insy7315.advancedairconapp

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
import com.insy7315.advancedairconapp.navigation.NavGraph as AppNavGraph
import com.insy7315.advancedairconapp.navigation.rememberNavManager
import com.insy7315.advancedairconapp.ui.theme.ArcticFlowTheme
import com.insy7315.advancedairconapp.ui.theme.ThemeManager
import com.insy7315.advancedairconapp.ui.theme.ThemeState
import com.insy7315.advancedairconapp.utils.LocaleManager
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.insy7315.advancedairconapp.data.ArcticFlowDatabase
import com.insy7315.advancedairconapp.data.SampleData
import com.insy7315.advancedairconapp.data.sync.SyncWorker
import com.insy7315.advancedairconapp.services.LocationTrackingCoordinator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
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

        SyncWorker.schedule(applicationContext)
        syncSampleData()
        cleanUpStaleTracking()

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
     * On cold start, if we're a technician whose jobs are all idle, wipe any
     * lingering location row on the server for our uid. This kills the
     * phantom pin that appears when the app was killed without tapping
     * "stop tracking".
     */
    private fun cleanUpStaleTracking() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val firebaseUser = FirebaseAuth.getInstance().currentUser ?: return@launch
                val db = ArcticFlowDatabase.getDatabase(applicationContext)
                val localUser = db.userDao().getUserById(firebaseUser.uid) ?: return@launch

                if (localUser.role.name != "TECHNICIAN") return@launch

                // Room returns a Flow<List<Job>>. Take the first emission.
                val jobs = db.jobDao()
                    .getJobsByTechnician(localUser.uid)
                    .first()

                val onMyWay = jobs.any { it.technicianOnWay }
                if (!onMyWay) {
                    Log.d("MainActivity", "No active tracking — clearing stale location")
                    LocationTrackingCoordinator.stopForTechnician(
                        context = applicationContext,
                        technicianId = localUser.uid
                    )
                }
            } catch (e: Exception) {
                Log.w("MainActivity", "cleanUpStaleTracking failed", e)
            }
        }
    }

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
                        productDao.insertProduct(sample)
                        insertedCount++
                    } else {
                        val merged = existing.copy(
                            brand = sample.brand, model = sample.model,
                            btu = sample.btu, price = sample.price,
                            description = sample.description,
                            imagePath = sample.imagePath,
                            brochurePath = sample.brochurePath,
                            warrantyPath = sample.warrantyPath,
                            rating = sample.rating
                        )
                        if (merged != existing) {
                            productDao.updateProduct(merged)
                            updatedCount++
                        }
                    }
                }

                Log.d(
                    "MainActivity",
                    "Sample data sync: $insertedCount inserted, $updatedCount updated"
                )
            } catch (e: Exception) {
                Log.e("MainActivity", "Error syncing sample data", e)
            }
        }
    }
}