package com.jamsell.gethics.iam.data

/** Body de ForgotPasswordResource (POST auth/forgot-password). */
data class ForgotPasswordRequest(
    val email: String
)

/** Body de ResetPasswordResource (POST auth/reset-password). token = el que llega en el enlace del correo. */
data class ResetPasswordRequest(
    val token: String,
    val newPassword: String
)

/** MessageResource: { "message": "..." } (respuesta de forgot/reset password). */
data class MessageResponse(
    val message: String
)

/** Body de UpdateProfileResource (PUT users/me). El correo y el rol no se editan. */
data class UpdateProfileRequest(
    val name: String,
    val phone: String?
)

/** ProfileUpdatedResource (respuesta de PUT users/me). */
data class ProfileUpdatedResponse(
    val message: String,
    val user: UserResponse
)
