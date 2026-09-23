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
import com.insy7315.advancedairconapp.BuildConfig
import com.insy7315.advancedairconapp.data.ArcticFlowDatabase
import com.insy7315.advancedairconapp.data.api.ApiClient
import com.insy7315.advancedairconapp.data.api.ApiRepository
import com.insy7315.advancedairconapp.data.api.UserSyncRequest
import com.insy7315.advancedairconapp.data.api.safeApiCall
import com.insy7315.advancedairconapp.data.entities.User
import com.insy7315.advancedairconapp.data.entities.UserRole
import com.insy7315.advancedairconapp.data.network.NetworkMonitor
import com.insy7315.advancedairconapp.services.TechLocationService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

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
            .requestIdToken(
                application.getString(
                    com.insy7315.advancedairconapp.R.string.default_web_client_id
                )
            )
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(application, gso)

        val currentUser = auth.currentUser
        if (currentUser != null) {
            handleFirebaseUser(currentUser)
        } else {
            _authState.value = AuthState.Unauthenticated
        }

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
        fallbackEmail: String?,
        fallbackRole: UserRole?
    ): Pair<String, UserRole?> {
        val raw = rawName ?: ""
        return if (raw.contains("|")) {
            val parts = raw.split("|", limit = 2)
            val name = parts[0].trim().ifBlank {
                fallbackEmail?.substringBefore("@") ?: "User"
            }
            val role = try {
                UserRole.valueOf(parts[1].trim().uppercase())
            } catch (_: Exception) {
                fallbackRole
            }
            name to role
        } else {
            val name = raw.trim().ifBlank {
                fallbackEmail?.substringBefore("@") ?: "User"
            }
            name to fallbackRole
        }
    }

    private fun handleFirebaseUser(firebaseUser: FirebaseUser) {
        viewModelScope.launch {
            try {
                val existing = database.userDao().getUserById(firebaseUser.uid)

                val (displayName, parsedRole) = parseFirebaseNameAndRole(
                    rawName = firebaseUser.displayName,
                    fallbackEmail = firebaseUser.email,
                    fallbackRole = null
                )

                val placeholderRole = existing?.role ?: parsedRole ?: UserRole.TECHNICIAN

                val placeholder = User(
                    uid = firebaseUser.uid,
                    email = firebaseUser.email ?: existing?.email ?: "",
                    displayName = displayName,
                    role = placeholderRole,
                    phoneNumber = existing?.phoneNumber,
                    photoUrl = firebaseUser.photoUrl?.toString() ?: existing?.photoUrl,
                    isEmailVerified = firebaseUser.isEmailVerified,
                    createdAt = existing?.createdAt ?: System.currentTimeMillis()
                )
                database.userDao().insertUser(placeholder)

                val finalUser = syncAndResolve(firebaseUser.uid, placeholder)

                _authState.value = AuthState.Authenticated(finalUser)
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
            if (firebaseUser == null) {
                _isLoading.value = false
                return SignInResult(success = false, message = "Registration failed")
            }

            val profileUpdates = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                .setDisplayName("$displayName|${role.name}")
                .build()
            firebaseUser.updateProfile(profileUpdates).await()

            val user = User(
                uid = firebaseUser.uid,
                email = email,
                displayName = displayName.ifBlank { email.substringBefore("@") },
                role = role,
                isEmailVerified = firebaseUser.isEmailVerified,
                createdAt = System.currentTimeMillis()
            )
            database.userDao().insertUser(user)

            val finalUser = syncAndResolve(firebaseUser.uid, user)

            _isLoading.value = false
            _authState.value = AuthState.Authenticated(finalUser)
            SignInResult(success = true, user = finalUser)
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
            if (firebaseUser == null) {
                _isLoading.value = false
                return SignInResult(success = false, message = "Login failed")
            }

            val existing = database.userDao().getUserById(firebaseUser.uid)
            val (displayName, parsedRole) = parseFirebaseNameAndRole(
                rawName = firebaseUser.displayName,
                fallbackEmail = firebaseUser.email,
                fallbackRole = null
            )

            val seedRole = existing?.role ?: parsedRole ?: UserRole.MANAGER

            val seed = User(
                uid = firebaseUser.uid,
                email = firebaseUser.email ?: email,
                displayName = displayName,
                role = seedRole,
                phoneNumber = existing?.phoneNumber,
                photoUrl = firebaseUser.photoUrl?.toString() ?: existing?.photoUrl,
                isEmailVerified = firebaseUser.isEmailVerified,
                createdAt = existing?.createdAt ?: System.currentTimeMillis()
            )
            database.userDao().insertUser(seed)

            val finalUser = syncAndResolve(firebaseUser.uid, seed)

            _isLoading.value = false
            _authState.value = AuthState.Authenticated(finalUser)
            SignInResult(success = true, user = finalUser)
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
            if (firebaseUser == null) {
                _isLoading.value = false
                return SignInResult(success = false, message = "Google Sign-In failed")
            }

            val existing = database.userDao().getUserById(firebaseUser.uid)
            val seenBeforeOnThisDevice = existing != null

            val (displayName, parsedRole) = parseFirebaseNameAndRole(
                rawName = firebaseUser.displayName,
                fallbackEmail = firebaseUser.email,
                fallbackRole = null
            )

            val optimisticRole = existing?.role
                ?: parsedRole
                ?: UserRole.MANAGER

            Log.d(
                TAG,
                "Google sign-in: seenBefore=$seenBeforeOnThisDevice, " +
                        "optimisticRole=$optimisticRole, email=${firebaseUser.email}"
            )

            val seed = User(
                uid = firebaseUser.uid,
                email = firebaseUser.email ?: existing?.email ?: "",
                displayName = displayName,
                role = optimisticRole,
                phoneNumber = existing?.phoneNumber,
                photoUrl = firebaseUser.photoUrl?.toString() ?: existing?.photoUrl,
                isEmailVerified = firebaseUser.isEmailVerified,
                createdAt = existing?.createdAt ?: System.currentTimeMillis()
            )
            database.userDao().insertUser(seed)

            val finalUser = syncAndResolve(firebaseUser.uid, seed)

            _isLoading.value = false
            _authState.value = AuthState.Authenticated(finalUser)
            SignInResult(success = true, user = finalUser)
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
            kotlinx.coroutines.delay(200)
            val user = database.userDao().getUserById(currentUser.uid)
            SignInResult(success = true, user = user)
        } else {
            SignInResult(success = false, message = "No saved session.")
        }
    }

    suspend fun refreshApiToken(): Boolean {
        val currentUser = auth.currentUser ?: return false
        val localUser = database.userDao().getUserById(currentUser.uid) ?: return false
        syncAndResolve(currentUser.uid, localUser)
        return ApiClient.hasToken(getApplication())
    }

    private suspend fun syncAndResolve(
        uid: String,
        fallback: User
    ): User {
        val ctx = getApplication<Application>()

        if (NetworkMonitor.isOnline(ctx)) {
            val api = ApiClient.get(ctx)
            val response = safeApiCall(TAG) {
                api.syncUser(
                    UserSyncRequest(
                        uid = fallback.uid,
                        email = fallback.email,
                        displayName = fallback.displayName,
                        role = fallback.role.name,
                        phoneNumber = fallback.phoneNumber
                    )
                )
            }
            if (response != null) {
                ApiClient.saveToken(ctx, response.token)
                Log.d(
                    TAG,
                    "syncUser sent role=${fallback.role.name}, " +
                            "hasToken=${ApiClient.hasToken(ctx)}"
                )
            } else {
                Log.w(TAG, "syncUser failed (baseUrl=${BuildConfig.API_BASE_URL})")
            }
        } else {
            Log.d(TAG, "Offline — skipping syncUser; will use local role")
        }

        if (NetworkMonitor.isOnline(ctx) && ApiClient.hasToken(ctx)) {
            val api = ApiClient.get(ctx)
            val me = safeApiCall(TAG) { api.getMe() }
            if (me != null) {
                val serverRole = try {
                    UserRole.valueOf(me.role.uppercase())
                } catch (_: Exception) {
                    Log.w(TAG, "Server returned unknown role '${me.role}'")
                    null
                }
                if (serverRole != null && serverRole != fallback.role) {
                    Log.d(
                        TAG,
                        "Server corrected role: ${fallback.role} → $serverRole"
                    )
                    val corrected = fallback.copy(role = serverRole)
                    database.userDao().insertUser(corrected)
                    return corrected
                } else if (serverRole != null) {
                    Log.d(TAG, "Server confirmed role=$serverRole")
                }
            } else {
                Log.w(TAG, "/api/users/me returned nothing")
            }
        }

        database.userDao().insertUser(fallback)
        return fallback
    }

    fun signOut() {
        val ctx = getApplication<Application>()

        // 1. Stop the foreground tracking service so it can't keep pushing
        //    stale coordinates with an expired token.
        try {
            TechLocationService.stop(ctx)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to stop TechLocationService", e)
        }

        // 2. Fire-and-forget server-side cleanup while the JWT is still valid.
        val currentUser = auth.currentUser
        if (currentUser != null && ApiClient.hasToken(ctx)) {
            viewModelScope.launch {
                try {
                    ApiRepository.stopTracking(ctx, currentUser.uid)
                    Log.d(TAG, "signOut: cleared server-side tracking row")
                } catch (e: Exception) {
                    Log.w(TAG, "signOut cleanup failed", e)
                }
            }
        }

        // 3. Now sign out of Firebase + Google and clear the JWT.
        auth.signOut()
        googleSignInClient.signOut()
        ApiClient.clearToken(ctx)

        // 4. Emit unauthenticated so NavGraph re-routes to login.
        _authState.value = AuthState.Unauthenticated
    }
}