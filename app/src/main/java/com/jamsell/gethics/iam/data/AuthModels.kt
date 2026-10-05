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

data class UserProfileDto(
    val id: String,
    val fullName: String,
    val email: String,
    val phone: String?,
    val address: String?
)

data class UpdateProfileRequest(
    val fullName: String,
    val phone: String?,
    val address: String?
)