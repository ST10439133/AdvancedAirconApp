// app/src/main/java/com/prog7314/arcticflow/data/repository/NotificationRepository.kt
package com.prog7314.arcticflow.data.repository

import com.prog7314.arcticflow.data.dao.NotificationDao
import com.prog7314.arcticflow.data.entities.Notification
import kotlinx.coroutines.flow.Flow

class NotificationRepository(
    private val notificationDao: NotificationDao
) {
    suspend fun addNotification(notification: Notification): Long {
        return notificationDao.insertNotification(notification)
    }

    suspend fun addNotifications(notifications: List<Notification>) {
        notificationDao.insertNotifications(notifications)
    }

    suspend fun markAsRead(notificationId: Int) {
        notificationDao.markAsRead(notificationId)
    }

    suspend fun markAllAsRead(userId: String) {
        notificationDao.markAllAsRead(userId)
    }

    suspend fun deleteNotification(notification: Notification) {
        notificationDao.deleteNotification(notification)
    }

    suspend fun deleteReadNotifications(userId: String) {
        notificationDao.deleteReadNotifications(userId)
    }

    // CHANGED: now returns a Flow<Int> instead of a suspend Int
    fun getUnreadCount(userId: String): Flow<Int> {
        return notificationDao.getUnreadCount(userId)
    }

    fun getNotificationsForUser(userId: String): Flow<List<Notification>> {
        return notificationDao.getNotificationsForUser(userId)
    }

    fun getUnreadNotifications(userId: String): Flow<List<Notification>> {
        return notificationDao.getUnreadNotifications(userId)
    }
}