// app/src/main/java/com/prog7314/arcticflow/auth/AuthViewModel.kt
package com.prog7314.arcticflow.auth

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.prog7314.arcticflow.data.ArcticFlowDatabase
import com.prog7314.arcticflow.data.entities.User
import com.prog7314.arcticflow.data.entities.UserRole
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import com.prog7314.arcticflow.data.api.ApiClient
import com.prog7314.arcticflow.data.api.UserSyncRequest
import com.prog7314.arcticflow.data.api.safeApiCall

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val auth = Firebase.auth
    private val database = ArcticFlowDatabase.getDatabase(application)

    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    val googleSignInClient: GoogleSignInClient

    init {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(application.getString(com.prog7314.arcticflow.R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(application, gso)

        // Check existing session
        val currentUser = auth.currentUser
        if (currentUser != null) {
            handleFirebaseUser(currentUser)
        } else {
            _authState.value = AuthState.Unauthenticated
        }

        // Listen for auth state changes
        auth.addAuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            if (user != null) {
                handleFirebaseUser(user)
            } else {
                _authState.value = AuthState.Unauthenticated
            }
        }
    }

    /**
     * Resolve the user & role.
     * Order of priority:
     * 1. Local DB by UID (source of truth)
     * 2. Firebase Auth displayName ("Name|ROLE")
     * 3. Fallback: TECHNICIAN
     */
    private fun handleFirebaseUser(firebaseUser: FirebaseUser) {
        viewModelScope.launch {
            try {
                val existingUser = database.userDao().getUserById(firebaseUser.uid)
                if (existingUser != null) {
                    _authState.value = AuthState.Authenticated(existingUser)
                    syncUserToApi(existingUser)
                    return@launch
                }

                // 2. Parse role from Firebase displayName
                val rawName = firebaseUser.displayName ?: ""
                val displayName: String
                val role: UserRole
                if (rawName.contains("|")) {
                    val parts = rawName.split("|", limit = 2)
                    displayName = parts[0].trim()
                    role = try {
                        UserRole.valueOf(parts[1].trim())
                    } catch (e: Exception) {
                        UserRole.TECHNICIAN
                    }
                } else {
                    displayName = rawName.ifBlank { firebaseUser.email ?: "User" }
                    role = UserRole.TECHNICIAN
                }

                // 3. Create a User record using resolved role
                val newUser = User(
                    uid = firebaseUser.uid,
                    email = firebaseUser.email ?: "",
                    displayName = displayName,
                    role = role,
                    isEmailVerified = firebaseUser.isEmailVerified,
                    createdAt = System.currentTimeMillis()
                )
                database.userDao().insertUser(newUser)
                _authState.value = AuthState.Authenticated(newUser)
                syncUserToApi(newUser)
            } catch (e: Exception) {
                _authState.value = AuthState.Error("Failed to load user data: ${e.message}")
            }
        }
    }

    suspend fun registerWithEmail(
        email: String,
        password: String,
        displayName: String,
        role: UserRole
    ): SignInResult {
        _isLoading.value = true
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser = result.user
            if (firebaseUser != null) {
                // ⚠️ Store role with displayName so we can rebuild on next login
                val profileUpdates = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                    .setDisplayName("$displayName|${role.name}")
                    .build()
                firebaseUser.updateProfile(profileUpdates).await()

                val user = User(
                    uid = firebaseUser.uid,
                    email = email,
                    displayName = displayName,
                    role = role,
                    isEmailVerified = firebaseUser.isEmailVerified,
                    createdAt = System.currentTimeMillis()
                )
                database.userDao().insertUser(user)

                _isLoading.value = false
                _authState.value = AuthState.Authenticated(user)
                syncUserToApi(user)
                SignInResult(success = true, user = user)
            } else {
                _isLoading.value = false
                SignInResult(success = false, message = "Registration failed")
            }
        } catch (e: Exception) {
            _isLoading.value = false
            _authState.value = AuthState.Error(e.message ?: "Registration failed")
            SignInResult(success = false, message = e.message)
        }
    }

    suspend fun loginWithEmail(email: String, password: String): SignInResult {
        _isLoading.value = true
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val firebaseUser = result.user
            if (firebaseUser != null) {
                // Look up local DB first
                var user = database.userDao().getUserById(firebaseUser.uid)

                if (user == null) {
                    // Try to rebuild from Firebase displayName format ("Name|ROLE")
                    val rawName = firebaseUser.displayName ?: ""
                    val displayName: String
                    val role: UserRole
                    if (rawName.contains("|")) {
                        val parts = rawName.split("|", limit = 2)
                        displayName = parts[0].trim()
                        role = try { UserRole.valueOf(parts[1].trim()) }
                        catch (e: Exception) { UserRole.TECHNICIAN }
                    } else {
                        displayName = rawName.ifBlank { firebaseUser.email ?: "User" }
                        role = UserRole.TECHNICIAN
                    }

                    user = User(
                        uid = firebaseUser.uid,
                        email = firebaseUser.email ?: "",
                        displayName = displayName,
                        role = role,
                        isEmailVerified = firebaseUser.isEmailVerified,
                        createdAt = System.currentTimeMillis()
                    )
                    database.userDao().insertUser(user)
                }

                _isLoading.value = false
                _authState.value = AuthState.Authenticated(user)
                syncUserToApi(user)
                SignInResult(success = true, user = user)
            } else {
                _isLoading.value = false
                SignInResult(success = false, message = "Login failed")
            }
        } catch (e: Exception) {
            // ❌ REMOVED the 3 bogus lines that referenced `user` here:
            //    _isLoading.value = false
            //    _authState.value = AuthState.Authenticated(user)   <-- error
            //    syncUserToApi(user)                                 <-- error
            //    SignInResult(success = true, user = user)           <-- error
            //
            // ✅ Correct catch behaviour: report the failure.
            _isLoading.value = false
            _authState.value = AuthState.Error(e.message ?: "Login failed")
            SignInResult(success = false, message = e.message)
        }
    }

    suspend fun signInWithGoogle(idToken: String): SignInResult {
        _isLoading.value = true
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val result = auth.signInWithCredential(credential).await()
            val firebaseUser = result.user
            if (firebaseUser != null) {
                var user = database.userDao().getUserById(firebaseUser.uid)
                if (user == null) {
                    user = User(
                        uid = firebaseUser.uid,
                        email = firebaseUser.email ?: "",
                        displayName = firebaseUser.displayName,
                        role = UserRole.TECHNICIAN,  // Google sign-in defaults to tech unless pre-registered
                        isEmailVerified = firebaseUser.isEmailVerified,
                        createdAt = System.currentTimeMillis()
                    )
                    database.userDao().insertUser(user)
                }
                _isLoading.value = false
                _authState.value = AuthState.Authenticated(user)
                syncUserToApi(user)
                SignInResult(success = true, user = user)
            } else {
                _isLoading.value = false
                SignInResult(success = false, message = "Google Sign-In failed")
            }
        } catch (e: Exception) {
            _isLoading.value = false
            _authState.value = AuthState.Error(e.message ?: "Google Sign-In failed")
            SignInResult(success = false, message = e.message)
        }
    }

    suspend fun resetPassword(email: String): Boolean {
        return try {
            auth.sendPasswordResetEmail(email).await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun signInWithBiometric(): SignInResult {
        val currentUser = auth.currentUser
        return if (currentUser != null) {
            handleFirebaseUser(currentUser)
            kotlinx.coroutines.delay(150)
            val user = database.userDao().getUserById(currentUser.uid)
            SignInResult(success = true, user = user)
        } else {
            SignInResult(success = false, message = "No saved session.")
        }
    }

    /**
     * Mirrors the local User into the REST API and stores the JWT
     * returned by POST /api/users/sync. Fails silently if the API
     * is unreachable — the app keeps working offline.
     */
    private suspend fun syncUserToApi(user: User) {
        val ctx = getApplication<Application>()
        val api = ApiClient.get(ctx)
        val response = safeApiCall("AuthViewModel") {
            api.syncUser(
                UserSyncRequest(
                    uid = user.uid,
                    email = user.email,
                    displayName = user.displayName,
                    role = user.role.name,
                    phoneNumber = user.phoneNumber
                )
            )
        }
        if (response != null) {
            ApiClient.saveToken(ctx, response.token)
        }
    }

    fun signOut() {
        auth.signOut()
        googleSignInClient.signOut()
        ApiClient.clearToken(getApplication())
        _authState.value = AuthState.Unauthenticated
    }
}