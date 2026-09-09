// app/src/main/java/com/prog7314/arcticflow/viewmodels/NotificationViewModel.kt
package com.prog7314.arcticflow.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.prog7314.arcticflow.data.ArcticFlowDatabase
import com.prog7314.arcticflow.data.entities.Notification
import com.prog7314.arcticflow.data.entities.NotificationType
import com.prog7314.arcticflow.data.repository.NotificationRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class NotificationViewModel(
    private val repository: NotificationRepository,
    private val userId: String
) : ViewModel() {

    private val _notifications = MutableStateFlow<List<Notification>>(emptyList())
    val notifications: StateFlow<List<Notification>> = _notifications.asStateFlow()

    private val _unreadCount = MutableStateFlow(0)
    val unreadCount: StateFlow<Int> = _unreadCount.asStateFlow()

    private val _filterType = MutableStateFlow<NotificationType?>(null)
    val filterType: StateFlow<NotificationType?> = _filterType.asStateFlow()

    init {
        loadNotifications()
        loadUnreadCount()
    }

    private fun loadNotifications() {
        viewModelScope.launch {
            repository.getNotificationsForUser(userId)
                .collect { notificationList ->
                    val filtered = if (_filterType.value != null) {
                        notificationList.filter { it.type == _filterType.value }
                    } else {
                        notificationList
                    }
                    _notifications.value = filtered
                }
        }
    }

    private fun loadUnreadCount() {
        viewModelScope.launch {
            _unreadCount.value = repository.getUnreadCount(userId)
        }
    }

    fun setFilter(type: NotificationType?) {
        _filterType.value = type
        // Trigger re-filter
        viewModelScope.launch {
            repository.getNotificationsForUser(userId)
                .collect { notificationList ->
                    val filtered = if (type != null) {
                        notificationList.filter { it.type == type }
                    } else {
                        notificationList
                    }
                    _notifications.value = filtered
                }
        }
    }

    fun markAsRead(notificationId: Int) {
        viewModelScope.launch {
            repository.markAsRead(notificationId)
            loadUnreadCount()
        }
    }

    fun markAllAsRead() {
        viewModelScope.launch {
            repository.markAllAsRead(userId)
            loadUnreadCount()
            // Update local state
            _notifications.value = _notifications.value.map { it.copy(isRead = true) }
        }
    }

    fun addNotification(notification: Notification) {
        viewModelScope.launch {
            repository.addNotification(notification)
            loadNotifications()
            loadUnreadCount()
        }
    }

    fun addSampleNotifications() {
        viewModelScope.launch {
            // Check if notifications already exist for this user
            val existingCount = _notifications.value.size
            if (existingCount == 0) {
                val sampleNotifications = listOf(
                    Notification(
                        title = "New Dispatch Assigned",
                        message = "Emergency AC repair at Apex Tech Plaza (Suite 401) is assigned.",
                        type = NotificationType.JOB,
                        timestamp = System.currentTimeMillis() - 10 * 60 * 1000, // 10 mins ago
                        userId = userId
                    ),
                    Notification(
                        title = "Quote Approved",
                        message = "Client approved quote #2910 for Oakwood Medical Center ($1,280.00).",
                        type = NotificationType.QUOTE,
                        timestamp = System.currentTimeMillis() - 60 * 60 * 1000, // 1 hour ago
                        userId = userId
                    ),
                    Notification(
                        title = "System Maintenance Scheduled",
                        message = "System database backup will occur at 11:00 PM tonight.",
                        type = NotificationType.SYSTEM,
                        timestamp = System.currentTimeMillis() - 3 * 60 * 60 * 1000, // 3 hours ago
                        userId = userId
                    ),
                    Notification(
                        title = "Job Card Submitted Successfully",
                        message = "Your job card #JC-1002 has been received and verified.",
                        type = NotificationType.JOB,
                        timestamp = System.currentTimeMillis() - 24 * 60 * 60 * 1000, // 1 day ago
                        userId = userId
                    )
                )
                repository.addNotifications(sampleNotifications)
                loadNotifications()
                loadUnreadCount()
            }
        }
    }

    companion object {
        fun Factory(context: Context, userId: String): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(NotificationViewModel::class.java)) {
                        val database = ArcticFlowDatabase.getDatabase(context)
                        val repository = NotificationRepository(database.notificationDao())
                        return NotificationViewModel(repository, userId) as T
                    }
                    throw IllegalArgumentException("Unknown ViewModel class")
                }
            }
        }
    }
}