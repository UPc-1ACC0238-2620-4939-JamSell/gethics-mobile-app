package com.jamsell.gethics.iam.data.repository

import com.jamsell.gethics.iam.data.AuthService
import com.jamsell.gethics.iam.data.ForgotPasswordRequest
import com.jamsell.gethics.iam.data.ResetPasswordRequest
import com.jamsell.gethics.iam.data.SignInRequest
import com.jamsell.gethics.iam.data.SignUpRequest
import com.jamsell.gethics.iam.domain.model.Role
import com.jamsell.gethics.shared.common.Resource
import com.jamsell.gethics.shared.data.local.SessionStorage
import com.jamsell.gethics.shared.data.remote.errorMessage
import java.io.IOException

private const val CONNECTION_ERROR = "No se pudo conectar con el servidor"

class AuthRepository(
    private val service: AuthService,
    private val sessionStorage: SessionStorage
) {
    /** Guarda token, id y rol (AuthInterceptor y la navegacion los leen) y devuelve el rol para elegir el panel. */
    suspend fun signIn(email: String, password: String): Resource<Role> =
        try {
            val response = service.signIn(SignInRequest(email, password))
            val body = response.body()
            if (response.isSuccessful && body != null) {
                sessionStorage.saveSession(body.token, body.user.id, body.user.role)
                Resource.Success(Role.from(body.user.role))
            } else {
                Resource.Error(response.errorMessage())
            }
        } catch (e: IOException) {
            Resource.Error(CONNECTION_ERROR)
        }

    /** Crea la cuenta. No guarda sesion: el backend no devuelve token y la pantalla vuelve al login. */
    suspend fun signUp(name: String, email: String, password: String, role: String): Resource<Unit> =
        try {
            val response = service.signUp(SignUpRequest(name, email, password, role))
            if (response.isSuccessful) Resource.Success(Unit) else Resource.Error(response.errorMessage())
        } catch (e: IOException) {
            Resource.Error(CONNECTION_ERROR)
        }

    /** US03. Devuelve el mensaje del backend ("Te enviamos un enlace para restablecer tu contraseña"). */
    suspend fun forgotPassword(email: String): Resource<String> =
        try {
            val response = service.forgotPassword(ForgotPasswordRequest(email))
            if (response.isSuccessful) {
                Resource.Success(response.body()?.message ?: "Te enviamos un enlace para restablecer tu contraseña")
            } else {
                Resource.Error(response.errorMessage())
            }
        } catch (e: IOException) {
            Resource.Error(CONNECTION_ERROR)
        }

    /** US03. No inicia sesion: tras el exito la pantalla vuelve al login. */
    suspend fun resetPassword(token: String, newPassword: String): Resource<String> =
        try {
            val response = service.resetPassword(ResetPasswordRequest(token.trim(), newPassword))
            if (response.isSuccessful) {
                Resource.Success(response.body()?.message ?: "Tu contraseña fue actualizada correctamente")
            } else {
                Resource.Error(response.errorMessage())
            }
        } catch (e: IOException) {
            Resource.Error(CONNECTION_ERROR)
        }

    fun signOut() = sessionStorage.clear()
}
