package com.jamsell.gethics.iam.presentation.reset_password

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jamsell.gethics.shared.ui.components.GethicsButton
import com.jamsell.gethics.shared.ui.components.GethicsTextField

/** US03: token del correo + nueva contraseña (POST auth/reset-password). */
@Composable
fun ResetPasswordScreen(viewModel: ResetPasswordViewModel, onDone: () -> Unit) {
    val state = viewModel.state.value
    var token by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirm by rememberSaveable { mutableStateOf("") }
    var submitted by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(state.data) {
        if (state.data == true) onDone()
    }

    val mismatch = submitted && password != confirm

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(horizontal = 24.dp, vertical = 48.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Nueva contraseña",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "Pega el código que llegó a tu correo y elige una contraseña nueva (mínimo 8 caracteres).",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground
        )

        GethicsTextField(
            value = token,
            onValueChange = { token = it },
            label = "Código del correo",
            modifier = Modifier.fillMaxWidth()
        )
        GethicsTextField(
            value = password,
            onValueChange = { password = it },
            label = "Nueva contraseña",
            modifier = Modifier.fillMaxWidth(),
            isPassword = true
        )
        GethicsTextField(
            value = confirm,
            onValueChange = { confirm = it },
            label = "Confirmar contraseña",
            modifier = Modifier.fillMaxWidth(),
            isPassword = true,
            errorMessage = if (mismatch) "Las contraseñas no coinciden" else null
        )

        if (state.message.isNotEmpty()) {
            Text(text = state.message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
        }

        GethicsButton(
            text = "Cambiar contraseña",
            onClick = {
                submitted = true
                if (password == confirm) viewModel.resetPassword(token, password)
            },
            modifier = Modifier.fillMaxWidth(),
            isLoading = state.isLoading
        )
    }
}
