package com.insy7315.advancedairconapp.data.dao

import androidx.room.*
import com.insy7315.advancedairconapp.data.entities.BuildingEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BuildingDao {
    @Insert
    suspend fun insertBuilding(building: BuildingEntity): Long

    @Update
    suspend fun updateBuilding(building: BuildingEntity)

    @Delete
    suspend fun deleteBuilding(building: BuildingEntity)

    @Query("SELECT * FROM buildings WHERE userId = :userId ORDER BY name ASC")
    fun getBuildingsByUser(userId: String): Flow<List<BuildingEntity>>

    @Query("SELECT * FROM buildings WHERE id = :buildingId")
    suspend fun getBuildingById(buildingId: Int): BuildingEntity?

    @Query("DELETE FROM buildings WHERE userId = :userId")
    suspend fun deleteAllBuildings(userId: String)
}