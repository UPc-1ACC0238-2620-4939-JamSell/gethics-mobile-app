package com.jamsell.gethics.finance.presentation.register_transaction

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.jamsell.gethics.finance.data.TransactionType
import com.jamsell.gethics.shared.ui.components.GethicsButton
import com.jamsell.gethics.shared.ui.components.GethicsTextField
import java.time.LocalDate

@Composable
fun RegisterTransactionScreen(viewModel: RegisterTransactionViewModel, onSaved: () -> Unit) {
    val state = viewModel.state.value
    var type by rememberSaveable { mutableStateOf(TransactionType.INCOME) }
    var amount by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf("") }
    var date by rememberSaveable { mutableStateOf(LocalDate.now().toString()) }
    var description by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(state.data) {
        if (state.data != null) onSaved()
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
            text = "Registrar movimiento",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground
        )

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            GethicsButton(
                text = TransactionType.INCOME.label,
                onClick = { type = TransactionType.INCOME },
                modifier = Modifier.weight(1f),
                outlined = type != TransactionType.INCOME
            )
            GethicsButton(
                text = TransactionType.EXPENSE.label,
                onClick = { type = TransactionType.EXPENSE },
                modifier = Modifier.weight(1f),
                outlined = type != TransactionType.EXPENSE
            )
        }

        GethicsTextField(
            value = amount,
            onValueChange = { amount = it },
            label = "Monto",
            modifier = Modifier.fillMaxWidth(),
            keyboardType = KeyboardType.Decimal
        )
        GethicsTextField(
            value = category,
            onValueChange = { category = it },
            label = "Categoría",
            modifier = Modifier.fillMaxWidth()
        )
        GethicsTextField(
            value = date,
            onValueChange = { date = it },
            label = "Fecha",
            modifier = Modifier.fillMaxWidth(),
            helperText = "Formato AAAA-MM-DD"
        )
        GethicsTextField(
            value = description,
            onValueChange = { description = it },
            label = "Descripción (opcional)",
            modifier = Modifier.fillMaxWidth()
        )

        if (state.message.isNotEmpty()) {
            Text(
                text = state.message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
        }

        GethicsButton(
            text = "Guardar",
            onClick = { viewModel.register(type, amount, category, date, description) },
            modifier = Modifier.fillMaxWidth(),
            isLoading = state.isLoading
        )
    }
}