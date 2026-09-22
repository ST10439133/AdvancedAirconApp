// app/src/main/java/com/insy7315/advancedairconapp/auth/AuthState.kt
package com.insy7315.advancedairconapp.auth

import com.insy7315.advancedairconapp.data.entities.User

sealed class AuthState {
    object Unauthenticated : AuthState()
    object Loading : AuthState()
    data class Authenticated(val user: User) : AuthState()
    data class Error(val message: String) : AuthState()
}

data class SignInResult(
    val success: Boolean,
    val user: User? = null,
    val message: String? = null
)
