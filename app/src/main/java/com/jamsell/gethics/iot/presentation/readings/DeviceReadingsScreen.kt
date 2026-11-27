package com.jamsell.gethics.iot.presentation.readings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jamsell.gethics.R
import com.jamsell.gethics.iot.data.SensorReadingResponse
import com.jamsell.gethics.iot.presentation.devices.formatDateTime
import com.jamsell.gethics.sanitary.presentation.register_event.FormHeader
import com.jamsell.gethics.sanitary.presentation.register_event.FormHorizontalPadding
import com.jamsell.gethics.sanitary.presentation.register_event.FormMessage
import com.jamsell.gethics.shared.ui.components.GethicsButton
import com.jamsell.gethics.shared.ui.components.GethicsCard
import java.time.LocalDate
import java.util.Locale

// Que fecha se esta eligiendo en el DatePicker
private const val PICK_NONE = 0
private const val PICK_FROM = 1
private const val PICK_TO = 2

internal const val TAG_FROM = "readings_from"
internal const val TAG_TO = "readings_to"
internal const val TAG_APPLY = "readings_apply"
internal const val TAG_CLEAR = "readings_clear"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceReadingsScreen(viewModel: DeviceReadingsViewModel, deviceCode: String, onBack: () -> Unit) {
    val locale = LocalConfiguration.current.locales[0]
    var picking by rememberSaveable { mutableIntStateOf(PICK_NONE) }
    val title = if (deviceCode.isBlank()) {
        stringResource(R.string.iot_readings_title)
    } else {
        stringResource(R.string.iot_readings_title_with_code, deviceCode)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        FormHeader(title = title, onBack = onBack)

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = FormHorizontalPadding, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item { FilterPanel(viewModel, deviceCode, locale, onPick = { picking = it }) }

            items(viewModel.readings, key = { it.id }) { reading -> ReadingRow(reading) }

            item { ListFooter(viewModel) }
        }
    }

    if (picking != PICK_NONE) {
        val current = if (picking == PICK_FROM) viewModel.from else viewModel.to
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = current?.toPickerMillis())
        DatePickerDialog(
            onDismissRequest = { picking = PICK_NONE },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let {
                        val date = it.pickerMillisToLocalDate()
                        if (picking == PICK_FROM) viewModel.onFromChange(date) else viewModel.onToChange(date)
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
private fun FilterPanel(viewModel: DeviceReadingsViewModel, deviceCode: String, locale: Locale, onPick: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        val deviceLabel = deviceCode.ifBlank { viewModel.deviceId }
        if (deviceLabel.isNotBlank()) {
            Text(
                text = stringResource(R.string.iot_readings_device, deviceLabel),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            DateFilterField(
                label = stringResource(R.string.iot_filter_from),
                date = viewModel.from,
                locale = locale,
                tag = TAG_FROM,
                onClick = { onPick(PICK_FROM) },
                onClear = { viewModel.onFromChange(null) },
                modifier = Modifier.weight(1f)
            )
            DateFilterField(
                label = stringResource(R.string.iot_filter_to),
                date = viewModel.to,
                locale = locale,
                tag = TAG_TO,
                onClick = { onPick(PICK_TO) },
                onClear = { viewModel.onToChange(null) },
                modifier = Modifier.weight(1f)
            )
        }

        if (viewModel.rangeInvalid) FormMessage(stringResource(R.string.iot_filter_invalid_range))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            GethicsButton(
                text = stringResource(R.string.iot_filter_apply),
                onClick = viewModel::applyFilters,
                modifier = Modifier.weight(1f).testTag(TAG_APPLY)
            )
            GethicsButton(
                text = stringResource(R.string.iot_filter_clear),
                onClick = viewModel::clearFilters,
                outlined = true,
                modifier = Modifier.weight(1f).testTag(TAG_CLEAR)
            )
        }

        if (viewModel.hasActiveFilters) {
            Text(
                text = stringResource(R.string.iot_filter_active, activeRangeText(viewModel.appliedFrom, viewModel.appliedTo, locale)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun activeRangeText(from: LocalDate?, to: LocalDate?, locale: Locale): String = when {
    from != null && to != null -> stringResource(R.string.iot_filter_range_both, formatFilterDate(from, locale), formatFilterDate(to, locale))
    from != null -> stringResource(R.string.iot_filter_range_from, formatFilterDate(from, locale))
    to != null -> stringResource(R.string.iot_filter_range_to, formatFilterDate(to, locale))
    else -> ""
}

@Composable
private fun DateFilterField(
    label: String,
    date: LocalDate?,
    locale: Locale,
    tag: String,
    onClick: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(onClick = onClick, modifier = Modifier.weight(1f).testTag(tag)) {
                Text(
                    text = date?.let { formatFilterDate(it, locale) } ?: stringResource(R.string.iot_filter_not_set),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (date != null) {
                IconButton(onClick = onClear) {
                    Icon(Icons.Default.Close, contentDescription = stringResource(R.string.iot_filter_clear_date, label))
                }
            }
        }
    }
}

/**
 * Estado bajo la lista: sin dispositivo / error con reintento / cargando / vacio / progreso.
 * Scroll infinito (US37): este elemento solo se compone cuando la lista llega a su final; al entrar con mas paginas
 * pendientes pide la siguiente (con el filtro aplicado). loadMore ya ignora la peticion si hay otra en curso o no hay mas.
 * Tras un error hasMore queda en false, asi que no reintenta solo: el usuario usa Reintentar.
 */
@Composable
private fun ListFooter(viewModel: DeviceReadingsViewModel) {
    val secondary = MaterialTheme.colorScheme.onSurfaceVariant
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        if (viewModel.hasValidDevice && viewModel.readings.isNotEmpty()) {
            Text(
                text = stringResource(R.string.iot_readings_progress, viewModel.readings.size, viewModel.totalElements),
                style = MaterialTheme.typography.bodySmall,
                color = secondary
            )
        }
        when {
            !viewModel.hasValidDevice -> Text(
                text = stringResource(R.string.iot_readings_device_unavailable),
                style = MaterialTheme.typography.bodyMedium,
                color = secondary
            )
            viewModel.hasError -> {
                FormMessage(
                    if (viewModel.connectionError) stringResource(R.string.iot_readings_error_connection) else viewModel.message
                )
                GethicsButton(
                    text = stringResource(R.string.iot_readings_retry),
                    onClick = viewModel::retry,
                    outlined = true,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            viewModel.isLoading -> LoadingText(viewModel.readings.isEmpty())
            // Escenario 2: el dispositivo aun no ha reportado datos (o ninguna lectura cae en el rango)
            viewModel.readings.isEmpty() && viewModel.loadedOnce -> Text(
                text = stringResource(
                    if (viewModel.hasActiveFilters) R.string.iot_readings_empty_filtered else R.string.iot_readings_empty
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = secondary
            )
            viewModel.hasMore -> {
                LaunchedEffect(viewModel.readings.size) { viewModel.loadMore() }
                LoadingText(initial = false)
            }
        }
    }
}

@Composable
private fun LoadingText(initial: Boolean) {
    Text(
        text = stringResource(if (initial) R.string.iot_readings_loading else R.string.iot_readings_loading_more),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun ReadingRow(reading: SensorReadingResponse) {
    GethicsCard(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = reading.metric,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(text = String.format(Locale.US, "%.2f", reading.value), fontWeight = FontWeight.Bold)
        }
        Text(
            text = formatDateTime(reading.recordedAt),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
