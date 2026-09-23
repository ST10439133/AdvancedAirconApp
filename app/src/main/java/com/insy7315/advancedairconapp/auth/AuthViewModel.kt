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

    /**
     * Fired automatically when Firebase reports a signed-in user on app start
     * or after any auth event. The role is finalized via /sync + /me before
     * AuthState.Authenticated is emitted, so navigation always sees the
     * correct role.
     */
    private fun handleFirebaseUser(firebaseUser: FirebaseUser) {
        viewModelScope.launch {
            try {
                val existing = database.userDao().getUserById(firebaseUser.uid)

                val (displayName, parsedRole) = parseFirebaseNameAndRole(
                    rawName = firebaseUser.displayName,
                    fallbackEmail = firebaseUser.email,
                    fallbackRole = null
                )

                // Placeholder role used until /me gives us the truth.
                // If we have nothing at all, TECHNICIAN is the safe placeholder
                // (it will be corrected before navigation completes).
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

            // Tag displayName with "Name|ROLE" so every device can resolve
            // the role without a network round-trip on future logins.
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

    /**
     * Google sign-in.
     *
     * Rules:
     *   - Brand-new Google user → MANAGER (client default for Google sign-ups).
     *   - Returning user (seeded technician, or previously-registered manager)
     *     → whatever the server already has. `/me` corrects us if needed.
     *
     * Authenticated is emitted only after /sync and /me complete, so
     * navigation always resolves against the final role.
     */
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

            // Optimistic role:
            //   1. Already known locally → keep it.
            //   2. Encoded "Name|ROLE" → use it.
            //   3. Otherwise → MANAGER (the app's default for Google sign-ups).
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

    /**
     * Pushes the user to /api/users/sync, then fetches /api/users/me and
     * returns the authoritative Room user (with the server's role).
     *
     * The Room row is always updated with whatever the server says.
     */
    private suspend fun syncAndResolve(
        uid: String,
        fallback: User
    ): User {
        val ctx = getApplication<Application>()

        // ---- 1. Push to /api/users/sync ----
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

        // ---- 2. Fetch /api/users/me to learn the authoritative role ----
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

        // No correction needed — persist and return what we have.
        database.userDao().insertUser(fallback)
        return fallback
    }

    fun signOut() {
        auth.signOut()
        googleSignInClient.signOut()
        ApiClient.clearToken(getApplication())
        _authState.value = AuthState.Unauthenticated
    }
}