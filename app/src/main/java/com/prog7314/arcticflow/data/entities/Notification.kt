// app/src/main/java/com/prog7314/arcticflow/data/entities/Notification.kt
package com.prog7314.arcticflow.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notifications")
data class Notification(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String,
    val message: String,
    val type: NotificationType = NotificationType.SYSTEM,
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false,
    val relatedId: String? = null, // Job ID, Quote ID, etc.
    val userId: String
)

enum class NotificationType {
    JOB, QUOTE, SYSTEM
}

data class NotificationUiState(
    val notifications: List<Notification> = emptyList(),
    val unreadCount: Int = 0
)