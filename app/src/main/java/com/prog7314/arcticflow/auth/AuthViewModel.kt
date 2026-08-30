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

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val auth = Firebase.auth
    private val userDao = ArcticFlowDatabase.getDatabase(application).userDao()

    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Google Sign-In Client - Make this public directly instead of using a getter
    val googleSignInClient: GoogleSignInClient by lazy {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken("991209034332-s0ss0aihnnmbr0pp215dqkvrkkkoq36d.apps.googleusercontent.com") // Replace with your web client ID
            .requestEmail()
            .build()
        GoogleSignIn.getClient(application, gso)
    }

    init {
        // Check if user is already signed in
        val currentUser = auth.currentUser
        if (currentUser != null) {
            loadUserFromFirebase(currentUser)
        } else {
            _authState.value = AuthState.Unauthenticated
        }
    }

    // Email/Password Registration
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
                // Save user to local database
                val user = User(
                    uid = firebaseUser.uid,
                    email = email,
                    displayName = displayName,
                    role = role
                )
                userDao.insertUser(user)
                _authState.value = AuthState.Authenticated(user)
                _isLoading.value = false
                SignInResult(success = true, user = user)
            } else {
                _isLoading.value = false
                SignInResult(success = false, message = "Registration failed")
            }
        } catch (e: Exception) {
            _isLoading.value = false
            _authError.value = e.message
            SignInResult(success = false, message = e.message)
        }
    }

    // Email/Password Login
    suspend fun loginWithEmail(email: String, password: String): SignInResult {
        _isLoading.value = true
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val firebaseUser = result.user
            if (firebaseUser != null) {
                val user = loadUserFromFirebase(firebaseUser)
                _authState.value = AuthState.Authenticated(user)
                _isLoading.value = false
                SignInResult(success = true, user = user)
            } else {
                _isLoading.value = false
                SignInResult(success = false, message = "Login failed")
            }
        } catch (e: Exception) {
            _isLoading.value = false
            _authError.value = e.message
            SignInResult(success = false, message = e.message)
        }
    }

    // Google Sign-In
    suspend fun signInWithGoogle(idToken: String): SignInResult {
        _isLoading.value = true
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val result = auth.signInWithCredential(credential).await()
            val firebaseUser = result.user
            if (firebaseUser != null) {
                val user = loadUserFromFirebase(firebaseUser)
                _authState.value = AuthState.Authenticated(user)
                _isLoading.value = false
                SignInResult(success = true, user = user)
            } else {
                _isLoading.value = false
                SignInResult(success = false, message = "Google Sign-In failed")
            }
        } catch (e: Exception) {
            _isLoading.value = false
            _authError.value = e.message
            SignInResult(success = false, message = e.message)
        }
    }

    // Biometric Authentication
    suspend fun authenticateWithBiometric(): Boolean {
        // This is handled by the BiometricManager
        // Return true if authentication was successful
        return true
    }

    // Load user from Firebase and save to local DB
    private fun loadUserFromFirebase(firebaseUser: FirebaseUser): User {
        // Use runBlocking to get the user from DB synchronously
        val existingUser = runBlocking { userDao.getUserById(firebaseUser.uid) }
        return if (existingUser != null) {
            // Update user info if needed
            val updatedUser = existingUser.copy(
                displayName = firebaseUser.displayName ?: existingUser.displayName,
                photoUrl = firebaseUser.photoUrl?.toString() ?: existingUser.photoUrl,
                isEmailVerified = firebaseUser.isEmailVerified
            )
            viewModelScope.launch {
                userDao.updateUser(updatedUser)
            }
            updatedUser
        } else {
            // Create new user
            val newUser = User(
                uid = firebaseUser.uid,
                email = firebaseUser.email ?: "",
                displayName = firebaseUser.displayName,
                photoUrl = firebaseUser.photoUrl?.toString(),
                isEmailVerified = firebaseUser.isEmailVerified
            )
            viewModelScope.launch {
                userDao.insertUser(newUser)
            }
            newUser
        }
    }

    // Sign out
    fun signOut() {
        auth.signOut()
        googleSignInClient.signOut()
        _authState.value = AuthState.Unauthenticated
    }

    // Clean up
    override fun onCleared() {
        super.onCleared()
        // Additional cleanup if needed
    }
}

// Helper function for blocking calls in coroutine
private fun <T> runBlocking(block: suspend () -> T): T {
    return kotlinx.coroutines.runBlocking { block() }
}