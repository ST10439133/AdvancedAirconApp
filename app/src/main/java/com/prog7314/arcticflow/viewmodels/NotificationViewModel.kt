// app/src/main/java/com/prog7314/arcticflow/viewmodels/NotificationViewModel.kt
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

    // CHANGED: observe the DAO's COUNT query directly instead of
    // loading the whole unread list and calling .size on it.
    val unreadCount: StateFlow<Int> =
        dao.getUnreadCount(userId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun markAsRead(id: Int) = viewModelScope.launch { dao.markAsRead(id) }
    fun markAllAsRead() = viewModelScope.launch { dao.markAllAsRead(userId) }

    /**
     * Insert the sample "Welcome" notification ONLY if the user has never
     * had one. We check the DB once instead of relying on
     * `notifications.value.isEmpty()` (which can race when the flow has
     * no collector yet).
     */
    fun addSampleNotifications() {
        viewModelScope.launch {
            val existing = dao.getNotificationsForUser(userId).first()
            val alreadyHasWelcome = existing.any { it.type == NotificationType.SYSTEM }
            if (!alreadyHasWelcome) {
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