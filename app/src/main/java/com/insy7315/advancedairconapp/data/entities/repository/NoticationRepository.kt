// app/src/main/java/com/insy7315/advancedaircornapp/data/repository/NotificationRepository.kt
package com.insy7315.advancedairconapp.data.repository

import com.insy7315.advancedairconapp.data.dao.NotificationDao
import com.insy7315.advancedairconapp.data.entities.Notification
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