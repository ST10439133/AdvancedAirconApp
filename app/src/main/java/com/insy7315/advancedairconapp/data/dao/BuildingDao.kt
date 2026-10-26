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

    @Query("SELECT * FROM buildings WHERE name = :name AND latitude != 0 LIMIT 1")
    suspend fun findByNameWithCoords(name: String): BuildingEntity?

    @Query("SELECT * FROM buildings WHERE name = :name LIMIT 1")
    suspend fun findFirstByName(name: String): BuildingEntity?

    @Query("""
        SELECT DISTINCT
            CASE
                WHEN fullAddress IS NOT NULL AND fullAddress != ''
                    THEN fullAddress
                ELSE address
            END
        FROM buildings
        WHERE (fullAddress LIKE '%' || :query || '%'
               OR address LIKE '%' || :query || '%')
        ORDER BY name ASC
        LIMIT 8
    """)
    suspend fun findAddressesLike(query: String): List<String>

    // TEMP: debug helper
    @Query("SELECT * FROM buildings")
    suspend fun getAllBuildingsDebug(): List<BuildingEntity>

    @Query("DELETE FROM buildings WHERE userId = :userId")
    suspend fun deleteAllBuildings(userId: String)

    @Query("UPDATE buildings SET serverId = :serverId WHERE id = :localId")
    suspend fun setServerId(localId: Int, serverId: Int)
}