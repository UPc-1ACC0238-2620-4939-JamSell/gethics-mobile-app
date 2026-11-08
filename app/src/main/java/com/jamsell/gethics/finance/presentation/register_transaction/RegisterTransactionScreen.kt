package com.jamsell.gethics.finance.presentation.register_transaction

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.jamsell.gethics.R
import com.jamsell.gethics.finance.data.FinancialTransactionType
import com.jamsell.gethics.sanitary.presentation.register_event.FormBottomBar
import com.jamsell.gethics.sanitary.presentation.register_event.FormDateField
import com.jamsell.gethics.sanitary.presentation.register_event.FormHeader
import com.jamsell.gethics.sanitary.presentation.register_event.FormHorizontalPadding
import com.jamsell.gethics.sanitary.presentation.register_event.FormLabel
import com.jamsell.gethics.sanitary.presentation.register_event.FormMessage
import com.jamsell.gethics.sanitary.presentation.register_event.FormPrimaryButton
import com.jamsell.gethics.sanitary.presentation.register_event.FormTextField
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/** Formulario de ingreso / gasto. Reutiliza los componentes de formulario de "Registrar evento sanitario". */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterTransactionScreen(viewModel: RegisterTransactionViewModel, onSaved: () -> Unit, onBack: () -> Unit) {
    val state = viewModel.state.value
    var type by rememberSaveable { mutableStateOf(FinancialTransactionType.EXPENSE) }
    var amount by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf("") }
    var date by rememberSaveable { mutableStateOf(LocalDate.now()) }
    var description by rememberSaveable { mutableStateOf("") }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    var formError by rememberSaveable { mutableStateOf<TransactionFormError?>(null) }

    LaunchedEffect(state.data) {
        if (state.data == true) onSaved()
    }

    val errorText = when (formError) {
        TransactionFormError.INVALID_AMOUNT -> stringResource(R.string.tx_error_amount)
        TransactionFormError.EMPTY_CATEGORY -> stringResource(R.string.tx_error_category)
        null -> state.message
    }

    Column(modifier = Modifier.fillMaxSize().imePadding()) {
        FormHeader(title = stringResource(R.string.tx_title), onBack = onBack)

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = FormHorizontalPadding)
                .padding(top = 24.dp, bottom = 16.dp)
        ) {
            FormLabel(stringResource(R.string.tx_type))
            Spacer(Modifier.height(8.dp))
            Row {
                FilterChip(
                    selected = type == FinancialTransactionType.INCOME,
                    onClick = { type = FinancialTransactionType.INCOME },
                    label = { Text(stringResource(R.string.tx_income)) }
                )
                Spacer(Modifier.width(8.dp))
                FilterChip(
                    selected = type == FinancialTransactionType.EXPENSE,
                    onClick = { type = FinancialTransactionType.EXPENSE },
                    label = { Text(stringResource(R.string.tx_expense)) }
                )
            }

            Spacer(Modifier.height(20.dp))
            FormLabel(stringResource(R.string.tx_amount))
            Spacer(Modifier.height(8.dp))
            FormTextField(
                value = amount,
                onValueChange = { text -> amount = text.filter { it.isDigit() || it == '.' || it == ',' } },
                keyboardType = KeyboardType.Decimal
            )

            Spacer(Modifier.height(20.dp))
            FormLabel(stringResource(R.string.tx_category))
            Spacer(Modifier.height(8.dp))
            FormTextField(value = category, onValueChange = { category = it })

            Spacer(Modifier.height(20.dp))
            FormLabel(stringResource(R.string.tx_date))
            Spacer(Modifier.height(8.dp))
            FormDateField(date = date, label = stringResource(R.string.tx_date), onClick = { showDatePicker = true })

            Spacer(Modifier.height(20.dp))
            FormLabel(stringResource(R.string.tx_description))
            Spacer(Modifier.height(8.dp))
            FormTextField(value = description, onValueChange = { description = it })

            if (errorText.isNotEmpty()) FormMessage(errorText)
        }

        FormBottomBar {
            FormPrimaryButton(
                text = stringResource(R.string.tx_save),
                onClick = {
                    formError = validateTransaction(amount, category)
                    if (formError == null) viewModel.save(type, amount, category, date, description)
                },
                isLoading = state.isLoading
            )
        }
    }

    if (showDatePicker) {
        // El DatePicker trabaja en milisegundos UTC: convertir con UTC para no correr la fecha un dia.
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let {
                        date = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()
                    }
                    showDatePicker = false
                }) { Text(stringResource(R.string.common_accept)) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text(stringResource(R.string.common_cancel)) }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }
}
