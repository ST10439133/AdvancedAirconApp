package com.prog7314.arcticflow

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.prog7314.arcticflow.data.ArcticFlowDatabase
import com.prog7314.arcticflow.data.SampleData
import com.prog7314.arcticflow.ui.screens.ProductListScreen
import com.prog7314.arcticflow.ui.theme.ArcticFlowTheme
import com.prog7314.arcticflow.viewmodels.ProductViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Load sample data for testing
        loadSampleData()

        setContent {
            ArcticFlowTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Create the ViewModel instance
                    val viewModel: ProductViewModel = viewModel()
                    ProductListScreen(viewModel = viewModel)
                }
            }
        }
    }

    private fun loadSampleData() {
        CoroutineScope(Dispatchers.IO).launch {
            val database = ArcticFlowDatabase.getDatabase(applicationContext)
            val productDao = database.productDao()

            // Check if data already exists
            val count = productDao.getProductCount()
            if (count == 0) {
                // Insert sample products
                productDao.insertProducts(SampleData.getSampleProducts())
            }
        }
    }
}