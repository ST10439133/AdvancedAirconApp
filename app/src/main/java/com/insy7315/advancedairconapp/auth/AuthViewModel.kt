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
import com.insy7315.advancedairconapp.data.api.UserSyncRequest
import com.insy7315.advancedairconapp.data.api.safeApiCall
import com.insy7315.advancedairconapp.data.entities.User
import com.insy7315.advancedairconapp.data.entities.UserRole
import com.insy7315.advancedairconapp.data.network.NetworkMonitor
import com.insy7315.advancedairconapp.services.TechLocationService
import kotlinx.coroutines.delay
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

    private var isRegistering = false

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
                if (!isRegistering) {
                    handleFirebaseUser(user)
                }
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
        if (isRegistering) return

        viewModelScope.launch {
            try {
                val existing = database.userDao().getUserById(firebaseUser.uid)

                val (displayName, parsedRole) = parseFirebaseNameAndRole(
                    rawName = firebaseUser.displayName,
                    fallbackEmail = firebaseUser.email,
                    fallbackRole = null
                )

                // If they exist locally, trust it.
                // If they have a tag (e.g., |TECHNICIAN), use it.
                // If they are brand new (no local row, no tag), they are a MANAGER.
                val resolvedRole = existing?.role
                    ?: parsedRole
                    ?: UserRole.MANAGER

                Log.d(TAG, "handleFirebaseUser: parsedRole=$parsedRole, resolvedRole=$resolvedRole")

                val placeholder = User(
                    uid = firebaseUser.uid,
                    email = firebaseUser.email ?: existing?.email ?: "",
                    displayName = displayName,
                    role = resolvedRole,
                    phoneNumber = existing?.phoneNumber,
                    photoUrl = firebaseUser.photoUrl?.toString() ?: existing?.photoUrl,
                    isEmailVerified = firebaseUser.isEmailVerified,
                    createdAt = existing?.createdAt ?: System.currentTimeMillis()
                )
                database.userDao().insertUser(placeholder)

                val trustLocal = existing == null && parsedRole == null

                val finalUser = syncAndResolve(
                    uid = firebaseUser.uid,
                    fallback = placeholder,
                    trustLocalRole = trustLocal
                )

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
        role: UserRole = UserRole.MANAGER
    ): SignInResult {
        _isLoading.value = true
        isRegistering = true

        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser = result.user
            if (firebaseUser == null) {
                _isLoading.value = false
                isRegistering = false
                return SignInResult(success = false, message = "Registration failed")
            }

            val profileUpdates = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                .setDisplayName("$displayName|${role.name}")
                .build()
            firebaseUser.updateProfile(profileUpdates).await()
            firebaseUser.reload().await()

            val user = User(
                uid = firebaseUser.uid,
                email = email,
                displayName = displayName.ifBlank { email.substringBefore("@") },
                role = role,
                isEmailVerified = firebaseUser.isEmailVerified,
                createdAt = System.currentTimeMillis()
            )
            database.userDao().insertUser(user)

            val finalUser = syncAndResolve(
                uid = firebaseUser.uid,
                fallback = user,
                trustLocalRole = true
            )

            val resolvedRole = if (finalUser.role == role) finalUser else finalUser.copy(role = role)
            if (resolvedRole != finalUser) {
                database.userDao().insertUser(resolvedRole)
            }

            _isLoading.value = false
            isRegistering = false
            _authState.value = AuthState.Authenticated(resolvedRole)
            SignInResult(success = true, user = resolvedRole)
        } catch (e: Exception) {
            _isLoading.value = false
            isRegistering = false
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

            val seedRole = existing?.role
                ?: parsedRole
                ?: UserRole.MANAGER

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

            val trustLocal = existing == null && parsedRole == null

            val finalUser = syncAndResolve(
                uid = firebaseUser.uid,
                fallback = seed,
                trustLocalRole = trustLocal
            )

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

            val existingByEmail = if (existing == null && !firebaseUser.email.isNullOrBlank()) {
                database.userDao().getUserByEmail(firebaseUser.email!!)
            } else null

            val (displayName, parsedRole) = parseFirebaseNameAndRole(
                rawName = firebaseUser.displayName,
                fallbackEmail = firebaseUser.email,
                fallbackRole = null
            )

            // THE FIX:
            // If they are in the local DB or pre-provisioned by email, use that role.
            // If they have a Firebase Annotation (|TECHNICIAN), use that.
            // If they are brand new, default to MANAGER.
            val optimisticRole = existing?.role
                ?: existingByEmail?.role
                ?: parsedRole
                ?: UserRole.MANAGER

            Log.d(TAG, "Google sign-in: parsedRole=$parsedRole, optimisticRole=$optimisticRole")

            val seed = User(
                uid = firebaseUser.uid,
                email = firebaseUser.email ?: existing?.email ?: "",
                displayName = displayName,
                role = optimisticRole,
                phoneNumber = existing?.phoneNumber ?: existingByEmail?.phoneNumber,
                photoUrl = firebaseUser.photoUrl?.toString() ?: existing?.photoUrl,
                isEmailVerified = firebaseUser.isEmailVerified,
                createdAt = existing?.createdAt ?: existingByEmail?.createdAt ?: System.currentTimeMillis()
            )
            database.userDao().insertUser(seed)

            // Trust local ONLY if we manually assigned MANAGER to a brand new user.
            // If they had a |TECHNICIAN annotation, we trust that too.
            val trustLocal = (existing == null && existingByEmail == null) || (existingByEmail != null)

            val finalUser = syncAndResolve(
                uid = firebaseUser.uid,
                fallback = seed,
                trustLocalRole = trustLocal
            )

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
            delay(200)
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
        fallback: User,
        trustLocalRole: Boolean = false
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
            }
        }

        if (NetworkMonitor.isOnline(ctx) && ApiClient.hasToken(ctx)) {
            val api = ApiClient.get(ctx)
            delay(400)
            val me = safeApiCall(TAG) { api.getMe() }
            if (me != null) {
                val serverRole = try {
                    UserRole.valueOf(me.role.uppercase())
                } catch (_: Exception) {
                    null
                }

                if (trustLocalRole) {
                    if (serverRole != null && serverRole != fallback.role) {
                        safeApiCall(TAG) {
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
                    }
                } else if (serverRole != null && serverRole != fallback.role) {
                    val corrected = fallback.copy(role = serverRole)
                    database.userDao().insertUser(corrected)
                    return corrected
                }
            }
        }

        database.userDao().insertUser(fallback)
        return fallback
    }

    fun signOut() {
        val ctx = getApplication<Application>()
        try {
            TechLocationService.stop(ctx)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to stop TechLocationService", e)
        }
        auth.signOut()
        googleSignInClient.signOut()
        ApiClient.clearToken(ctx)
        _authState.value = AuthState.Unauthenticated
    }
}