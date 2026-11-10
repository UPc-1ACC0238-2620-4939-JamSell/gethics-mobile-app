package com.jamsell.gethics.iot.presentation.devices

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jamsell.gethics.R
import com.jamsell.gethics.iot.data.DeviceResponse
import com.jamsell.gethics.sanitary.presentation.register_event.FormHeader
import com.jamsell.gethics.sanitary.presentation.register_event.FormHorizontalPadding
import com.jamsell.gethics.sanitary.presentation.register_event.FormLabel
import com.jamsell.gethics.sanitary.presentation.register_event.FormMessage
import com.jamsell.gethics.sanitary.presentation.register_event.FormPrimaryButton
import com.jamsell.gethics.sanitary.presentation.register_event.FormTextField
import com.jamsell.gethics.shared.ui.components.GethicsCard
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private val ConnectedColor = Color(0xFF2E7D32)
private val DisconnectedColor = Color(0xFFC62828)
private val DisplayFormat = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")

/** Fecha ISO del backend (yyyy-MM-dd'T'HH:mm:ss) a dd/MM/yyyy HH:mm; si no se puede leer, se muestra tal cual. */
internal fun formatDateTime(value: String): String =
    try {
        LocalDateTime.parse(value).format(DisplayFormat)
    } catch (e: Exception) {
        value
    }

@Composable
fun DevicesScreen(
    viewModel: DevicesViewModel,
    onBack: () -> Unit,
    onOpenReadings: (deviceId: String, code: String) -> Unit
) {
    val state = viewModel.state.value
    val devices = state.data.orEmpty()

    Column(modifier = Modifier.fillMaxSize()) {
        FormHeader(title = stringResource(R.string.iot_title), onBack = onBack)

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                horizontal = FormHorizontalPadding,
                vertical = 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                FormLabel(stringResource(R.string.iot_code))
                Spacer(Modifier.height(8.dp))
                FormTextField(value = viewModel.code.value, onValueChange = viewModel::onCodeChange)
                if (viewModel.codeError.value) FormMessage(stringResource(R.string.iot_error_code))
                if (viewModel.linkMessage.value.isNotEmpty()) FormMessage(viewModel.linkMessage.value)
                Spacer(Modifier.height(16.dp))
                FormPrimaryButton(
                    text = stringResource(R.string.iot_link),
                    onClick = viewModel::link,
                    isLoading = viewModel.isLinking.value
                )
                Spacer(Modifier.height(8.dp))
            }

            if (devices.isEmpty()) {
                item {
                    val text = when {
                        state.isLoading -> stringResource(R.string.iot_loading)
                        state.message.isNotEmpty() -> state.message
                        else -> stringResource(R.string.iot_empty)
                    }
                    Text(
                        text = text,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(devices, key = { it.id }) { device ->
                    DeviceCard(device = device, onClick = { onOpenReadings(device.id, device.code) })
                }
            }
        }
    }
}

@Composable
private fun DeviceCard(device: DeviceResponse, onClick: () -> Unit) {
    GethicsCard(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = device.code,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = stringResource(
                    if (device.isConnected) R.string.iot_status_connected else R.string.iot_status_disconnected
                ),
                fontWeight = FontWeight.Bold,
                color = if (device.isConnected) ConnectedColor else DisconnectedColor
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(
                R.string.iot_last_reading,
                device.lastReadingAt?.let { formatDateTime(it) } ?: stringResource(R.string.iot_no_reading)
            ),
            modifier = Modifier.padding(top = 4.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
