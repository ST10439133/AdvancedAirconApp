package com.prog7314.arcticflow

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.prog7314.arcticflow.auth.AuthState
import com.prog7314.arcticflow.auth.AuthViewModel
import com.prog7314.arcticflow.data.ArcticFlowDatabase
import com.prog7314.arcticflow.data.SampleData
import com.prog7314.arcticflow.ui.screens.*
import com.prog7314.arcticflow.ui.theme.ArcticFlowTheme
import com.prog7314.arcticflow.ui.theme.ThemeManager
import com.prog7314.arcticflow.ui.theme.ThemeState
import com.prog7314.arcticflow.utils.LocaleManager
import com.prog7314.arcticflow.viewmodels.ProductViewModel
import com.google.firebase.FirebaseApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val themeManager by lazy { ThemeManager(this) }

    // Apply saved locale when app starts
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

            ArcticFlowTheme(
                themeState = themeState.value,
                content = {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        ArcticFlowApp(
                            themeState = themeState.value,
                            onThemeChange = { newTheme ->
                                themeState.value = newTheme
                                themeManager.saveThemeState(newTheme)
                            }
                        )
                    }
                }
            )
        }
    }

    @Composable
    fun ArcticFlowApp(
        themeState: ThemeState,
        onThemeChange: (ThemeState) -> Unit
    ) {
        val authViewModel: AuthViewModel = viewModel()
        val productViewModel: ProductViewModel = viewModel()
        val authState by authViewModel.authState.collectAsState()
        val navController = rememberNavController()

        var showRegister by remember { mutableStateOf(false) }

        LaunchedEffect(authState) {
            Log.d("MainActivity", "AuthState: $authState")
        }

        when (authState) {
            is AuthState.Authenticated -> {
                Log.d("MainActivity", "User is authenticated: ${(authState as AuthState.Authenticated).user.email}")

                NavHost(
                    navController = navController,
                    startDestination = "product_list"
                ) {
                    composable("product_list") {
                        ProductListScreen(
                            viewModel = productViewModel,
                            authViewModel = authViewModel,
                            onNavigateToSettings = {
                                navController.navigate("settings")
                            }
                        )
                    }

                    composable("settings") {
                        SettingsScreen(
                            authViewModel = authViewModel,
                            onNavigateToProfile = {
                                navController.navigate("profile")
                            },
                            onNavigateToEditProfile = {
                                navController.navigate("edit_profile")
                            },
                            onSignOut = {
                                authViewModel.signOut()
                                navController.popBackStack()
                            },
                            onThemeChange = onThemeChange,
                            currentTheme = themeState,
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }

                    composable("profile") {
                        ProfileScreen(
                            authViewModel = authViewModel,
                            onNavigateBack = {
                                navController.popBackStack()
                            },
                            onNavigateToEditProfile = {
                                navController.navigate("edit_profile")
                            }
                        )
                    }

                    composable("edit_profile") {
                        EditProfileScreen(
                            authViewModel = authViewModel,
                            onNavigateBack = {
                                navController.popBackStack()
                            },
                            onSaveSuccess = {
                                navController.popBackStack()
                            }
                        )
                    }
                }
            }
            is AuthState.Loading -> {
                var showTimeout by remember { mutableStateOf(false) }

                LaunchedEffect(Unit) {
                    delay(3000)
                    showTimeout = true
                    if (authState is AuthState.Loading) {
                        Log.w("MainActivity", "Loading timeout - forcing unauthenticated")
                        authViewModel.signOut()
                    }
                }

                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    if (showTimeout) {
                        Text(
                            text = "Loading is taking too long...",
                            modifier = Modifier.padding(16.dp)
                        )
                    } else {
                        CircularProgressIndicator()
                    }
                }
            }
            is AuthState.Error -> {
                Log.e("MainActivity", "Auth error: ${(authState as AuthState.Error).message}")
                LoginScreen(
                    viewModel = authViewModel,
                    onLoginSuccess = {},
                    onNavigateToRegister = { showRegister = true }
                )
            }
            else -> {
                Log.d("MainActivity", "Showing login/register screens")
                if (showRegister) {
                    RegisterScreen(
                        viewModel = authViewModel,
                        onRegisterSuccess = {
                            showRegister = false
                        },
                        onNavigateToLogin = { showRegister = false }
                    )
                } else {
                    LoginScreen(
                        viewModel = authViewModel,
                        onLoginSuccess = {},
                        onNavigateToRegister = { showRegister = true }
                    )
                }
            }
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