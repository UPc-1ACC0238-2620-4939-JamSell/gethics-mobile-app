package com.jamsell.gethics.iam.presentation.sign_up

import androidx.compose.runtime.Composable
import com.jamsell.gethics.shared.ui.components.GethicsButton
import com.jamsell.gethics.shared.ui.components.ScreenPlaceholder

@Composable
fun SignUpScreen(viewModel: SignUpViewModel, onBack: () -> Unit) {
    // TODO: reemplazar ScreenPlaceholder por la UI real
    ScreenPlaceholder(
        title = "Crear cuenta",
        userStory = "US01",
        description = "Registro con rol (GANADERO / VETERINARIO). POST auth/register."
    ) {
        GethicsButton(text = "Volver", onClick = { onBack() }, outlined = true)
    }
}
