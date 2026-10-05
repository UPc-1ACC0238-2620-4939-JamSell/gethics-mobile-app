package com.jamsell.gethics.iam.data

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface AuthService {

    /** US02. 200 con token y usuario; 401 "Correo o contraseña incorrectos" sin decir que dato fallo. */
    @POST("auth/login")
    suspend fun signIn(@Body request: SignInRequest): Response<AuthenticatedUserResponse>

    /** US01. 201 con el usuario creado; 409 "El correo ya está en uso"; 400 "Datos inválidos". */
    @POST("auth/register")
    suspend fun signUp(@Body request: SignUpRequest): Response<UserResponse>
    @POST("api/v1/auth/forgot-password")
    suspend fun forgotPassword(@Body request: ForgotPasswordRequest): Response<AuthResponse>

    @POST("api/v1/auth/reset-password")
    suspend fun resetPassword(@Body request: ResetPasswordRequest): Response<AuthResponse>
    // TODO: auth/forgot-password y auth/reset-password (US03)

    @GET("api/v1/users/{id}")
    suspend fun getUserProfile(@Path("id") userId: String): Response<UserProfileDto>

    @PUT("api/v1/users/{id}")
    suspend fun updateProfile(
        @Path("id") userId: String,
        @Body request: UpdateProfileRequest
    ): Response<UserProfileDto>
}
