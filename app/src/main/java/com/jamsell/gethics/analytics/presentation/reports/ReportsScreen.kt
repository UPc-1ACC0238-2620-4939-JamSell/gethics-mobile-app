package com.jamsell.gethics.analytics.presentation.reports

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.width
import com.jamsell.gethics.R
import com.jamsell.gethics.analytics.data.ExportedReport
import com.jamsell.gethics.analytics.data.LivestockReportResponse
import com.jamsell.gethics.analytics.data.ReportSectionResponse
import com.jamsell.gethics.finance.data.FinancialTransactionType
import com.jamsell.gethics.finance.presentation.finance_summary.formatMoney
import com.jamsell.gethics.sanitary.presentation.register_event.FormDateField
import com.jamsell.gethics.sanitary.presentation.register_event.FormHorizontalPadding
import com.jamsell.gethics.sanitary.presentation.register_event.FormLabel
import com.jamsell.gethics.sanitary.presentation.register_event.FormMessage
import com.jamsell.gethics.sanitary.presentation.register_event.FormPrimaryButton
import com.jamsell.gethics.sanitary.presentation.register_event.FormTextField
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
fun ReportsScreen(
    viewModel: ReportsViewModel,
    onOpenSettings: () -> Unit,
    onOpenDevices: () -> Unit = {},
    shareFile: (Context, ExportedReport, String) -> Unit = ::shareReport
) {
    val state = viewModel.state.value
    val from = viewModel.from.value
    val to = viewModel.to.value
    var picking by rememberSaveable { mutableStateOf(PICK_NONE) }
    val exportUi = viewModel.exportState.value
    val context = LocalContext.current
    val shareTitle = stringResource(R.string.reports_export_share_title)

    // US39: cuando el CSV esta listo se abre el selector para compartirlo o guardarlo.
    LaunchedEffect(exportUi.data) {
        exportUi.data?.let {
            shareFile(context, it, shareTitle)
            viewModel.consumeExport()
        }
    }

    // Genera el reporte del mes actual la primera vez; al volver a la pantalla se conserva el reporte y los filtros aplicados.
    LaunchedEffect(Unit) { if (viewModel.appliedFilters.value == null) viewModel.applyFilters() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
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
            IconButton(onClick = onOpenDevices) {
                Icon(Icons.Filled.Sensors, contentDescription = stringResource(R.string.iot_open))
            }
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

        // US49: tipo de movimiento y categoria (borrador: no cambian el reporte hasta Aplicar filtros)
        Spacer(Modifier.height(16.dp))
        FormLabel(stringResource(R.string.tx_type))
        Spacer(Modifier.height(8.dp))
        Row {
            TypeChip(null, viewModel.type.value, viewModel::onTypeChange)
            Spacer(Modifier.width(8.dp))
            TypeChip(FinancialTransactionType.INCOME, viewModel.type.value, viewModel::onTypeChange)
            Spacer(Modifier.width(8.dp))
            TypeChip(FinancialTransactionType.EXPENSE, viewModel.type.value, viewModel::onTypeChange)
        }

        Spacer(Modifier.height(16.dp))
        FormLabel(stringResource(R.string.tx_category))
        Spacer(Modifier.height(8.dp))
        FormTextField(value = viewModel.category.value, onValueChange = viewModel::onCategoryChange)

        Spacer(Modifier.height(16.dp))
        FormPrimaryButton(
            text = stringResource(R.string.reports_apply_filters),
            onClick = viewModel::applyFilters,
            isLoading = state.isLoading
        )

        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = viewModel::clearFilters, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.reports_clear_filters))
        }

        // US49: resumen de los filtros que realmente estan aplicados al reporte visible (y a la exportacion)
        val applied = viewModel.appliedFilters.value
        if (applied != null && applied.hasActiveFilters) {
            Spacer(Modifier.height(12.dp))
            ActiveFiltersChip(applied)
        }

        Spacer(Modifier.height(12.dp))
        OutlinedButton(
            onClick = viewModel::exportReport,
            enabled = !exportUi.isLoading && applied != null,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                stringResource(if (exportUi.isLoading) R.string.reports_exporting else R.string.reports_export)
            )
        }
        // Escenario 2 de US39: sin informacion con esos filtros, el aviso del servidor se muestra tal cual
        if (exportUi.message.isNotEmpty()) FormMessage(exportUi.message)
        viewModel.exportedFileName.value?.let {
            Text(
                text = stringResource(R.string.reports_exported, it),
                modifier = Modifier.padding(top = 8.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        val report = state.data
        if (report != null) {
            Spacer(Modifier.height(20.dp))
            ReportContent(report, filtered = applied?.hasActiveFilters == true)
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
private fun ReportContent(report: LivestockReportResponse, filtered: Boolean) {
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
                if (filtered) NoResultsNote()
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

/** US49. Una opcion del selector de tipo: null = Todos (no se envia type al backend). */
@Composable
private fun TypeChip(
    option: FinancialTransactionType?,
    selected: FinancialTransactionType?,
    onSelect: (FinancialTransactionType?) -> Unit
) {
    FilterChip(
        modifier = Modifier.testTag(typeChipTag(option)),
        selected = option == selected,
        onClick = { onSelect(option) },
        label = { Text(typeLabel(option)) }
    )
}

@Composable
private fun typeLabel(type: FinancialTransactionType?): String = stringResource(
    when (type) {
        null -> R.string.reports_filter_all
        FinancialTransactionType.INCOME -> R.string.finance_income
        FinancialTransactionType.EXPENSE -> R.string.finance_expense
    }
)

/** US49. "Filtros activos: Gastos · Alimento", con lo que realmente esta aplicado (no con el borrador). */
@Composable
private fun ActiveFiltersChip(applied: ReportFilters) {
    val summary = listOfNotNull(applied.type?.let { typeLabel(it) }, applied.category).joinToString(" · ")
    Surface(
        modifier = Modifier.testTag(ACTIVE_FILTERS_TAG),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.secondaryContainer
    ) {
        Text(
            text = stringResource(R.string.reports_filters_active, summary),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}

@Composable
private fun NoResultsNote() {
    Text(
        text = stringResource(R.string.reports_no_results_filtered),
        modifier = Modifier.padding(top = 8.dp),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

internal const val ACTIVE_FILTERS_TAG = "reports_active_filters"

/** Etiqueta de prueba del chip de tipo: reports_type_ALL | reports_type_INCOME | reports_type_EXPENSE. */
internal fun typeChipTag(type: FinancialTransactionType?) = "reports_type_${type?.name ?: "ALL"}"
