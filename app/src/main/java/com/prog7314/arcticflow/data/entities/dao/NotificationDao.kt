// app/src/main/java/com/prog7314/arcticflow/data/dao/NotificationDao.kt
package com.prog7314.arcticflow.data.dao

import androidx.room.*
import com.prog7314.arcticflow.data.entities.Notification
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationDao {
    @Insert
    suspend fun insertNotification(notification: Notification): Long

    @Insert
    suspend fun insertNotifications(notifications: List<Notification>)

    @Update
    suspend fun updateNotification(notification: Notification)

    @Delete
    suspend fun deleteNotification(notification: Notification)

    @Query("SELECT * FROM notifications WHERE userId = :userId ORDER BY timestamp DESC")
    fun getNotificationsForUser(userId: String): Flow<List<Notification>>

    @Query("SELECT * FROM notifications WHERE userId = :userId AND isRead = 0 ORDER BY timestamp DESC")
    fun getUnreadNotifications(userId: String): Flow<List<Notification>>

    // CHANGED: was `suspend fun getUnreadCount(...): Int`
    // Now returns a reactive Flow<Int> so the ViewModel can observe it
    // without re-querying the whole list.
    @Query("SELECT COUNT(*) FROM notifications WHERE userId = :userId AND isRead = 0")
    fun getUnreadCount(userId: String): Flow<Int>

    @Query("UPDATE notifications SET isRead = 1 WHERE userId = :userId")
    suspend fun markAllAsRead(userId: String)

    @Query("UPDATE notifications SET isRead = 1 WHERE id = :notificationId")
    suspend fun markAsRead(notificationId: Int)

    @Query("DELETE FROM notifications WHERE userId = :userId")
    suspend fun deleteAllNotifications(userId: String)

    @Query("DELETE FROM notifications WHERE userId = :userId AND isRead = 1")
    suspend fun deleteReadNotifications(userId: String)
}