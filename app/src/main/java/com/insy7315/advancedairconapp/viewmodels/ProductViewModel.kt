// app/src/main/java/com/insy7315/advancedaircornapp/viewmodels/ProductViewModel.kt
package com.insy7315.advancedairconapp.viewmodels

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.insy7315.advancedairconapp.data.ArcticFlowDatabase
import com.insy7315.advancedairconapp.data.entities.FilterType
import com.insy7315.advancedairconapp.data.entities.Product
import com.insy7315.advancedairconapp.data.entities.SortType
import com.insy7315.advancedairconapp.data.network.SupabaseManager
import com.insy7315.advancedairconapp.data.repository.ProductRepository
import io.github.jan.supabase.storage.storage
import io.github.jan.supabase.storage.upload
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class ProductViewModel(application: Application) : AndroidViewModel(application) {

    // Initialize Repository
    private val repository = ProductRepository(
        ArcticFlowDatabase.getDatabase(application).productDao(),
        ArcticFlowDatabase.getDatabase(application).brochureDao()
    )

    // State flows for UI
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedBrand = MutableStateFlow<String?>(null)
    val selectedBrand: StateFlow<String?> = _selectedBrand.asStateFlow()

    private val _sortType = MutableStateFlow(SortType.NAME_ASC)
    val sortType: StateFlow<SortType> = _sortType.asStateFlow()

    private val _filterType = MutableStateFlow(FilterType.ALL)
    val filterType: StateFlow<FilterType> = _filterType.asStateFlow()

    private val _priceRange = MutableStateFlow(Pair(0.0, 100000.0))
    val priceRange: StateFlow<Pair<Double, Double>> = _priceRange.asStateFlow()

    private val _brands = MutableStateFlow<List<String>>(emptyList())
    val brands: StateFlow<List<String>> = _brands.asStateFlow()

    // Upload progress state (so the UI can show a spinner)
    private val _isUploading = MutableStateFlow(false)
    val isUploading: StateFlow<Boolean> = _isUploading.asStateFlow()

    // Combined filtered and sorted products
    val products = combine(
        _searchQuery,
        _selectedBrand,
        _sortType,
        _filterType,
        _priceRange
    ) { query, brand, sortType, filterType, priceRange ->
        val baseFlow = when (filterType) {
            FilterType.FAVORITES -> repository.getFavoriteProducts()
            FilterType.BY_BRAND -> {
                if (brand != null) repository.getProductsByBrand(brand)
                else repository.getAllProducts()
            }
            FilterType.BY_PRICE_RANGE ->
                repository.getProductsByPriceRange(priceRange.first, priceRange.second)
            else -> repository.getAllProducts()
        }

        baseFlow.map { products ->
            var filtered = products

            if (query.isNotEmpty()) {
                filtered = filtered.filter { product ->
                    product.name.contains(query, ignoreCase = true) ||
                            product.brand.contains(query, ignoreCase = true) ||
                            product.model.contains(query, ignoreCase = true)
                }
            }

            when (sortType) {
                SortType.NAME_ASC -> filtered.sortedBy { it.name }
                SortType.NAME_DESC -> filtered.sortedByDescending { it.name }
                SortType.PRICE_LOW_TO_HIGH -> filtered.sortedBy { it.price }
                SortType.PRICE_HIGH_TO_LOW -> filtered.sortedByDescending { it.price }
                SortType.RATING_HIGH_TO_LOW -> filtered.sortedByDescending { it.rating }
            }
        }
    }.flatMapLatest { it }

    init {
        loadBrands()
    }

    // Load brands from database
    private fun loadBrands() {
        viewModelScope.launch {
            _brands.value = repository.getAllBrands()
        }
    }

    // Search products
    fun searchProducts(query: String) {
        _searchQuery.value = query
    }

    // Filter by brand
    fun filterByBrand(brand: String?) {
        _selectedBrand.value = brand
        _filterType.value = if (brand != null) FilterType.BY_BRAND else FilterType.ALL
    }

    // Filter by price range
    fun filterByPriceRange(min: Double, max: Double) {
        _priceRange.value = Pair(min, max)
        _filterType.value = FilterType.BY_PRICE_RANGE
    }

    // Sort products
    fun sortProducts(sortType: SortType) {
        _sortType.value = sortType
    }

    // Toggle favorite status
    fun toggleFavorite(productId: Int, isFavorite: Boolean) {
        viewModelScope.launch {
            repository.toggleFavorite(productId, isFavorite)
        }
    }

    // Get single product by ID
    suspend fun getProductById(id: Int): Product? {
        return repository.getProductById(id)
    }

    // Add new product
    fun addProduct(product: Product) {
        viewModelScope.launch {
            repository.insertProduct(product)
            loadBrands()
        }
    }

    // Delete product
    fun deleteProduct(product: Product) {
        viewModelScope.launch {
            repository.deleteProduct(product)
            loadBrands()
        }
    }

    // Clear all products
    fun clearAllProducts() {
        viewModelScope.launch {
            repository.clearAllProducts()
            loadBrands()
        }
    }

    // SUPABASE IMAGE UPLOAD

    suspend fun uploadProductImage(
        uri: Uri,
        fileName: String,
        contentResolver: android.content.ContentResolver
    ): String? {
        _isUploading.value = true
        return try {
            withContext(Dispatchers.IO) {
                // 1. Copy the picked image into a temp file
                val inputStream = contentResolver.openInputStream(uri)
                    ?: throw IllegalStateException("Cannot open URI: $uri")

                val tempFile = File.createTempFile("temp_upload_", ".jpg")
                inputStream.use { input ->
                    tempFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }

                // 2. Upload to the "products" bucket in Supabase
                SupabaseManager.client
                    .storage
                    .from("products")
                    .upload(fileName, tempFile) {
                        upsert = true
                    }

                // 3. Clean up the temp file
                tempFile.delete()

                // 4. Return the public URL so we can save it to RoomDB
                val baseUrl = SupabaseManager.client.supabaseUrl
                    .removePrefix("https://")
                    .removePrefix("http://")
                "https://$baseUrl/storage/v1/object/public/products/$fileName"
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } finally {
            _isUploading.value = false
        }
    }

    // SUPABASE PDF CATALOGUE UPLOAD

    suspend fun uploadProductCatalogue(
        uri: Uri,
        fileName: String,
        contentResolver: android.content.ContentResolver
    ): String? {
        _isUploading.value = true
        return try {
            withContext(Dispatchers.IO) {
                // 1. Copy the picked PDF into a temp file
                val inputStream = contentResolver.openInputStream(uri)
                    ?: throw IllegalStateException("Cannot open URI: $uri")

                // Force .pdf extension so it's unambiguous
                val safeName = if (fileName.endsWith(".pdf", ignoreCase = true))
                    fileName else "$fileName.pdf"

                val tempFile = File.createTempFile("temp_catalogue_", ".pdf")
                inputStream.use { input ->
                    tempFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }

                // 2. Upload to the "products" bucket (same bucket works fine)
                SupabaseManager.client
                    .storage
                    .from("products")
                    .upload(safeName, tempFile) {
                        upsert = true
                    }

                // 3. Clean up
                tempFile.delete()

                // 4. Return the public URL
                val baseUrl = SupabaseManager.client.supabaseUrl
                    .removePrefix("https://")
                    .removePrefix("http://")
                "https://$baseUrl/storage/v1/object/public/products/$safeName"
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } finally {
            _isUploading.value = false
        }
    }

    // Refresh data from Azure
    fun syncDataFromAzure() {
        viewModelScope.launch {
            try {
                loadBrands()
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
}