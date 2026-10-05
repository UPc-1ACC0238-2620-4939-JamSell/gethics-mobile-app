package com.jamsell.gethics.iam.data.repository

import com.google.gson.Gson
import com.jamsell.gethics.iam.data.AuthService
import com.jamsell.gethics.iam.data.AuthenticatedUserResponse
import com.jamsell.gethics.iam.data.SignInRequest
import com.jamsell.gethics.iam.data.UserResponse
import com.jamsell.gethics.iam.domain.model.Role
import com.jamsell.gethics.shared.common.Resource
import com.jamsell.gethics.shared.data.local.FakeSharedPreferences
import com.jamsell.gethics.shared.data.local.SessionStorage
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response
import java.io.IOException

class AuthRepositoryTest {

    private val session = SessionStorage(FakeSharedPreferences())

    private fun repositoryReturning(block: (SignInRequest) -> Response<AuthenticatedUserResponse>) =
        AuthRepository(object : AuthService {
            override suspend fun signIn(request: SignInRequest) = block(request)
        }, session)

    private fun authenticated(role: String) = AuthenticatedUserResponse(
        token = "jwt-token", refreshToken = "refresh", tokenType = "Bearer", expiresIn = 3600,
        user = UserResponse(42, "Ana", "ana@gethics.pe", role, null, null)
    )

    private fun error(code: Int, body: String): Response<AuthenticatedUserResponse> =
        Response.error(code, body.toResponseBody("application/json".toMediaType()))

    @Test
    fun `200 devuelve el rol y guarda la sesion`() = runBlocking {
        val result = repositoryReturning { Response.success(authenticated("VETERINARIO")) }.signIn("ana@gethics.pe", "secreta")

        assertEquals(Role.VETERINARIO, result.data)
        assertEquals("jwt-token", session.getToken())
        assertEquals(42L, session.getUserId())
        assertEquals("VETERINARIO", session.getRole())
    }

    @Test
    fun `401 devuelve el mensaje generico del backend y no guarda sesion`() = runBlocking {
        val body = """{"code":"INVALID_CREDENTIALS","message":"Correo o contraseña incorrectos"}"""
        val result = repositoryReturning { error(401, body) }.signIn("ana@gethics.pe", "mal")

        assertTrue(result is Resource.Error)
        assertEquals("Correo o contraseña incorrectos", result.message)
        assertFalse(session.isLoggedIn())
    }

    @Test
    fun `400 por campos vacios devuelve el mensaje del backend`() = runBlocking {
        val body = """{"code":"VALIDATION_ERROR","message":"Datos inválidos","details":"email: must not be blank"}"""
        val result = repositoryReturning { error(400, body) }.signIn("", "")
        assertEquals("Datos inválidos", result.message)
    }

    @Test
    fun `sin conexion devuelve error de conexion`() = runBlocking {
        val result = repositoryReturning { throw IOException() }.signIn("ana@gethics.pe", "secreta")
        assertEquals("No se pudo conectar con el servidor", result.message)
    }

    @Test
    fun `email y password llegan intactos al service`() = runBlocking {
        var sent: SignInRequest? = null
        repositoryReturning { sent = it; Response.success(authenticated("GANADERO")) }.signIn("ana@gethics.pe", "secreta")
        assertEquals(SignInRequest("ana@gethics.pe", "secreta"), sent)
    }

    @Test
    fun `JSON real del login se deserializa en los DTOs`() {
        // Forma de AuthenticatedUserResource / UserResource (origin/feature/US01-US02-auth)
        val json = """
            {"token":"eyJhbGciOi","refreshToken":"eyJyZWZyZXNo","tokenType":"Bearer","expiresIn":3600,
             "user":{"id":7,"name":"Juan","email":"juan@gethics.pe","role":"GANADERO","phone":null,"photoUrl":null}}
        """.trimIndent()

        val response = Gson().fromJson(json, AuthenticatedUserResponse::class.java)

        assertEquals("eyJhbGciOi", response.token)
        assertEquals(3600L, response.expiresIn)
        assertEquals(7L, response.user.id)
        assertEquals("GANADERO", response.user.role)
        assertNull(response.user.phone)
    }
}
