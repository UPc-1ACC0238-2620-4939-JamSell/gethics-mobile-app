package com.jamsell.gethics.analytics.presentation.reports

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jamsell.gethics.R
import com.jamsell.gethics.analytics.data.LivestockReportResponse
import com.jamsell.gethics.analytics.data.ReportSectionResponse
import com.jamsell.gethics.finance.presentation.finance_summary.formatMoney
import com.jamsell.gethics.sanitary.presentation.register_event.FormDateField
import com.jamsell.gethics.sanitary.presentation.register_event.FormHorizontalPadding
import com.jamsell.gethics.sanitary.presentation.register_event.FormLabel
import com.jamsell.gethics.sanitary.presentation.register_event.FormMessage
import com.jamsell.gethics.sanitary.presentation.register_event.FormPrimaryButton
import com.jamsell.gethics.shared.ui.components.GethicsCard
import java.time.Instant
import java.time.ZoneOffset

private val IncomeColor = Color(0xFF2E7D32)
private val ExpenseColor = Color(0xFFC62828)

// Que fecha se esta eligiendo en el DatePicker
private const val PICK_NONE = 0
private const val PICK_FROM = 1
private const val PICK_TO = 2

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(viewModel: ReportsViewModel, onOpenSettings: () -> Unit) {
    val state = viewModel.state.value
    val from = viewModel.from.value
    val to = viewModel.to.value
    var picking by rememberSaveable { mutableStateOf(PICK_NONE) }

    // Genera el reporte del mes actual al abrir la pantalla.
    LaunchedEffect(Unit) { viewModel.generate() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = FormHorizontalPadding)
            .padding(top = 12.dp, bottom = 24.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.reports_title),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleLarge
            )
            IconButton(onClick = onOpenSettings) {
                Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.settings_open))
            }
        }

        Spacer(Modifier.height(12.dp))
        FormLabel(stringResource(R.string.reports_from))
        Spacer(Modifier.height(8.dp))
        FormDateField(date = from, label = stringResource(R.string.reports_from), onClick = { picking = PICK_FROM })

        Spacer(Modifier.height(16.dp))
        FormLabel(stringResource(R.string.reports_to))
        Spacer(Modifier.height(8.dp))
        FormDateField(date = to, label = stringResource(R.string.reports_to), onClick = { picking = PICK_TO })

        if (viewModel.rangeError.value) FormMessage(stringResource(R.string.reports_error_range))

        Spacer(Modifier.height(16.dp))
        FormPrimaryButton(
            text = stringResource(R.string.reports_generate),
            onClick = viewModel::generate,
            isLoading = state.isLoading
        )

        val report = state.data
        if (report != null) {
            Spacer(Modifier.height(20.dp))
            ReportContent(report)
        } else if (state.message.isNotEmpty()) {
            FormMessage(state.message)
        }
    }

    if (picking != PICK_NONE) {
        val current = if (picking == PICK_FROM) from else to
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = current.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { picking = PICK_NONE },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let {
                        val date = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()
                        if (picking == PICK_FROM) viewModel.setFrom(date) else viewModel.setTo(date)
                    }
                    picking = PICK_NONE
                }) { Text(stringResource(R.string.common_accept)) }
            },
            dismissButton = {
                TextButton(onClick = { picking = PICK_NONE }) { Text(stringResource(R.string.common_cancel)) }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }
}

@Composable
private fun ReportContent(report: LivestockReportResponse) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        GethicsCard {
            Text(
                text = stringResource(R.string.reports_finance),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(8.dp))
            AmountRow(stringResource(R.string.finance_income), formatMoney(report.finance.totalIncome), IncomeColor)
            AmountRow(stringResource(R.string.finance_expense), formatMoney(report.finance.totalExpense), ExpenseColor)
            AmountRow(
                stringResource(R.string.reports_net),
                formatMoney(report.finance.netResult),
                if (report.finance.netResult < 0) ExpenseColor else IncomeColor
            )
            if (report.finance.limitedData) {
                LimitedNote(report.finance.message)
            }
        }
        SectionCard(stringResource(R.string.reports_health), report.health)
        SectionCard(stringResource(R.string.reports_productivity), report.productivity)
    }
}

@Composable
private fun AmountRow(label: String, value: String, color: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
    ) {
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Text(value, fontWeight = FontWeight.Bold, color = color)
    }
}

@Composable
private fun SectionCard(title: String, section: ReportSectionResponse) {
    GethicsCard {
        Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        if (section.limitedData) {
            LimitedNote(section.message)
        } else {
            Text(
                text = section.message ?: stringResource(R.string.reports_ok),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun LimitedNote(message: String?) {
    Text(
        text = message ?: stringResource(R.string.reports_limited),
        modifier = Modifier.padding(top = 8.dp),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}
