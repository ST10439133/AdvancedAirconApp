// app/src/main/java/com/insy7315/advancedaircornapp/ui/components/AppTopBar.kt
package com.insy7315.advancedairconapp.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.insy7315.advancedairconapp.navigation.NavManager
import com.insy7315.advancedairconapp.viewmodels.NotificationViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(
    title: String,
    navManager: NavManager,
    notificationViewModel: NotificationViewModel? = null,
    showBackButton: Boolean = false,
    actions: @Composable () -> Unit = {}
) {

    val unreadCount: Int = if (notificationViewModel != null) {
        notificationViewModel.unreadCount.collectAsStateWithLifecycle().value
    } else {
        0
    }

    TopAppBar(
        title = { Text(title) },
        navigationIcon = {
            if (showBackButton) {
                IconButton(onClick = { navManager.navigateToMain() }) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back to Dashboard"
                    )
                }
            }
        },
        actions = {
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
                            if (unreadCount > 0) Icons.Default.Notifications
                            else Icons.Default.NotificationsNone,
                            contentDescription = "Notifications"
                        )
                    }
                }
            }
            actions()
        }
    )
}