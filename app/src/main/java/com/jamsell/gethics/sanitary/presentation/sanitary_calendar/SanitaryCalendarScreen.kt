package com.jamsell.gethics.sanitary.presentation.sanitary_calendar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jamsell.gethics.sanitary.data.ScheduledEventDto
import com.jamsell.gethics.shared.ui.components.EmptyState
import com.jamsell.gethics.shared.ui.components.ErrorMessage
import com.jamsell.gethics.shared.ui.components.GethicsCard
import com.jamsell.gethics.shared.ui.components.LoadingBox
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

private val SPANISH = Locale.forLanguageTag("es")
private val MONTH_FORMAT = DateTimeFormatter.ofPattern("MMMM yyyy", SPANISH)
private val DAY_FORMAT = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", SPANISH)

private fun String.capitalized() = replaceFirstChar { it.titlecase(SPANISH) }

@Composable
fun SanitaryCalendarScreen(viewModel: SanitaryCalendarViewModel) {
    val state = viewModel.state.value
    val events = state.data

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Calendario sanitario",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary
        )

        MonthSelector(
            month = viewModel.selectedMonth.value,
            onPrevious = viewModel::previousMonth,
            onNext = viewModel::nextMonth
        )

        when {
            state.isLoading -> LoadingBox()
            events == null -> ErrorMessage(message = state.message, onRetry = viewModel::load)
            events.isEmpty() -> EmptyState(message = state.message)
            else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(events, key = { it.id }) { ScheduledEventCard(it) }
            }
        }
    }
}

@Composable
private fun MonthSelector(month: YearMonth, onPrevious: () -> Unit, onNext: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onPrevious) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Mes anterior")
        }
        Text(
            text = month.format(MONTH_FORMAT).capitalized(),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center
        )
        IconButton(onClick = onNext) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Mes siguiente")
        }
    }
}

/** Solo datos utiles para el ganadero: fecha, tipo y descripcion. animalId (UUID) no se muestra. */
@Composable
private fun ScheduledEventCard(event: ScheduledEventDto) {
    GethicsCard {
        Text(
            text = LocalDate.parse(event.scheduledDate).format(DAY_FORMAT).capitalized(),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )
        event.type?.let { Text(text = it.label, style = MaterialTheme.typography.titleMedium) }
        event.description?.takeIf { it.isNotBlank() }?.let {
            Text(text = it, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
