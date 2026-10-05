package com.jamsell.gethics.iam.presentation.sign_in

import com.jamsell.gethics.iam.data.AuthService
import com.jamsell.gethics.iam.data.AuthenticatedUserResponse
import com.jamsell.gethics.iam.data.SignInRequest
import com.jamsell.gethics.iam.data.UserResponse
import com.jamsell.gethics.iam.data.repository.AuthRepository
import com.jamsell.gethics.iam.domain.model.Role
import com.jamsell.gethics.shared.data.local.FakeSharedPreferences
import com.jamsell.gethics.shared.data.local.SessionStorage
import com.jamsell.gethics.shared.navigation.Routes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
class SignInViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private val requests = mutableListOf<SignInRequest>()

    private fun viewModel(response: Response<AuthenticatedUserResponse>) = SignInViewModel(
        AuthRepository(object : AuthService {
            override suspend fun signIn(request: SignInRequest): Response<AuthenticatedUserResponse> {
                requests += request
                return response
            }
        }, SessionStorage(FakeSharedPreferences()))
    )

    private fun ok(role: String) = Response.success(
        AuthenticatedUserResponse("jwt", "refresh", "Bearer", 3600, UserResponse(1, "Ana", "ana@gethics.pe", role, null, null))
    )

    private fun error(code: Int, message: String): Response<AuthenticatedUserResponse> =
        Response.error(code, """{"message":"$message"}""".toResponseBody("application/json".toMediaType()))

    @Test
    fun `credenciales correctas exponen el rol para navegar`() = runTest(dispatcher) {
        val viewModel = viewModel(ok("VETERINARIO"))

        viewModel.signIn("ana@gethics.pe", "secreta")
        assertTrue(viewModel.state.value.isLoading)
        advanceUntilIdle()

        assertEquals(Role.VETERINARIO, viewModel.state.value.data)
        assertFalse(viewModel.state.value.isLoading)
    }

    @Test
    fun `password incorrecta muestra el mensaje del backend`() = runTest(dispatcher) {
        val viewModel = viewModel(error(401, "Correo o contraseña incorrectos"))

        viewModel.signIn("ana@gethics.pe", "mal")
        advanceUntilIdle()

        assertNull(viewModel.state.value.data)
        assertEquals("Correo o contraseña incorrectos", viewModel.state.value.message)
    }

    @Test
    fun `campos vacios se envian al backend sin validacion local`() = runTest(dispatcher) {
        val viewModel = viewModel(error(400, "Datos inválidos"))

        viewModel.signIn("", "")
        advanceUntilIdle()

        assertEquals(listOf(SignInRequest("", "")), requests)
        assertEquals("Datos inválidos", viewModel.state.value.message)
    }

    @Test
    fun `cada rol navega a su panel principal`() {
        assertEquals(Routes.ANIMAL_LIST, Routes.homeFor(Role.GANADERO))
        assertEquals(Routes.ASSIGNED_CLIENTS, Routes.homeFor(Role.VETERINARIO))
    }
}
