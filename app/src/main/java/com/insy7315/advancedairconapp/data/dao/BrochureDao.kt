package com.insy7315.advancedairconapp.data.dao

import androidx.room.*
import com.insy7315.advancedairconapp.data.entities.Brochure
import kotlinx.coroutines.flow.Flow

@Dao
interface BrochureDao {

    @Insert
    suspend fun insertBrochure(brochure: Brochure): Long

    @Insert
    suspend fun insertBrochures(brochures: List<Brochure>)

    @Update
    suspend fun updateBrochure(brochure: Brochure)

    @Delete
    suspend fun deleteBrochure(brochure: Brochure)

    @Query("SELECT * FROM brochures WHERE productId = :productId")
    fun getBrochuresByProductId(productId: Int): Flow<List<Brochure>>

    @Query("SELECT * FROM brochures WHERE id = :brochureId")
    suspend fun getBrochureById(brochureId: Int): Brochure?

    @Query("SELECT * FROM brochures WHERE fileType = :fileType AND productId = :productId")
    fun getBrochuresByType(productId: Int, fileType: String): Flow<List<Brochure>>

    @Query("DELETE FROM brochures WHERE productId = :productId")
    suspend fun deleteBrochuresByProductId(productId: Int)
}