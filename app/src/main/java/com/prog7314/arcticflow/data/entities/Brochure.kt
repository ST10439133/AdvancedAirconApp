package com.prog7314.arcticflow.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "brochures")
data class Brochure(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val productId: Int,
    val fileName: String,
    val filePath: String,
    val fileType: String,
    val uploadedDate: Date = Date(),
    val version: String = "1.0"
)