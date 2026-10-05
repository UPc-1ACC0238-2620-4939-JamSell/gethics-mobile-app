package com.jamsell.gethics.iam.data

data class ForgotPasswordRequest(
    val email: String
)

data class ResetPasswordRequest(
    val email: String,
    val code: String,
    val newPassword: String
)

data class AuthResponse(
    val message: String,
    val isSuccess: Boolean
)