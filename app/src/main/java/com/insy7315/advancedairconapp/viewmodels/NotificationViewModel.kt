// app/src/main/java/com/insy7315/advancedairconapp/viewmodels/NotificationViewModel.kt
//Google for Developers. 2026. StateFlow and SharedFlow | Android Developers. [Online]. Available at: https://developer.android.com/kotlin/flow/stateflow-and-sharedflow [Accessed: 5 October 2026].
//Google for Developers. 2026. ViewModel overview | Android Developers. [Online]. Available at: https://developer.android.com/topic/libraries/architecture/viewmodel [Accessed: 5 October 2026].


package com.insy7315.advancedairconapp.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.insy7315.advancedairconapp.data.ArcticFlowDatabase
import com.insy7315.advancedairconapp.data.entities.Notification
import com.insy7315.advancedairconapp.data.entities.NotificationType
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

    // loading the whole unread list and calling .size on it.
    val unreadCount: StateFlow<Int> =
        dao.getUnreadCount(userId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun markAsRead(id: Int) = viewModelScope.launch { dao.markAsRead(id) }
    fun markAllAsRead() = viewModelScope.launch { dao.markAllAsRead(userId) }

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