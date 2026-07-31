package com.prog7314.arcticflow.data.entities

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
    val imagePath: String,      // Path in Azure File Share
    val brochurePath: String,   // Path in Azure File Share
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