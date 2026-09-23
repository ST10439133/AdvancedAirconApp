package com.insy7315.advancedairconapp.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "buildings")
data class BuildingEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val serverId: Int? = null,

    val userId: String = "",
    val name: String = "",
    val address: String = "",
    val suburb: String = "",
    val city: String = "",
    val province: String = "",
    val postalCode: String = "",
    val fullAddress: String = "",
    val unitCount: Int = 1,
    val floors: Int = 1,
    val buildingType: BuildingType = BuildingType.RESIDENTIAL,
    val registeredDate: Long = System.currentTimeMillis(),
    val status: BuildingStatusEnum = BuildingStatusEnum.ACTIVE
)

enum class BuildingType {
    RESIDENTIAL,
    COMMERCIAL,
    INDUSTRIAL,
    MIXED_USE,
    INSTITUTIONAL
}

enum class BuildingStatusEnum {
    ACTIVE,
    INACTIVE,
    MAINTENANCE
}