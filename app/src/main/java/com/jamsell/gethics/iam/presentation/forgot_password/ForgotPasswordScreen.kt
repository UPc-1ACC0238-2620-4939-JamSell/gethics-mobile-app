package com.jamsell.gethics.iam.presentation.forgot_password

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.jamsell.gethics.shared.ui.components.GethicsButton
import com.jamsell.gethics.shared.ui.components.GethicsTextField

@Composable
fun ForgotPasswordScreen(
    viewModel: ForgotPasswordViewModel,
    onCodeSent: () -> Unit
) {
    val state = viewModel.state.value
    var email by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(state.data) {
        if (state.data != null) {
            onCodeSent()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Recuperar contraseña",
            style = MaterialTheme.typography.titleLarge
        )

        Text(
            text = "Ingresa tu correo registrado y te enviaremos las instrucciones para restablecer tu contraseña.",
            style = MaterialTheme.typography.bodyMedium
        )

        GethicsTextField(
            value = email,
            onValueChange = { email = it },
            label = "Correo electrónico",
            modifier = Modifier.fillMaxWidth(),
            keyboardType = KeyboardType.Email
        )

        if (state.message.isNotEmpty()) {
            Text(
                text = state.message,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        GethicsButton(
            text = "Enviar instrucciones",
            onClick = { viewModel.sendRecoveryCode(email) },
            modifier = Modifier.fillMaxWidth(),
            isLoading = state.isLoading
        )
    }
}