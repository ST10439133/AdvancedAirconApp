// app/src/main/java/com/prog7314/arcticflow/data/repository/ProductRepository.kt
package com.prog7314.arcticflow.data.repository

import com.prog7314.arcticflow.data.dao.BrochureDao
import com.prog7314.arcticflow.data.dao.ProductDao
import com.prog7314.arcticflow.data.entities.Brochure
import com.prog7314.arcticflow.data.entities.Product
import kotlinx.coroutines.flow.Flow  // ADD THIS IMPORT

class ProductRepository(
    private val productDao: ProductDao,
    private val brochureDao: BrochureDao
) {

    // Product operations
    suspend fun insertProduct(product: Product): Long = productDao.insertProduct(product)

    suspend fun insertProducts(products: List<Product>) = productDao.insertProducts(products)

    suspend fun updateProduct(product: Product) = productDao.updateProduct(product)

    suspend fun deleteProduct(product: Product) = productDao.deleteProduct(product)

    fun getAllProducts(): Flow<List<Product>> = productDao.getAllProducts()

    suspend fun getProductById(id: Int): Product? = productDao.getProductById(id)

    fun getProductsByBrand(brand: String): Flow<List<Product>> = productDao.getProductsByBrand(brand)

    fun getFavoriteProducts(): Flow<List<Product>> = productDao.getFavoriteProducts()

    fun getProductsByPriceRange(min: Double, max: Double): Flow<List<Product>> =
        productDao.getProductsByPriceRange(min, max)

    suspend fun toggleFavorite(productId: Int, isFavorite: Boolean) =
        productDao.updateFavoriteStatus(productId, isFavorite)

    suspend fun getAllBrands(): List<String> = productDao.getAllBrands()

    suspend fun getProductCount(): Int = productDao.getProductCount()

    suspend fun clearAllProducts() = productDao.deleteAllProducts()

    // Brochure operations
    suspend fun addBrochure(brochure: Brochure) = brochureDao.insertBrochure(brochure)

    fun getBrochuresForProduct(productId: Int): Flow<List<Brochure>> =
        brochureDao.getBrochuresByProductId(productId)

    suspend fun getBrochureById(id: Int): Brochure? = brochureDao.getBrochureById(id)

    suspend fun deleteBrochuresForProduct(productId: Int) =
        brochureDao.deleteBrochuresByProductId(productId)
}