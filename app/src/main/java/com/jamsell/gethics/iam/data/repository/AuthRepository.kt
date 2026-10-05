package com.jamsell.gethics.iam.data.repository

import com.jamsell.gethics.iam.data.AuthService
import com.jamsell.gethics.iam.data.SignInRequest
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

    // TODO: signUp (US01) / forgotPassword / resetPassword (US03)

    fun signOut() = sessionStorage.clear()
}
