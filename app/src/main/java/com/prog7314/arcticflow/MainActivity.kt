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

        loadSampleData()

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

    private fun loadSampleData() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val database = ArcticFlowDatabase.getDatabase(applicationContext)
                val productDao = database.productDao()
                val count = productDao.getProductCount()
                if (count == 0) {
                    productDao.insertProducts(SampleData.getSampleProducts())
                    Log.d("MainActivity", "Sample data loaded successfully")
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Error loading sample data", e)
            }
        }
    }
}