package com.jamsell.gethics.iam.presentation.reset_password

import androidx.compose.runtime.Composable
import com.jamsell.gethics.shared.ui.components.GethicsButton
import com.jamsell.gethics.shared.ui.components.ScreenPlaceholder

@Composable
fun ResetPasswordScreen(viewModel: ResetPasswordViewModel, onDone: () -> Unit) {
    // TODO: reemplazar ScreenPlaceholder por la UI real
    ScreenPlaceholder(
        title = "Nueva contrasena",
        userStory = "US03",
        description = "Token + nueva contrasena. POST auth/reset-password."
    ) {
        GethicsButton(text = "Listo (simulado)", onClick = { onDone() }, outlined = true)
    }
}
