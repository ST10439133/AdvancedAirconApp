// app/src/main/java/com/insy7315/advancedairconapp/data/entities/Notification.kt
//IEEE Xplore. 2024. A Comprehensive Analysis of Push Notification Technology in Mobile Applications and Its Impact on User Engagement. [Online]. Available at: https://ieeexplore.ieee.org/document/10459575 [Accessed: 5 October 2026].
//IEEE Xplore. 2023. Design and Implementation of Real-Time Notification System Based on Android. [Online]. Available at: https://ieeexplore.ieee.org/document/10165401 [Accessed: 5 October 2026].
//Android Developers. 2026. Save data in a local database using Room. [Online]. Available at: https://developer.android.com/training/data-storage/room [Accessed: 5 October 2026].

package com.insy7315.advancedairconapp.data.entities

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
    val relatedId: String? = null,
    val userId: String
)

enum class NotificationType {
    JOB, QUOTE, SYSTEM
}

data class NotificationUiState(
    val notifications: List<Notification> = emptyList(),
    val unreadCount: Int = 0
)