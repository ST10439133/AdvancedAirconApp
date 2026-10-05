// app/src/main/java/com/insy7315/advancedairconapp/viewmodels/ProductViewModel.kt
//Google for Developers. 2026. StateFlow and SharedFlow | Android Developers. [Online]. Available at: https://developer.android.com/kotlin/flow/stateflow-and-sharedflow [Accessed: 5 October 2026].
//Google for Developers. 2026. Combine flows | Android Developers. [Online]. Available at: https://developer.android.com/kotlin/flow/combine [Accessed: 5 October 2026].

package com.insy7315.advancedairconapp.viewmodels

import android.app.Application
import android.net.Uri
import android.util.Log
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

data class ProductUiState(
    val query: String = "",
    val brand: String? = null,
    val sortType: SortType = SortType.NAME_ASC,
    val filterType: FilterType = FilterType.ALL,
    val priceMin: Double = 0.0,
    val priceMax: Double = 100_000.0
)

class ProductViewModel(application: Application) : AndroidViewModel(application) {

    private companion object {
        const val TAG = "ProductFilter"
    }

    // Repository
    private val repository = ProductRepository(
        ArcticFlowDatabase.getDatabase(application).productDao(),
        ArcticFlowDatabase.getDatabase(application).brochureDao()
    )

    init {
        Log.d(TAG, "ProductViewModel constructed — new code is live")
    }

    // Legacy state flows (kept for UI compatibility)
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedBrand = MutableStateFlow<String?>(null)
    val selectedBrand: StateFlow<String?> = _selectedBrand.asStateFlow()

    private val _sortType = MutableStateFlow(SortType.NAME_ASC)
    val sortType: StateFlow<SortType> = _sortType.asStateFlow()

    private val _filterType = MutableStateFlow(FilterType.ALL)
    val filterType: StateFlow<FilterType> = _filterType.asStateFlow()

    private val _priceRange = MutableStateFlow(Pair(0.0, 100_000.0))
    val priceRange: StateFlow<Pair<Double, Double>> = _priceRange.asStateFlow()

    private val _brands = MutableStateFlow<List<String>>(emptyList())
    val brands: StateFlow<List<String>> = _brands.asStateFlow()

    private val _isUploading = MutableStateFlow(false)
    val isUploading: StateFlow<Boolean> = _isUploading.asStateFlow()

    // Atomic UI state (the one the pipeline actually uses)
    private val _uiState = MutableStateFlow(ProductUiState())
    val uiState: StateFlow<ProductUiState> = _uiState.asStateFlow()

    // Raw catalogue
    private val catalogue: Flow<List<Product>> =
        repository.getAllProducts()
            .onEach { Log.d(TAG, "Room emitted ${it.size} products") }

    // Derived, filtered, sorted list
    val products: StateFlow<List<Product>> =
        combine(catalogue, _uiState) { all, state ->
            applyFilters(all, state)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    init {
        loadBrands()
    }

    // Filtering / sorting
    private fun applyFilters(all: List<Product>, state: ProductUiState): List<Product> {
        Log.d(
            TAG,
            "applyFilters:\n" +
                    "  total from Room = ${all.size}\n" +
                    "  query='${state.query}'\n" +
                    "  brand='${state.brand}'\n" +
                    "  filterType=${state.filterType}\n" +
                    "  sortType=${state.sortType}"
        )

        if (all.isNotEmpty()) {
            Log.d(
                TAG,
                "  DB sample: " + all.take(5).joinToString { "'${it.name}'|brand='${it.brand}'" }
            )
        }

        // 1. Base filter
        val base: List<Product> = when (state.filterType) {
            FilterType.FAVORITES -> all.filter { it.isFavorite }
            FilterType.BY_BRAND -> {
                val b = state.brand?.trim()?.lowercase()
                if (b.isNullOrBlank()) all
                else all.filter { it.brand.trim().lowercase() == b }
            }
            FilterType.BY_PRICE_RANGE ->
                all.filter { it.price in state.priceMin..state.priceMax }
            FilterType.ALL -> all
        }

        // 2. Search query
        val q = state.query.trim()
        val searched = if (q.isEmpty()) base else base.filter { p ->
            p.name.contains(q, ignoreCase = true) ||
                    p.brand.contains(q, ignoreCase = true) ||
                    p.model.contains(q, ignoreCase = true)
        }

        // 3. Sort
        val sorted = when (state.sortType) {
            SortType.NAME_ASC -> searched.sortedBy { it.name.lowercase() }
            SortType.NAME_DESC -> searched.sortedByDescending { it.name.lowercase() }
            SortType.PRICE_LOW_TO_HIGH -> searched.sortedBy { it.price }
            SortType.PRICE_HIGH_TO_LOW -> searched.sortedByDescending { it.price }
            SortType.RATING_HIGH_TO_LOW -> searched.sortedByDescending { it.rating }
        }

        Log.d(TAG, "  FINAL = ${sorted.size}")
        return sorted
    }

    // Setters
    private fun updateState(transform: (ProductUiState) -> ProductUiState) {
        _uiState.value = transform(_uiState.value)
    }

    private fun loadBrands() {
        viewModelScope.launch {
            val b = repository.getAllBrands()
            Log.d(TAG, "loadBrands: $b")
            _brands.value = b
        }
    }

    fun searchProducts(query: String) {
        Log.d(TAG, "searchProducts('$query')")
        _searchQuery.value = query
        updateState { it.copy(query = query) }
    }

    fun filterByBrand(brand: String?) {
        Log.d(TAG, "filterByBrand('$brand')")
        _selectedBrand.value = brand
        val newType = if (brand != null) FilterType.BY_BRAND else FilterType.ALL
        _filterType.value = newType
        updateState { it.copy(brand = brand, filterType = newType) }
    }

    fun filterByPriceRange(min: Double, max: Double) {
        Log.d(TAG, "filterByPriceRange($min..$max)")
        _priceRange.value = Pair(min, max)
        _filterType.value = FilterType.BY_PRICE_RANGE
        updateState {
            it.copy(priceMin = min, priceMax = max, filterType = FilterType.BY_PRICE_RANGE)
        }
    }

    fun sortProducts(sortType: SortType) {
        Log.d(TAG, "sortProducts($sortType)")
        _sortType.value = sortType
        updateState { it.copy(sortType = sortType) }
    }

    fun toggleFavorite(productId: Int, isFavorite: Boolean) {
        viewModelScope.launch {
            repository.toggleFavorite(productId, isFavorite)
        }
    }

    suspend fun getProductById(id: Int): Product? = repository.getProductById(id)

    fun addProduct(product: Product) {
        viewModelScope.launch {
            repository.insertProduct(product)
            loadBrands()
        }
    }

    fun deleteProduct(product: Product) {
        viewModelScope.launch {
            repository.deleteProduct(product)
            loadBrands()
        }
    }

    fun clearAllProducts() {
        viewModelScope.launch {
            repository.clearAllProducts()
            loadBrands()
        }
    }

    // Supabase uploads
    suspend fun uploadProductImage(
        uri: Uri,
        fileName: String,
        contentResolver: android.content.ContentResolver
    ): String? {
        _isUploading.value = true
        return try {
            withContext(Dispatchers.IO) {
                val inputStream = contentResolver.openInputStream(uri)
                    ?: throw IllegalStateException("Cannot open URI: $uri")

                val tempFile = File.createTempFile("temp_upload_", ".jpg")
                inputStream.use { input ->
                    tempFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }

                SupabaseManager.client
                    .storage
                    .from("products")
                    .upload(fileName, tempFile) { upsert = true }

                tempFile.delete()

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

    suspend fun uploadProductCatalogue(
        uri: Uri,
        fileName: String,
        contentResolver: android.content.ContentResolver
    ): String? {
        _isUploading.value = true
        return try {
            withContext(Dispatchers.IO) {
                val inputStream = contentResolver.openInputStream(uri)
                    ?: throw IllegalStateException("Cannot open URI: $uri")

                val safeName = if (fileName.endsWith(".pdf", ignoreCase = true))
                    fileName else "$fileName.pdf"

                val tempFile = File.createTempFile("temp_catalogue_", ".pdf")
                inputStream.use { input ->
                    tempFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }

                SupabaseManager.client
                    .storage
                    .from("products")
                    .upload(safeName, tempFile) { upsert = true }

                tempFile.delete()

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

    fun syncDataFromAzure() {
        viewModelScope.launch {
            try {
                loadBrands()
            } catch (_: Exception) { }
        }
    }
}