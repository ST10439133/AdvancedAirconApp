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

    @Query("SELECT * FROM buildings WHERE serverId = :serverId LIMIT 1")
    suspend fun getBuildingByServerId(serverId: Int): BuildingEntity?

    // NEW — used by the map to resolve destination coordinates from a
    // technician's location push (matching by building name)
    @Query("SELECT * FROM buildings WHERE name = :name AND latitude != 0 LIMIT 1")
    suspend fun findByNameWithCoords(name: String): BuildingEntity?

    @Query("SELECT * FROM buildings WHERE name = :name LIMIT 1")
    suspend fun findFirstByName(name: String): BuildingEntity?

    @Query("DELETE FROM buildings WHERE userId = :userId")
    suspend fun deleteAllBuildings(userId: String)

    @Query("UPDATE buildings SET serverId = :serverId WHERE id = :localId")
    suspend fun setServerId(localId: Int, serverId: Int)
}