// app/src/main/java/com/prog7314/arcticflow/ui/screens/NotificationScreen.kt
package com.prog7314.arcticflow.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prog7314.arcticflow.data.entities.Notification
import com.prog7314.arcticflow.data.entities.NotificationType
import com.prog7314.arcticflow.navigation.NavManager
import com.prog7314.arcticflow.viewmodels.NotificationViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationScreen(
    viewModel: NotificationViewModel,
    navManager: NavManager
) {
    val notifications by viewModel.notifications.collectAsStateWithLifecycle()
    val unreadCount by viewModel.unreadCount.collectAsStateWithLifecycle()

    // Local filter state — no longer tied to ViewModel
    var filterType by remember { mutableStateOf<NotificationType?>(null) }

    val filteredNotifications = remember(notifications, filterType) {
        if (filterType == null) notifications
        else notifications.filter { it.type == filterType }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notifications") },
                navigationIcon = {
                    IconButton(onClick = { navManager.navigateBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (unreadCount > 0) {
                        TextButton(onClick = { viewModel.markAllAsRead() }) {
                            Text("Mark All Read")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Filter tabs
            val tabs = listOf<Pair<NotificationType?, String>>(
                null to "All",
                NotificationType.JOB to "Jobs",
                NotificationType.QUOTE to "Quotes",
                NotificationType.SYSTEM to "System"
            )

            val selectedIndex = tabs.indexOfFirst { it.first == filterType }
                .coerceAtLeast(0)

            ScrollableTabRow(
                selectedTabIndex = selectedIndex,
                containerColor = MaterialTheme.colorScheme.surface,
                edgePadding = 16.dp
            ) {
                tabs.forEachIndexed { index, (type, label) ->
                    Tab(
                        selected = selectedIndex == index,
                        onClick = { filterType = type },
                        text = {
                            Text(
                                label,
                                fontWeight = if (selectedIndex == index) FontWeight.Bold
                                else FontWeight.Normal
                            )
                        }
                    )
                }
            }

            if (filteredNotifications.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.NotificationsOff,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No notifications",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "You're all caught up!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredNotifications, key = { it.id }) { notification ->
                        NotificationItem(
                            notification = notification,
                            onMarkRead = { viewModel.markAsRead(notification.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationItem(
    notification: Notification,
    onMarkRead: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onMarkRead() },
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (notification.isRead) 1.dp else 2.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = if (notification.isRead) {
                MaterialTheme.colorScheme.surface
            } else {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Type icon
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            when (notification.type) {
                                NotificationType.JOB -> Color(0xFF2196F3)
                                NotificationType.QUOTE -> Color(0xFF4CAF50)
                                NotificationType.SYSTEM -> Color(0xFFFF9800)
                            }.copy(alpha = 0.2f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        when (notification.type) {
                            NotificationType.JOB -> Icons.Default.Work
                            NotificationType.QUOTE -> Icons.Default.Receipt
                            NotificationType.SYSTEM -> Icons.Default.Settings
                        },
                        contentDescription = null,
                        tint = when (notification.type) {
                            NotificationType.JOB -> Color(0xFF2196F3)
                            NotificationType.QUOTE -> Color(0xFF4CAF50)
                            NotificationType.SYSTEM -> Color(0xFFFF9800)
                        }
                    )
                }

                Column {
                    Text(
                        text = notification.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = if (notification.isRead) FontWeight.Normal
                        else FontWeight.Bold
                    )
                    Text(
                        text = notification.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatTimestamp(notification.timestamp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }

            if (!notification.isRead) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(MaterialTheme.colorScheme.primary)
                )
            }
        }
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp

    return when {
        diff < 60_000 -> "Just now"
        diff < 60_000 * 60 -> "${diff / 60_000}m ago"
        diff < 60_000 * 60 * 24 -> "${diff / (60_000 * 60)}h ago"
        else -> {
            val format = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
            format.format(Date(timestamp))
        }
    }
}