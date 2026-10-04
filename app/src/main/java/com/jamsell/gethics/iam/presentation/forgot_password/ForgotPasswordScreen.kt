package com.jamsell.gethics.iam.presentation.forgot_password

import androidx.compose.runtime.Composable
import com.jamsell.gethics.shared.ui.components.GethicsButton
import com.jamsell.gethics.shared.ui.components.ScreenPlaceholder

@Composable
fun ForgotPasswordScreen(viewModel: ForgotPasswordViewModel, onBack: () -> Unit, onHaveToken: () -> Unit) {
    // TODO: reemplazar ScreenPlaceholder por la UI real
    ScreenPlaceholder(
        title = "Recuperar contrasena",
        userStory = "US03",
        description = "Pedir enlace al correo. POST auth/forgot-password."
    ) {
        GethicsButton(text = "Ya tengo mi codigo", onClick = { onHaveToken() }, outlined = true)
        GethicsButton(text = "Volver", onClick = { onBack() }, outlined = true)
    }
}
