package com.insy7315.advancedaircornapp.data.dao

import androidx.room.*
import com.insy7315.advancedaircornapp.data.entities.Product
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {

    @Insert
    suspend fun insertProduct(product: Product): Long

    @Insert
    suspend fun insertProducts(products: List<Product>)

    @Update
    suspend fun updateProduct(product: Product)

    @Delete
    suspend fun deleteProduct(product: Product)

    @Query("SELECT * FROM products ORDER BY name ASC")
    fun getAllProducts(): Flow<List<Product>>

    @Query("SELECT * FROM products WHERE id = :productId")
    suspend fun getProductById(productId: Int): Product?

    @Query("SELECT * FROM products WHERE brand = :brand ORDER BY name ASC")
    fun getProductsByBrand(brand: String): Flow<List<Product>>

    @Query("SELECT * FROM products WHERE isFavorite = 1 ORDER BY name ASC")
    fun getFavoriteProducts(): Flow<List<Product>>

    @Query("SELECT * FROM products WHERE price BETWEEN :minPrice AND :maxPrice")
    fun getProductsByPriceRange(minPrice: Double, maxPrice: Double): Flow<List<Product>>

    @Query("SELECT * FROM products ORDER BY price ASC")
    fun getProductsPriceLowToHigh(): Flow<List<Product>>

    @Query("SELECT * FROM products ORDER BY price DESC")
    fun getProductsPriceHighToLow(): Flow<List<Product>>

    @Query("SELECT * FROM products ORDER BY rating DESC")
    fun getProductsByRating(): Flow<List<Product>>

    @Query("SELECT DISTINCT brand FROM products ORDER BY brand ASC")
    suspend fun getAllBrands(): List<String>

    @Query("UPDATE products SET isFavorite = :isFavorite WHERE id = :productId")
    suspend fun updateFavoriteStatus(productId: Int, isFavorite: Boolean)

    @Query("DELETE FROM products")
    suspend fun deleteAllProducts()

    @Query("SELECT COUNT(*) FROM products")
    suspend fun getProductCount(): Int

    // app/src/main/java/com/prog7314/arcticflow/data/dao/ProductDao.kt
    @Query("SELECT * FROM products")
    suspend fun getAllProductsOnce(): List<Product>
}
