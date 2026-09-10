package com.prog7314.arcticflow.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.prog7314.arcticflow.data.ArcticFlowDatabase
import com.prog7314.arcticflow.data.entities.Notification
import com.prog7314.arcticflow.data.entities.NotificationType
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class NotificationViewModel(
    private val database: ArcticFlowDatabase,
    private val userId: String
) : ViewModel() {

    private val dao = database.notificationDao()

    val notifications: StateFlow<List<Notification>> =
        dao.getNotificationsForUser(userId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val unreadCount: StateFlow<Int> =
        dao.getUnreadNotifications(userId)
            .map { it.size }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun markAsRead(id: Int) = viewModelScope.launch { dao.markAsRead(id) }
    fun markAllAsRead() = viewModelScope.launch { dao.markAllAsRead(userId) }

    fun addSampleNotifications() {
        viewModelScope.launch {
            if (notifications.value.isEmpty()) {
                dao.insertNotification(
                    Notification(
                        title = "Welcome to ArcticFlow",
                        message = "You'll see job requests and updates here.",
                        type = NotificationType.SYSTEM,
                        userId = userId
                    )
                )
            }
        }
    }

    companion object {
        fun Factory(context: Context, userId: String): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val db = ArcticFlowDatabase.getDatabase(context)
                    return NotificationViewModel(db, userId) as T
                }
            }
    }
}