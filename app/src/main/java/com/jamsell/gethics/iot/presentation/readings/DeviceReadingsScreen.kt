package com.jamsell.gethics.iot.presentation.readings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jamsell.gethics.R
import com.jamsell.gethics.iot.data.SensorReadingResponse
import com.jamsell.gethics.iot.presentation.devices.formatDateTime
import com.jamsell.gethics.sanitary.presentation.register_event.FormHeader
import com.jamsell.gethics.sanitary.presentation.register_event.FormHorizontalPadding
import com.jamsell.gethics.sanitary.presentation.register_event.FormMessage
import com.jamsell.gethics.shared.ui.components.GethicsCard
import java.util.Locale

@Composable
fun DeviceReadingsScreen(viewModel: DeviceReadingsViewModel, deviceCode: String, onBack: () -> Unit) {
    val readings = viewModel.readings.value
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
            items(readings, key = { it.id }) { reading -> ReadingRow(reading) }

            when {
                // Escenario 2: el dispositivo aun no ha reportado datos
                readings.isEmpty() && viewModel.loadedOnce.value && viewModel.message.value.isEmpty() -> item {
                    Text(
                        text = stringResource(R.string.iot_readings_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                viewModel.message.value.isNotEmpty() -> item { FormMessage(viewModel.message.value) }
                viewModel.hasMore.value || viewModel.isLoading.value -> item {
                    // Al aparecer este elemento (final de la lista) se pide la siguiente pagina
                    LaunchedEffect(readings.size) { if (viewModel.hasMore.value) viewModel.loadMore() }
                    Text(
                        text = stringResource(R.string.iot_readings_loading_more),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
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
