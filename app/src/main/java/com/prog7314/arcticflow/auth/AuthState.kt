// app/src/main/java/com/prog7314/arcticflow/auth/AuthState.kt
package com.prog7314.arcticflow.auth

import com.prog7314.arcticflow.data.entities.User

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
