// Android Developers. 2026. Save data on the device | Data and files. [Online]. Available at: https://developer.android.com/training/data-storage/shared-preferences [Accessed: 5 November 2026].
// Android Developers. 2026. Dynamic color | Jetpack Compose. [Online]. Available at: https://developer.android.com/develop/ui/compose/designsystems/material3#dynamic-color [Accessed: 5 November 2026].
package com.insy7315.advancedairconapp.ui.screens

import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.insy7315.advancedairconapp.auth.AuthViewModel
import com.insy7315.advancedairconapp.navigation.NavManager
import com.insy7315.advancedairconapp.ui.theme.ThemeState

// BRAND TOKENS
private val BabyBlue     = Color(0xFF1F3A5F)
private val BabyBlueDeep = Color(0xFF152A47)
private val BabyBlueSoft = Color(0xFFE8EDF3)
private val ErrorRed     = Color(0xFF8B1E20)
private val ErrorSoft    = Color(0xFFF4DADD)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    authViewModel: AuthViewModel,
    navManager: NavManager,
    onThemeChange: (ThemeState) -> Unit,
    currentTheme: ThemeState
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = { navManager.navigateBack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Profile Section
            item {
                SettingsCard(sectionTitle = "Profile") {
                    SettingsItem(
                        icon = Icons.Default.Person,
                        title = "View Profile",
                        onClick = { navManager.navigateToProfile() }
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 4.dp),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                    SettingsItem(
                        icon = Icons.Default.Edit,
                        title = "Edit Profile",
                        onClick = { navManager.navigateToEditProfile() }
                    )
                }
            }

            // Appearance Section
            item {
                SettingsCard(sectionTitle = "Appearance") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = BabyBlueSoft,
                                modifier = Modifier.size(34.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.DarkMode,
                                        contentDescription = null,
                                        tint = BabyBlueDeep,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Text("Dark Mode")
                        }
                        Switch(
                            checked = currentTheme.isDarkMode,
                            onCheckedChange = {
                                onThemeChange(currentTheme.copy(isDarkMode = it))
                            }
                        )
                    }

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = BabyBlueSoft,
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Default.Palette,
                                            contentDescription = null,
                                            tint = BabyBlueDeep,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                Text("Dynamic Colors (Android 12+)")
                            }
                            Switch(
                                checked = currentTheme.dynamicColor,
                                onCheckedChange = {
                                    onThemeChange(currentTheme.copy(dynamicColor = it))
                                }
                            )
                        }
                    }
                }
            }

            // Account Section
            item {
                SettingsCard(sectionTitle = "Account") {
                    SettingsItem(
                        icon = Icons.AutoMirrored.Filled.Logout,
                        title = "Sign Out",
                        iconTint = ErrorRed,
                        textColor = ErrorRed,
                        backgroundTint = ErrorSoft,
                        onClick = {
                            authViewModel.signOut()
                            navManager.navigateToLogin()
                        }
                    )
                }
            }
        }
    }
}

// Section card
@Composable
private fun SettingsCard(
    sectionTitle: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = sectionTitle.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = BabyBlueDeep,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(6.dp))
            content()
        }
    }
}

// Settings row
@Composable
fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    iconTint: Color = BabyBlueDeep,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    backgroundTint: Color = BabyBlueSoft,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = backgroundTint,
                modifier = Modifier.size(34.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(
                    text = title,
                    color = textColor,
                    style = MaterialTheme.typography.bodyLarge
                )
                subtitle?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
        Icon(
            Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
    }
}