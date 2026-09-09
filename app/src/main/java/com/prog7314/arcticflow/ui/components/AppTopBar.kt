// app/src/main/java/com/prog7314/arcticflow/ui/components/AppTopBar.kt
package com.prog7314.arcticflow.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prog7314.arcticflow.navigation.NavManager
import com.prog7314.arcticflow.viewmodels.NotificationViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(
    title: String,
    navManager: NavManager,
    notificationViewModel: NotificationViewModel? = null,
    showBackButton: Boolean = false,
    actions: @Composable () -> Unit = {}
) {
    val unreadCount by notificationViewModel?.unreadCount?.collectAsStateWithLifecycle() ?: remember { mutableStateOf(0) }

    TopAppBar(
        title = { Text(title) },
        navigationIcon = {
            if (showBackButton) {
                IconButton(onClick = { navManager.navigateBack() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            }
        },
        actions = {
            // Notification Bell
            if (notificationViewModel != null) {
                BadgedBox(
                    badge = {
                        if (unreadCount > 0) {
                            Badge(
                                containerColor = Color.Red,
                                modifier = Modifier.size(18.dp)
                            ) {
                                Text(
                                    text = if (unreadCount > 99) "99+" else unreadCount.toString(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White
                                )
                            }
                        }
                    }
                ) {
                    IconButton(onClick = { navManager.navigateToNotifications() }) {
                        Icon(
                            if (unreadCount > 0) Icons.Default.Notifications else Icons.Default.NotificationsNone,
                            contentDescription = "Notifications"
                        )
                    }
                }
            }
            actions()
        }
    )
}