package com.jamsell.gethics.iam.data

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthService {

    /** US02. 200 con token y usuario; 401 "Correo o contraseña incorrectos" sin decir que dato fallo. */
    @POST("auth/login")
    suspend fun signIn(@Body request: SignInRequest): Response<AuthenticatedUserResponse>

    // TODO: auth/register (US01), auth/forgot-password y auth/reset-password (US03)
}
