package com.jamsell.gethics.iam.presentation.sign_in

import androidx.compose.runtime.Composable
import com.jamsell.gethics.shared.ui.components.GethicsButton
import com.jamsell.gethics.shared.ui.components.ScreenPlaceholder

@Composable
fun SignInScreen(viewModel: SignInViewModel, onSignedIn: () -> Unit, onNavigateToSignUp: () -> Unit, onNavigateToForgotPassword: () -> Unit) {
    // TODO: reemplazar ScreenPlaceholder por la UI real
    ScreenPlaceholder(
        title = "Iniciar sesion",
        userStory = "US02",
        description = "Login. Rutas del backend: POST auth/login."
    ) {
        GethicsButton(text = "Entrar (simulado)", onClick = { onSignedIn() }, outlined = true)
        GethicsButton(text = "Crear cuenta", onClick = { onNavigateToSignUp() }, outlined = true)
        GethicsButton(text = "Olvide mi contrasena", onClick = { onNavigateToForgotPassword() }, outlined = true)
    }
}
