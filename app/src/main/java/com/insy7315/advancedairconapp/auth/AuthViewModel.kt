// IEEE Xplore. 2026. A Comparative Study of User Experience in Mobile Banking Authentication Mechanism: Usability Analysis Using Usability Testing and SUS. [Online]. Available at: https://ieeexplore.ieee.org/abstract/document/11323707 [Accessed: 5 November 2026].
// IEEE Xplore. 2025. Implementation of Biometric Technology in Indonesian Mobile Banking: A TAM Perspective on Enhancing Transaction Security and Enjoyment. [Online]. Available at: https://ieeexplore.ieee.org/abstract/document/11005053 [Accessed: 5 November 2026].
// Android Developers. 2026. androidx.biometric | API Reference. [Online]. Available at: https://developer.android.com/reference/androidx/biometric/package-summary [Accessed: 3 November 2026].
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


    // ROLE PARSING


    /**
     * Firebase stores the display name as "Real Name|ROLE" once the user
     * has been provisioned. Falls back to a safe default when no
     * annotation is present.
     */
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
     * Returns the user's locally-stored role, if any, from either the UID
     * row (fast path) or a pre-provisioned email row (e.g. an admin
     * created the technician before their first sign-in).
     *
     * Returns null when we have no *deliberate* role for this user.
     */
    private suspend fun lookupLocalRole(
        uid: String,
        email: String?
    ): Pair<User?, User?> {
        val byUid = database.userDao().getUserById(uid)
        val byEmail = if (byUid == null && !email.isNullOrBlank()) {
            database.userDao().getUserByEmail(email)
        } else null
        return byUid to byEmail
    }


    // AUTH STATE HANDLING


    /**
     * Resolves a signed-in Firebase user. This is the single source of
     * truth for what role the app should show.
     *
     * Resolution order (highest priority first):
     *   1. Local row by UID (established by a previous successful login
     *      or registerWithEmail).
     *   2. Pre-provisioned row by email (admin-created technician).
     *   3. Firebase displayName |ROLE annotation.
     *   4. Server response from GET /api/users/me (canonical).
     *   5. Safe fallback = TECHNICIAN — never MANAGER by accident.
     */
    private fun handleFirebaseUser(firebaseUser: FirebaseUser) {
        if (isRegistering) return

        viewModelScope.launch {
            try {
                val (existing, existingByEmail) = lookupLocalRole(
                    firebaseUser.uid,
                    firebaseUser.email
                )

                val (displayName, parsedRole) = parseFirebaseNameAndRole(
                    rawName = firebaseUser.displayName,
                    fallbackEmail = firebaseUser.email,
                    fallbackRole = null
                )

                // A role is "trusted" only if it came from a deliberate source.
                // A brand-new Firebase user with no local row and no annotation
                // is NOT trusted — we must ask the server.
                val trustedRole: UserRole? = existing?.role
                    ?: existingByEmail?.role
                    ?: parsedRole
                val trustLocal = trustedRole != null

                val seedRole = trustedRole ?: UserRole.TECHNICIAN // neutral default

                Log.d(
                    TAG,
                    "handleFirebaseUser: uid=${firebaseUser.uid}, " +
                            "existing=${existing?.role}, " +
                            "byEmail=${existingByEmail?.role}, " +
                            "parsed=${parsedRole}, " +
                            "trustLocal=$trustLocal, seedRole=$seedRole"
                )

                val seed = User(
                    uid = firebaseUser.uid,
                    email = firebaseUser.email ?: existing?.email ?: "",
                    displayName = displayName,
                    role = seedRole,
                    phoneNumber = existing?.phoneNumber ?: existingByEmail?.phoneNumber,
                    photoUrl = firebaseUser.photoUrl?.toString() ?: existing?.photoUrl,
                    isEmailVerified = firebaseUser.isEmailVerified,
                    createdAt = existing?.createdAt
                        ?: existingByEmail?.createdAt
                        ?: System.currentTimeMillis()
                )
                database.userDao().insertUser(seed)

                val finalUser = syncAndResolve(
                    uid = firebaseUser.uid,
                    fallback = seed,
                    trustLocalRole = trustLocal
                )

                assertNotAccidentalManagerPromotion(
                    finalUser = finalUser,
                    existing = existing,
                    existingByEmail = existingByEmail,
                    parsedRole = parsedRole
                )

                _authState.value = AuthState.Authenticated(finalUser)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to handle Firebase user", e)
                _authState.value = AuthState.Error("Failed to load user data: ${e.message}")
            }
        }
    }


    // REGISTRATION (unchanged semantics: user picks their role)


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

            // Stamp the role into the Firebase display name so it survives
            // any local wipe and is visible on other devices.
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

            // This is a deliberate role — trust it and push it to the server.
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


    // EMAIL / PASSWORD LOGIN

    suspend fun loginWithEmail(email: String, password: String): SignInResult {
        _isLoading.value = true
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val firebaseUser = result.user
            if (firebaseUser == null) {
                _isLoading.value = false
                return SignInResult(success = false, message = "Login failed")
            }

            val (existing, existingByEmail) = lookupLocalRole(
                firebaseUser.uid,
                firebaseUser.email
            )

            val (displayName, parsedRole) = parseFirebaseNameAndRole(
                rawName = firebaseUser.displayName,
                fallbackEmail = firebaseUser.email,
                fallbackRole = null
            )

            // Trust ONLY a deliberate role. A brand-new user with no local
            // row and no |ROLE annotation must be resolved by the server.
            val trustedRole: UserRole? = existing?.role
                ?: existingByEmail?.role
                ?: parsedRole
            val trustLocal = trustedRole != null

            val seedRole = trustedRole ?: UserRole.TECHNICIAN

            Log.d(
                TAG,
                "loginWithEmail: uid=${firebaseUser.uid}, " +
                        "existing=${existing?.role}, " +
                        "byEmail=${existingByEmail?.role}, " +
                        "parsed=${parsedRole}, " +
                        "trustLocal=$trustLocal, seedRole=$seedRole"
            )

            val seed = User(
                uid = firebaseUser.uid,
                email = firebaseUser.email ?: email,
                displayName = displayName,
                role = seedRole,
                phoneNumber = existing?.phoneNumber ?: existingByEmail?.phoneNumber,
                photoUrl = firebaseUser.photoUrl?.toString() ?: existing?.photoUrl,
                isEmailVerified = firebaseUser.isEmailVerified,
                createdAt = existing?.createdAt
                    ?: existingByEmail?.createdAt
                    ?: System.currentTimeMillis()
            )
            database.userDao().insertUser(seed)

            val finalUser = syncAndResolve(
                uid = firebaseUser.uid,
                fallback = seed,
                trustLocalRole = trustLocal
            )

            assertNotAccidentalManagerPromotion(
                finalUser = finalUser,
                existing = existing,
                existingByEmail = existingByEmail,
                parsedRole = parsedRole
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

    // ============================================================
    // GOOGLE SIGN-IN
    // ============================================================

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

            val (existing, existingByEmail) = lookupLocalRole(
                firebaseUser.uid,
                firebaseUser.email
            )

            val (displayName, parsedRole) = parseFirebaseNameAndRole(
                rawName = firebaseUser.displayName,
                fallbackEmail = firebaseUser.email,
                fallbackRole = null
            )

            val trustedRole: UserRole? = existing?.role
                ?: existingByEmail?.role
                ?: parsedRole
            val trustLocal = trustedRole != null

            val seedRole = trustedRole ?: UserRole.TECHNICIAN

            Log.d(
                TAG,
                "signInWithGoogle: uid=${firebaseUser.uid}, " +
                        "existing=${existing?.role}, " +
                        "byEmail=${existingByEmail?.role}, " +
                        "parsed=${parsedRole}, " +
                        "trustLocal=$trustLocal, seedRole=$seedRole"
            )

            val seed = User(
                uid = firebaseUser.uid,
                email = firebaseUser.email ?: existing?.email ?: "",
                displayName = displayName,
                role = seedRole,
                phoneNumber = existing?.phoneNumber ?: existingByEmail?.phoneNumber,
                photoUrl = firebaseUser.photoUrl?.toString() ?: existing?.photoUrl,
                isEmailVerified = firebaseUser.isEmailVerified,
                createdAt = existing?.createdAt
                    ?: existingByEmail?.createdAt
                    ?: System.currentTimeMillis()
            )
            database.userDao().insertUser(seed)

            val finalUser = syncAndResolve(
                uid = firebaseUser.uid,
                fallback = seed,
                trustLocalRole = trustLocal
            )

            assertNotAccidentalManagerPromotion(
                finalUser = finalUser,
                existing = existing,
                existingByEmail = existingByEmail,
                parsedRole = parsedRole
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


    // PASSWORD RESET

    suspend fun resetPassword(email: String): Boolean {
        return try {
            auth.sendPasswordResetEmail(email).await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // ============================================================
    // BIOMETRIC
    // ============================================================

    suspend fun signInWithBiometric(): SignInResult {
        val currentUser = auth.currentUser
            ?: return SignInResult(success = false, message = "No saved session.")

        return try {
            // Reuse the same resolution path as any other sign-in so role
            // correctness is guaranteed.
            val (existing, existingByEmail) = lookupLocalRole(
                currentUser.uid,
                currentUser.email
            )
            val (displayName, parsedRole) = parseFirebaseNameAndRole(
                rawName = currentUser.displayName,
                fallbackEmail = currentUser.email,
                fallbackRole = null
            )
            val trustedRole: UserRole? = existing?.role
                ?: existingByEmail?.role
                ?: parsedRole
            val trustLocal = trustedRole != null
            val seedRole = trustedRole ?: UserRole.TECHNICIAN

            val seed = User(
                uid = currentUser.uid,
                email = currentUser.email ?: existing?.email ?: "",
                displayName = displayName,
                role = seedRole,
                phoneNumber = existing?.phoneNumber ?: existingByEmail?.phoneNumber,
                photoUrl = currentUser.photoUrl?.toString() ?: existing?.photoUrl,
                isEmailVerified = currentUser.isEmailVerified,
                createdAt = existing?.createdAt
                    ?: existingByEmail?.createdAt
                    ?: System.currentTimeMillis()
            )
            database.userDao().insertUser(seed)

            val finalUser = syncAndResolve(
                uid = currentUser.uid,
                fallback = seed,
                trustLocalRole = trustLocal
            )

            _authState.value = AuthState.Authenticated(finalUser)
            SignInResult(success = true, user = finalUser)
        } catch (e: Exception) {
            Log.e(TAG, "signInWithBiometric failed", e)
            SignInResult(success = false, message = e.message)
        }
    }

    // ============================================================
    // TOKEN REFRESH
    // ============================================================

    suspend fun refreshApiToken(): Boolean {
        val currentUser = auth.currentUser ?: return false
        val localUser = database.userDao().getUserById(currentUser.uid) ?: return false
        syncAndResolve(currentUser.uid, localUser, trustLocalRole = true)
        return ApiClient.hasToken(getApplication())
    }

    // ============================================================
    // CORE SYNC + ROLE RESOLUTION
    // ============================================================

    /**
     * Resolves the user against the server and returns the canonical User.
     *
     * Rules:
     *   - If we trust a local role (deliberate), we push it up when it
     *     differs from the server — this is the register flow.
     *   - If we do NOT trust a local role, the server wins. If the server
     *     has no opinion, we default to TECHNICIAN (never MANAGER).
     *   - If we're offline and have no trusted local role, we default to
     *     TECHNICIAN rather than guessing MANAGER.
     */
    private suspend fun syncAndResolve(
        uid: String,
        fallback: User,
        trustLocalRole: Boolean = false
    ): User {
        val ctx = getApplication<Application>()

        // ---- Step 1: obtain JWT (this is where the server learns new users) ----
        if (NetworkMonitor.isOnline(ctx)) {
            val api = ApiClient.get(ctx)
            val response = safeApiCall(TAG) {
                api.syncUser(
                    UserSyncRequest(
                        uid = fallback.uid,
                        email = fallback.email,
                        displayName = fallback.displayName,
                        // If we don't trust the role, don't invent one.
                        // Send TECHNICIAN as a neutral placeholder; the
                        // server overrides it on /me.
                        role = if (trustLocalRole) fallback.role.name else "TECHNICIAN",
                        phoneNumber = fallback.phoneNumber
                    )
                )
            }
            if (response != null) {
                ApiClient.saveToken(ctx, response.token)
            } else if (!trustLocalRole) {
                // Could not acquire JWT AND we have no trusted local role.
                // Do NOT silently promote to MANAGER.
                Log.w(TAG, "syncAndResolve: no JWT and no trusted local role")
                return fallback.copy(role = UserRole.TECHNICIAN)
            }
        } else if (!trustLocalRole) {
            Log.w(TAG, "syncAndResolve: offline and no trusted local role")
            return fallback.copy(role = UserRole.TECHNICIAN)
        }

        // ---- Step 2: ask the server who this user actually is ----
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
                    // Deliberate local role wins — push it if the server disagrees.
                    if (serverRole != null && serverRole != fallback.role) {
                        Log.d(
                            TAG,
                            "syncAndResolve: pushing local role " +
                                    "${fallback.role} over server $serverRole"
                        )
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
                    // Server is authoritative — adopt its role.
                    Log.d(
                        TAG,
                        "syncAndResolve: adopting server role $serverRole " +
                                "(was ${fallback.role})"
                    )
                    val corrected = fallback.copy(role = serverRole)
                    database.userDao().insertUser(corrected)
                    return corrected
                }
            }
        }

        database.userDao().insertUser(fallback)
        return fallback
    }

    // ============================================================
    // DEBUG GUARD
    // ============================================================

    /**
     * Catches accidental MANAGER promotion in debug builds.
     * If a user ends up as MANAGER without a *deliberate* source, log a
     * loud warning so it shows up in Logcat.
     */
    private fun assertNotAccidentalManagerPromotion(
        finalUser: User,
        existing: User?,
        existingByEmail: User?,
        parsedRole: UserRole?
    ) {
        if (!BuildConfig.DEBUG) return
        if (finalUser.role != UserRole.MANAGER) return

        val deliberateSource = existing?.role == UserRole.MANAGER ||
                existingByEmail?.role == UserRole.MANAGER ||
                parsedRole == UserRole.MANAGER

        if (!deliberateSource) {
            Log.e(
                TAG,
                "SUSPICIOUS MANAGER PROMOTION for ${finalUser.email}: " +
                        "existing=${existing?.role}, " +
                        "byEmail=${existingByEmail?.role}, " +
                        "parsed=$parsedRole. " +
                        "This means the server returned MANAGER — verify " +
                        "the backend's user record."
            )
        }
    }

    // ============================================================
    // SIGN OUT
    // ============================================================

    fun signOut() {
        val ctx = getApplication<Application>()
        try {
            TechLocationService.stop(ctx)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to stop TechLocationService", e)
        }

        // Clear the local user row so the next sign-in re-resolves the
        // role from the server. This prevents stale roles from sticking
        // around after a backend correction.
        val uid = auth.currentUser?.uid
        viewModelScope.launch {
            if (uid != null) {
                try {
                    database.userDao().getUserById(uid)?.let {
                        database.userDao().deleteUser(it)
                        Log.d(TAG, "signOut: cleared local user row for uid=$uid")
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "signOut: failed to clear local user", e)
                }
            }
        }

        auth.signOut()
        googleSignInClient.signOut()
        ApiClient.clearToken(ctx)
        _authState.value = AuthState.Unauthenticated
    }
}