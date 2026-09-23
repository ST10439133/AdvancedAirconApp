// app/src/main/java/com/insy7315/advancedairconapp/auth/AuthViewModel.kt
package com.insy7315.advancedairconapp.auth

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.insy7315.advancedairconapp.data.ArcticFlowDatabase
import com.insy7315.advancedairconapp.data.entities.User
import com.insy7315.advancedairconapp.data.entities.UserRole
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import com.insy7315.advancedairconapp.data.api.ApiClient
import com.insy7315.advancedairconapp.data.api.UserSyncRequest
import com.insy7315.advancedairconapp.data.api.safeApiCall
import com.insy7315.advancedairconapp.data.network.NetworkMonitor

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val TAG = "AuthViewModel"

    private val auth = Firebase.auth
    private val database = ArcticFlowDatabase.getDatabase(application)

    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    val googleSignInClient: GoogleSignInClient

    init {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(application.getString(com.insy7315.advancedairconapp.R.string.default_web_client_id))
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

    private fun parseFirebaseNameAndRole(
        rawName: String?,
        fallbackEmail: String?
    ): Pair<String, UserRole> {
        val raw = rawName ?: ""
        return if (raw.contains("|")) {
            val parts = raw.split("|", limit = 2)
            val name = parts[0].trim().ifBlank {
                fallbackEmail?.substringBefore("@") ?: "User"
            }
            val role = try {
                UserRole.valueOf(parts[1].trim())
            } catch (_: Exception) {
                UserRole.TECHNICIAN
            }
            name to role
        } else {
            val name = raw.trim().ifBlank {
                fallbackEmail?.substringBefore("@") ?: "User"
            }
            name to UserRole.TECHNICIAN
        }
    }

    private fun handleFirebaseUser(firebaseUser: FirebaseUser) {
        viewModelScope.launch {
            try {
                Log.d(
                    TAG,
                    "handleFirebaseUser: uid=${firebaseUser.uid}, " +
                            "displayName='${firebaseUser.displayName}', " +
                            "email='${firebaseUser.email}'"
                )

                val (displayName, parsedRole) = parseFirebaseNameAndRole(
                    rawName = firebaseUser.displayName,
                    fallbackEmail = firebaseUser.email
                )

                val existing = database.userDao().getUserById(firebaseUser.uid)

                // FIX: trust Room's saved role over Firebase parsing.
                val user = User(
                    uid = firebaseUser.uid,
                    email = firebaseUser.email ?: existing?.email ?: "",
                    displayName = displayName,
                    role = existing?.role ?: parsedRole,
                    phoneNumber = existing?.phoneNumber,
                    photoUrl = firebaseUser.photoUrl?.toString() ?: existing?.photoUrl,
                    isEmailVerified = firebaseUser.isEmailVerified,
                    createdAt = existing?.createdAt ?: System.currentTimeMillis()
                )

                database.userDao().insertUser(user)
                Log.d(TAG, "Upserted Room user: $user")

                _authState.value = AuthState.Authenticated(user)
                syncUserToApi(user)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to handle Firebase user", e)
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
                val profileUpdates = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                    .setDisplayName("$displayName|${role.name}")
                    .build()
                firebaseUser.updateProfile(profileUpdates).await()

                val user = User(
                    uid = firebaseUser.uid,
                    email = email,
                    displayName = displayName.ifBlank {
                        email.substringBefore("@")
                    },
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
                val (displayName, parsedRole) = parseFirebaseNameAndRole(
                    rawName = firebaseUser.displayName,
                    fallbackEmail = firebaseUser.email
                )

                val existing = database.userDao().getUserById(firebaseUser.uid)

                // FIX: trust Room's saved role over Firebase parsing.
                val user = User(
                    uid = firebaseUser.uid,
                    email = firebaseUser.email ?: email,
                    displayName = displayName,
                    role = existing?.role ?: parsedRole,
                    phoneNumber = existing?.phoneNumber,
                    photoUrl = firebaseUser.photoUrl?.toString() ?: existing?.photoUrl,
                    isEmailVerified = firebaseUser.isEmailVerified,
                    createdAt = existing?.createdAt ?: System.currentTimeMillis()
                )
                database.userDao().insertUser(user)

                _isLoading.value = false
                _authState.value = AuthState.Authenticated(user)
                syncUserToApi(user)
                SignInResult(success = true, user = user)
            } else {
                _isLoading.value = false
                SignInResult(success = false, message = "Login failed")
            }
        } catch (e: Exception) {
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
                val existing = database.userDao().getUserById(firebaseUser.uid)
                val (displayName, parsedRole) = parseFirebaseNameAndRole(
                    rawName = firebaseUser.displayName,
                    fallbackEmail = firebaseUser.email
                )

                val user = User(
                    uid = firebaseUser.uid,
                    email = firebaseUser.email ?: existing?.email ?: "",
                    displayName = displayName,
                    role = existing?.role ?: parsedRole,
                    phoneNumber = existing?.phoneNumber,
                    photoUrl = firebaseUser.photoUrl?.toString() ?: existing?.photoUrl,
                    isEmailVerified = firebaseUser.isEmailVerified,
                    createdAt = existing?.createdAt ?: System.currentTimeMillis()
                )
                database.userDao().insertUser(user)

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

    private suspend fun syncUserToApi(user: User) {
        val ctx = getApplication<Application>()

        if (!NetworkMonitor.isOnline(ctx)) {
            Log.d(TAG, "Offline — skipping user sync")
            return
        }

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