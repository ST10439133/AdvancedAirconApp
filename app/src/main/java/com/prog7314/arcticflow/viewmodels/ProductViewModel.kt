package com.prog7314.arcticflow.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.prog7314.arcticflow.data.ArcticFlowDatabase
import com.prog7314.arcticflow.data.entities.FilterType
import com.prog7314.arcticflow.data.entities.Product
import com.prog7314.arcticflow.data.entities.SortType
import com.prog7314.arcticflow.data.repository.ProductRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

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

    // Combined filtered and sorted products
    val products = combine(
        _searchQuery,
        _selectedBrand,
        _sortType,
        _filterType,
        _priceRange
    ) { query, brand, sortType, filterType, priceRange ->
        // Get base flow based on filter
        val baseFlow = when (filterType) {
            FilterType.FAVORITES -> repository.getFavoriteProducts()
            FilterType.BY_BRAND -> {
                if (brand != null) repository.getProductsByBrand(brand)
                else repository.getAllProducts()
            }
            FilterType.BY_PRICE_RANGE -> repository.getProductsByPriceRange(priceRange.first, priceRange.second)
            else -> repository.getAllProducts()
        }

        // Apply search and sorting
        baseFlow.map { products ->
            var filtered = products

            // Apply search filter
            if (query.isNotEmpty()) {
                filtered = filtered.filter { product ->
                    product.name.contains(query, ignoreCase = true) ||
                            product.brand.contains(query, ignoreCase = true) ||
                            product.model.contains(query, ignoreCase = true)
                }
            }

            // Apply sorting
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

    // Refresh data from Azure
    fun syncDataFromAzure() {
        viewModelScope.launch {
            try {
                // You'll implement this when Azure integration is ready
                // repository.syncProductsFromAzure()
                loadBrands()
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
}