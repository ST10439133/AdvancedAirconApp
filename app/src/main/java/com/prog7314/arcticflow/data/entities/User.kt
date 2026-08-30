package com.prog7314.arcticflow.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey
    val uid: String,
    val email: String,
    val displayName: String? = null,
    val role: UserRole = UserRole.TECHNICIAN,
    val phoneNumber: String? = null,
    val photoUrl: String? = null,
    val isEmailVerified: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

enum class UserRole {
    MANAGER,
    TECHNICIAN
}