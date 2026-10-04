package com.jamsell.gethics.finance.presentation.finance_summary

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.jamsell.gethics.shared.ui.components.GethicsButton
import com.jamsell.gethics.shared.ui.components.ScreenPlaceholder

@Composable
fun FinanceSummaryScreen(viewModel: FinanceSummaryViewModel, onRegisterTransaction: () -> Unit) {
    // TODO: reemplazar ScreenPlaceholder por la UI real
    ScreenPlaceholder(
        title = "Finanzas",
        userStory = "US15",
        description = "Resumen y lista de ingresos y gastos."
    ) {
        GethicsButton(text = "Registrar ingreso / gasto", onClick = { onRegisterTransaction() }, outlined = true)
    }
}
