package com.jamsell.gethics.finance.presentation.finance_summary

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
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
import com.jamsell.gethics.finance.data.TransactionDto
import com.jamsell.gethics.finance.data.TransactionType
import com.jamsell.gethics.shared.ui.components.GethicsButton
import java.time.LocalDate
import java.util.Locale

private fun money(value: Double) = "S/ " + String.format(Locale.US, "%,.2f", value)

private fun inCurrentMonth(date: String): Boolean = try {
    val parsed = LocalDate.parse(date)
    val now = LocalDate.now()
    parsed.year == now.year && parsed.month == now.month
} catch (e: Exception) {
    false
}

@Composable
fun FinanceSummaryScreen(viewModel: FinanceSummaryViewModel, onRegisterTransaction: () -> Unit) {
    val state = viewModel.state.value
    var onlyThisMonth by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) { viewModel.load() }

    val summary = state.data
    val visible = summary?.transactions
        ?.filter { !onlyThisMonth || inCurrentMonth(it.date) }
        .orEmpty()
    val income = visible.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
    val expense = visible.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Finanzas", style = MaterialTheme.typography.titleLarge)

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            GethicsButton("Todo", { onlyThisMonth = false }, Modifier.weight(1f), outlined = onlyThisMonth)
            GethicsButton("Este mes", { onlyThisMonth = true }, Modifier.weight(1f), outlined = !onlyThisMonth)
        }

        if (summary != null) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Balance neto", style = MaterialTheme.typography.labelLarge)
                    Text(money(income - expense), style = MaterialTheme.typography.headlineMedium)
                    Text("Ingresos: ${money(income)}", style = MaterialTheme.typography.bodyMedium)
                    Text("Egresos: ${money(expense)}", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        GethicsButton("Registrar ingreso / gasto", onRegisterTransaction, Modifier.fillMaxWidth(), outlined = true)

        when {
            state.isLoading && summary == null -> CircularProgressIndicator()
            state.message.isNotEmpty() ->
                Text(state.message, color = MaterialTheme.colorScheme.error)
            summary != null && visible.isEmpty() ->
                Text("No hay movimientos en este periodo. El balance es S/ 0.00")
            else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(visible, key = { it.id }) { TransactionRow(it) }
            }
        }
    }
}

@Composable
private fun TransactionRow(item: TransactionDto) {
    val isIncome = item.type == TransactionType.INCOME
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(item.category, style = MaterialTheme.typography.titleMedium)
                Text(item.date, style = MaterialTheme.typography.bodySmall)
            }
            Text(
                text = (if (isIncome) "+ " else "- ") + money(item.amount),
                color = if (isIncome) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}