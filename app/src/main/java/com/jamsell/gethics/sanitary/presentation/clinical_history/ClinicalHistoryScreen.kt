package com.jamsell.gethics.sanitary.presentation.clinical_history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jamsell.gethics.sanitary.data.ClinicalHistoryEventDto
import com.jamsell.gethics.shared.ui.components.EmptyState
import com.jamsell.gethics.shared.ui.components.ErrorMessage
import com.jamsell.gethics.shared.ui.components.GethicsCard
import com.jamsell.gethics.shared.ui.components.LoadingBox
import java.time.format.DateTimeFormatter
import java.util.Locale

// Solo fecha, sin hora: US11 registra con fecha unicamente (se envia a las 00:00).
private val DATE_FORMAT = DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy", Locale.forLanguageTag("es"))

@Composable
fun ClinicalHistoryScreen(viewModel: ClinicalHistoryViewModel) {
    val state = viewModel.state.value
    val events = state.data

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Historial clinico",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary
        )

        when {
            state.isLoading -> LoadingBox()
            events == null -> ErrorMessage(message = state.message, onRetry = viewModel::load)
            events.isEmpty() -> EmptyState(message = state.message)
            // Orden del backend (mas antiguo primero): no se reordena aqui.
            else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(events, key = { it.id }) { ClinicalHistoryEventCard(it) }
            }
        }
    }
}

/** Fecha efectiva, tipo, estado y descripcion. Sin ids ni hora. */
@Composable
private fun ClinicalHistoryEventCard(event: ClinicalHistoryEventDto) {
    GethicsCard {
        effectiveDate(event)?.let {
            Text(
                text = it.format(DATE_FORMAT),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }
        event.type?.let { Text(text = it.label, style = MaterialTheme.typography.titleMedium) }
        event.status?.let { Text(text = it.label, style = MaterialTheme.typography.bodySmall) }
        event.description?.takeIf { it.isNotBlank() }?.let {
            Text(text = it, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
