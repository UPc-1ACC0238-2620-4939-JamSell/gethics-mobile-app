package com.jamsell.gethics.iam.presentation.sign_up

import com.jamsell.gethics.iam.data.AuthService
import com.jamsell.gethics.iam.data.SignInRequest
import com.jamsell.gethics.iam.data.SignUpRequest
import com.jamsell.gethics.iam.data.UserResponse
import com.jamsell.gethics.iam.data.repository.AuthRepository
import com.jamsell.gethics.iam.domain.model.Role
import com.jamsell.gethics.shared.data.local.FakeSharedPreferences
import com.jamsell.gethics.shared.data.local.SessionStorage
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
class SignUpViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private val requests = mutableListOf<SignUpRequest>()

    private fun viewModel(response: Response<UserResponse>) = SignUpViewModel(
        AuthRepository(object : AuthService {
            override suspend fun signIn(request: SignInRequest) = error("no usado")
            override suspend fun signUp(request: SignUpRequest): Response<UserResponse> {
                requests += request
                return response
            }
        }, SessionStorage(FakeSharedPreferences()))
    )

    private fun created(role: String) = Response.success(201, UserResponse(1, "Ana", "ana@gethics.pe", role, null, null))

    private fun error(code: Int, message: String): Response<UserResponse> =
        Response.error(code, """{"message":"$message"}""".toResponseBody("application/json".toMediaType()))

    @Test
    fun `registro exitoso marca registered y el loading funciona`() = runTest(dispatcher) {
        val viewModel = viewModel(created("GANADERO"))

        viewModel.signUp("Ana", "ana@gethics.pe", "secreta123", Role.GANADERO)
        assertTrue(viewModel.state.value.isLoading)
        advanceUntilIdle()

        assertEquals(true, viewModel.state.value.data)
        assertFalse(viewModel.state.value.isLoading)
    }

    @Test
    fun `correo ya registrado expone el mensaje del backend`() = runTest(dispatcher) {
        val viewModel = viewModel(error(409, "El correo ya está en uso"))

        viewModel.signUp("Ana", "ana@gethics.pe", "secreta123", Role.GANADERO)
        advanceUntilIdle()

        assertNull(viewModel.state.value.data)
        assertEquals("El correo ya está en uso", viewModel.state.value.message)
    }

    @Test
    fun `envia exactamente el rol seleccionado`() = runTest(dispatcher) {
        val viewModel = viewModel(created("VETERINARIO"))

        viewModel.signUp("Ana", "ana@gethics.pe", "secreta123", Role.VETERINARIO)
        advanceUntilIdle()

        assertEquals(listOf(SignUpRequest("Ana", "ana@gethics.pe", "secreta123", "VETERINARIO")), requests)
    }

    @Test
    fun `sin rol seleccionado se envia vacio y decide el backend`() = runTest(dispatcher) {
        val viewModel = viewModel(error(400, "Datos inválidos"))

        viewModel.signUp("Ana", "ana@gethics.pe", "secreta123", null)
        advanceUntilIdle()

        assertEquals("", requests.single().role)
        assertEquals("Datos inválidos", viewModel.state.value.message)
    }
}
