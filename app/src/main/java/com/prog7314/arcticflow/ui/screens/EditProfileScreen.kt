package com.prog7314.arcticflow.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prog7314.arcticflow.auth.AuthState
import com.prog7314.arcticflow.auth.AuthViewModel
import com.prog7314.arcticflow.data.entities.UserRole
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    authViewModel: AuthViewModel,
    onNavigateBack: () -> Unit,
    onSaveSuccess: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val authState by authViewModel.authState.collectAsStateWithLifecycle()
    val user = when (authState) {
        is AuthState.Authenticated -> (authState as AuthState.Authenticated).user
        else -> null
    }

    var displayName by remember { mutableStateOf(user?.displayName ?: "") }
    var selectedRole by remember { mutableStateOf(user?.role ?: UserRole.TECHNICIAN) }
    var isSaving by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Profile") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            if (displayName.isNotBlank()) {
                                coroutineScope.launch {
                                    isSaving = true
                                    // TODO: Update user in Firebase and local DB
                                    Toast.makeText(context, "Profile updated!", Toast.LENGTH_SHORT).show()
                                    isSaving = false
                                    onSaveSuccess()
                                }
                            } else {
                                Toast.makeText(context, "Name cannot be empty", Toast.LENGTH_SHORT).show()
                            }
                        },
                        enabled = !isSaving
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "Save")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Avatar
            Surface(
                modifier = Modifier.size(100.dp),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Text(
                        text = displayName.take(1).uppercase(),
                        style = MaterialTheme.typography.displayLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Email (read-only)
                    OutlinedTextField(
                        value = user?.email ?: "",
                        onValueChange = {},
                        label = { Text("Email") },
                        readOnly = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Display Name
                    OutlinedTextField(
                        value = displayName,
                        onValueChange = { displayName = it },
                        label = { Text("Display Name") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isSaving
                    )

                    // Role Selection
                    Column {
                        Text(
                            text = "Role",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = selectedRole == UserRole.TECHNICIAN,
                                onClick = { selectedRole = UserRole.TECHNICIAN },
                                label = { Text("Technician") },
                                enabled = !isSaving
                            )
                            FilterChip(
                                selected = selectedRole == UserRole.MANAGER,
                                onClick = { selectedRole = UserRole.MANAGER },
                                label = { Text("Manager") },
                                enabled = !isSaving
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (isSaving) {
                CircularProgressIndicator()
            }
        }
    }
}