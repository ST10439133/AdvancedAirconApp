// app/src/main/java/com/insy7315/advancedaircornapp/data/entities/Product.kt
package com.insy7315.advancedairconapp.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class Product(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val brand: String,
    val model: String,
    val btu: Int,
    val price: Double,
    val description: String,
    val imagePath: String,
    val brochurePath: String,
    val warrantyPath: String? = null,
    val rating: Float = 0f,
    val isFavorite: Boolean = false
)

enum class SortType {
    NAME_ASC,
    NAME_DESC,
    PRICE_LOW_TO_HIGH,
    PRICE_HIGH_TO_LOW,
    RATING_HIGH_TO_LOW
}

enum class FilterType {
    ALL,
    FAVORITES,
    BY_BRAND,
    BY_PRICE_RANGE
}