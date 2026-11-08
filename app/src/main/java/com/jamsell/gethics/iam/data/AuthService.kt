package com.jamsell.gethics.iam.data

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthService {

    /** US02. 200 con token y usuario; 401 "Correo o contraseña incorrectos" sin decir que dato fallo. */
    @POST("auth/login")
    suspend fun signIn(@Body request: SignInRequest): Response<AuthenticatedUserResponse>

    /** US01. 201 con el usuario creado; 409 "El correo ya está en uso"; 400 "Datos inválidos". */
    @POST("auth/register")
    suspend fun signUp(@Body request: SignUpRequest): Response<UserResponse>

    /** US03. 200 con mensaje generico (no revela si el correo existe); 400 si el correo no tiene formato valido. */
    @POST("auth/forgot-password")
    suspend fun forgotPassword(@Body request: ForgotPasswordRequest): Response<MessageResponse>

    /** US03. 200 con mensaje; 400 si el token es invalido o venció, o la contraseña tiene menos de 8 caracteres. */
    @POST("auth/reset-password")
    suspend fun resetPassword(@Body request: ResetPasswordRequest): Response<MessageResponse>
}
