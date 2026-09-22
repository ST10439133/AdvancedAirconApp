// app/src/main/java/com/insy7315/advancedaircornapp/ui/screens/SettingsScreen.kt
package com.insy7315.advancedairconapp.ui.screens

import android.os.Build
import androidx.activity.ComponentActivity
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
import com.insy7315.advancedairconapp.utils.LocaleManager
import com.insy7315.advancedairconapp.utils.rememberTranslation
import kotlinx.coroutines.launch

// BRAND TOKENS
private val BabyBlue     = Color(0xFF4FA8D8)
private val BabyBlueDeep = Color(0xFF2E7BA6)
private val BabyBlueSoft = Color(0xFFE1F1FB)
private val ErrorRed     = Color(0xFFBA1A1A)
private val ErrorSoft    = Color(0xFFFFDAD6)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    authViewModel: AuthViewModel,
    navManager: NavManager,
    onThemeChange: (ThemeState) -> Unit,
    currentTheme: ThemeState
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val localeManager = remember { LocaleManager(context) }
    val t = rememberTranslation()

    val currentLocale = localeManager.getCurrentLocale()
    val currentDisplayName = getLanguageDisplayName(currentLocale.language)
    var showLanguageDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(t("settings")) },
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
                SettingsCard(sectionTitle = t("profile")) {
                    SettingsItem(
                        icon = Icons.Default.Person,
                        title = t("view_profile"),
                        onClick = { navManager.navigateToProfile() }
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 4.dp),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                    SettingsItem(
                        icon = Icons.Default.Edit,
                        title = t("edit_profile"),
                        onClick = { navManager.navigateToEditProfile() }
                    )
                }
            }

            // Appearance Section
            item {
                SettingsCard(sectionTitle = t("appearance")) {
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
                            Text(t("dark_mode"))
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
                                Text(t("dynamic_colors"))
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

            // Language Section
            item {
                SettingsCard(sectionTitle = t("language")) {
                    SettingsItem(
                        icon = Icons.Default.Language,
                        title = t("change_language"),
                        subtitle = currentDisplayName,
                        onClick = { showLanguageDialog = true }
                    )
                }
            }

            // Account Section
            item {
                SettingsCard(sectionTitle = t("account")) {
                    SettingsItem(
                        icon = Icons.AutoMirrored.Filled.Logout,
                        title = t("sign_out"),
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

    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            shape = RoundedCornerShape(20.dp),
            title = {
                Text(
                    t("select_language"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    localeManager.getAvailableLocales().forEach { localeInfo ->
                        TextButton(
                            onClick = {
                                coroutineScope.launch {
                                    localeManager.setLocale(localeInfo.locale)
                                    showLanguageDialog = false
                                    (context as? ComponentActivity)?.recreate()
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${localeInfo.flag} ${localeInfo.displayName}",
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f)
                                )
                                if (currentLocale.language == localeInfo.locale.language) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = BabyBlueDeep
                                    )
                                }
                            }
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLanguageDialog = false }) {
                    Text(
                        t("cancel"),
                        fontWeight = FontWeight.SemiBold,
                        color = BabyBlueDeep
                    )
                }
            }
        )
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

private fun getLanguageDisplayName(languageCode: String): String {
    return when (languageCode) {
        "en" -> "English"
        "af" -> "Afrikaans"
        "zu" -> "isiZulu"
        else -> "Unknown"
    }
}