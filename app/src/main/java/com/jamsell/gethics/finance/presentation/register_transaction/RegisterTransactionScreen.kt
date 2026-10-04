package com.jamsell.gethics.finance.presentation.register_transaction

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.jamsell.gethics.shared.ui.components.GethicsButton
import com.jamsell.gethics.shared.ui.components.ScreenPlaceholder

@Composable
fun RegisterTransactionScreen(viewModel: RegisterTransactionViewModel, onSaved: () -> Unit) {
    // TODO: reemplazar ScreenPlaceholder por la UI real
    ScreenPlaceholder(
        title = "Registrar ingreso / gasto",
        userStory = "US15",
        description = "Formulario para registrar un ingreso o gasto."
    ) {
        GethicsButton(text = "Guardar (simulado)", onClick = { onSaved() }, outlined = true)
    }
}
