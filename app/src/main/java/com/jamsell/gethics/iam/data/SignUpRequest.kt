package com.jamsell.gethics.iam.data

/**
 * Body de RegisterUserResource (POST auth/register). role: "GANADERO" | "VETERINARIO".
 * La respuesta 201 es UserResponse (sin token: el registro no inicia sesion).
 */
data class SignUpRequest(
    val name: String,
    val email: String,
    val password: String,
    val role: String
)
