// app/src/main/java/com/prog7314/arcticflow/ui/screens/ProfileScreen.kt
package com.prog7314.arcticflow.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prog7314.arcticflow.auth.AuthState
import com.prog7314.arcticflow.auth.AuthViewModel
import com.prog7314.arcticflow.navigation.NavManager
import java.text.SimpleDateFormat
import java.util.*

// ============================================================
// BRAND TOKENS
// ============================================================
private val BabyBlue     = Color(0xFF4FA8D8)
private val BabyBlueDeep = Color(0xFF2E7BA6)
private val BabyBlueSoft = Color(0xFFE1F1FB)
private val SuccessGreen = Color(0xFF2E7D32)
private val SuccessSoft  = Color(0xFFE6F4EA)
private val ErrorRed     = Color(0xFFBA1A1A)
private val ErrorSoft    = Color(0xFFFFDAD6)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    authViewModel: AuthViewModel,
    navManager: NavManager
) {
    val authState by authViewModel.authState.collectAsStateWithLifecycle()
    val user = when (authState) {
        is AuthState.Authenticated -> (authState as AuthState.Authenticated).user
        else -> null
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile") },
                navigationIcon = {
                    IconButton(onClick = { navManager.navigateBack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { navManager.navigateToEditProfile() }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Profile")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (user != null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Spacer(Modifier.height(8.dp))

                // ===== AVATAR =====
                Surface(
                    modifier = Modifier.size(120.dp),
                    shape = CircleShape,
                    color = Color.Transparent
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(listOf(BabyBlue, BabyBlueDeep)),
                                shape = CircleShape
                            )
                    ) {
                        Text(
                            text = (user.displayName?.take(1) ?: user.email.take(1)).uppercase(),
                            style = MaterialTheme.typography.displayMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                Spacer(Modifier.height(4.dp))

                Text(
                    text = user.displayName?.takeIf { it.isNotBlank() } ?: "Unnamed User",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                // Role pill
                Surface(
                    shape = RoundedCornerShape(50),
                    color = BabyBlueSoft
                ) {
                    Text(
                        user.role.name.lowercase().replaceFirstChar { it.titlecase() },
                        style = MaterialTheme.typography.labelMedium,
                        color = BabyBlueDeep,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }

                Spacer(Modifier.height(8.dp))

                // ===== INFO CARD =====
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        ProfileDetailRow(
                            icon = Icons.Default.Person,
                            label = "Name",
                            value = user.displayName ?: "Not set"
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 12.dp),
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                        ProfileDetailRow(
                            icon = Icons.Default.Email,
                            label = "Email",
                            value = user.email
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 12.dp),
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                        ProfileDetailRow(
                            icon = Icons.Default.Badge,
                            label = "Role",
                            value = user.role.name.lowercase()
                                .replaceFirstChar { it.titlecase() }
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 12.dp),
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                        ProfileDetailRow(
                            icon = Icons.Default.CalendarToday,
                            label = "Account created",
                            value = formatDate(user.createdAt)
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 12.dp),
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                        ProfileDetailRow(
                            icon = Icons.Default.Verified,
                            label = "Email verified",
                            value = if (user.isEmailVerified) "Verified" else "Not verified",
                            valueColor = if (user.isEmailVerified) SuccessGreen else ErrorRed,
                            pillBackground = if (user.isEmailVerified) SuccessSoft else ErrorSoft
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun ProfileDetailRow(
    icon: ImageVector,
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
    pillBackground: Color? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = BabyBlueSoft,
            modifier = Modifier.size(34.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = BabyBlueDeep,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        if (pillBackground != null) {
            Surface(
                shape = RoundedCornerShape(50),
                color = pillBackground
            ) {
                Text(
                    value,
                    style = MaterialTheme.typography.labelSmall,
                    color = valueColor,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        } else {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = valueColor,
                maxLines = 1
            )
        }
    }
}

private fun formatDate(timestamp: Long): String {
    val date = Date(timestamp)
    val format = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    return format.format(date)
}