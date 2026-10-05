package com.jamsell.gethics.iam.data

/** Body de LoginResource (POST auth/login). El backend exige ambos campos no vacios. */
data class SignInRequest(
    val email: String,
    val password: String
)

/** AuthenticatedUserResource. */
data class AuthenticatedUserResponse(
    val token: String,
    val refreshToken: String,
    val tokenType: String,    // "Bearer"
    val expiresIn: Long,      // segundos
    val user: UserResponse
)

/** UserResource. role: "GANADERO" | "VETERINARIO". */
data class UserResponse(
    val id: Long,
    val name: String,
    val email: String,
    val role: String,
    val phone: String?,
    val photoUrl: String?
)
