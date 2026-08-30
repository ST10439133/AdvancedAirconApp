package com.prog7314.arcticflow

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
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
import com.prog7314.arcticflow.viewmodels.ProductViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val themeManager by lazy { ThemeManager(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        loadSampleData()

        setContent {
            val themeState = remember { mutableStateOf(themeManager.getThemeState()) }

            ArcticFlowTheme(themeState = themeState.value) {
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

        when (authState) {
            is AuthState.Authenticated -> {
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
                            currentTheme = themeState
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
                LoadingScreen()
            }
            else -> {
                var showRegister by remember { mutableStateOf(false) }
                if (showRegister) {
                    RegisterScreen(
                        viewModel = authViewModel,
                        onRegisterSuccess = { showRegister = false },
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

    @Composable
    fun LoadingScreen() {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            CircularProgressIndicator(
                modifier = Modifier.fillMaxSize()
            )
        }
    }

    private fun loadSampleData() {
        CoroutineScope(Dispatchers.IO).launch {
            val database = ArcticFlowDatabase.getDatabase(applicationContext)
            val productDao = database.productDao()
            val count = productDao.getProductCount()
            if (count == 0) {
                productDao.insertProducts(SampleData.getSampleProducts())
            }
        }
    }
}