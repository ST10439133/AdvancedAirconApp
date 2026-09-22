package com.insy7315.advancedaircornapp.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.insy7315.advancedaircornapp.data.network.NetworkMonitor
import kotlinx.coroutines.flow.collectLatest

@Composable
fun OfflineBanner(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var isOffline by remember { mutableStateOf(!NetworkMonitor.isOnline(context)) }

    LaunchedEffect(Unit) {
        NetworkMonitor.observe(context).collectLatest { online -> isOffline = !online }
    }

    AnimatedVisibility(
        visible = isOffline,
        enter = slideInVertically(),
        exit = slideOutVertically(),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF9E5E00))
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.CloudOff,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(10.dp))
            Text(
                "Offline — changes will sync when you reconnect",
                color = Color.White,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}