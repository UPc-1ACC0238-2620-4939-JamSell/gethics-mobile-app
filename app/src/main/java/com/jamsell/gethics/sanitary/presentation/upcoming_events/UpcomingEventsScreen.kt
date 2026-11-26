package com.jamsell.gethics.sanitary.presentation.upcoming_events

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jamsell.gethics.R
import com.jamsell.gethics.sanitary.data.SanitaryEventType
import com.jamsell.gethics.shared.ui.components.AlertBanner
import com.jamsell.gethics.shared.ui.components.ErrorMessage
import com.jamsell.gethics.shared.ui.components.LoadingBox
import com.jamsell.gethics.shared.ui.theme.AlertBg
import com.jamsell.gethics.shared.ui.theme.AlertText
import com.jamsell.gethics.shared.ui.theme.FigmaLineHeight
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

// Mismo lenguaje visual que el calendario sanitario (US12) y la lista de alertas (US21): fondo claro, header de 64dp con
// borde inferior y tarjetas blancas de borde 1dp. Colores locales, como en esas pantallas: el theme global no cambia.
private val PageBackground = Color(0xFFF5F7F4)
private val TextPrimary = Color(0xFF16211A)
private val TextSecondary = Color(0xFF66736A)
private val CardBorder = Color(0xFFE2E7DF)
private val NeutralBg = Color(0xFFEEF1EC)

private val CardShape = RoundedCornerShape(21.6.dp)
private val ChipShape = RoundedCornerShape(8.dp)

private val HeaderTitleStyle = TextStyle(fontSize = 18.sp, lineHeight = 28.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.sp, lineHeightStyle = FigmaLineHeight, color = TextPrimary)
private val EventTitleStyle = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.sp, lineHeightStyle = FigmaLineHeight, color = TextPrimary)
private val EventSubtitleStyle = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Normal, letterSpacing = 0.sp, lineHeightStyle = FigmaLineHeight, color = TextSecondary)
private val ChipStyle = TextStyle(fontSize = 11.sp, lineHeight = 16.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.sp, lineHeightStyle = FigmaLineHeight)
private val EmptyStyle = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Normal, letterSpacing = 0.sp, lineHeightStyle = FigmaLineHeight, color = TextSecondary)

/** US46: eventos sanitarios proximos y vencidos, con su animal, fecha y una etiqueta de urgencia. */
@Composable
fun UpcomingEventsScreen(viewModel: UpcomingEventsViewModel, onBack: () -> Unit) {
    val state = viewModel.state.value
    val events = state.data

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PageBackground)
    ) {
        Header(onBack)
        when {
            state.isLoading -> LoadingBox()
            events == null -> ErrorMessage(
                message = state.message.ifBlank { stringResource(R.string.upcoming_events_load_error) },
                onRetry = viewModel::load
            )
            events.isEmpty() -> EmptyMessage(message = state.message.ifBlank { stringResource(R.string.upcoming_events_empty) })
            else -> LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(16.dp)
            ) {
                val needAttention = events.count { it.urgency != EventUrgency.UPCOMING }
                if (needAttention > 0) {
                    item(key = "banner") {
                        AlertBanner(
                            text = if (needAttention == 1) stringResource(R.string.upcoming_events_attention_one)
                            else stringResource(R.string.upcoming_events_attention_many, needAttention)
                        )
                    }
                }
                items(events, key = { it.id }) { UpcomingEventCard(it) }
            }
        }
    }
}

/** Header 64dp + borde inferior 1dp, igual que Alertas (US21) e Historial clinico (US14). */
@Composable
private fun Header(onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 64.dp)
                .padding(start = 12.dp, end = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_back),
                    contentDescription = stringResource(R.string.upcoming_events_back),
                    modifier = Modifier.size(20.dp),
                    tint = TextPrimary
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(text = stringResource(R.string.upcoming_events_title), modifier = Modifier.weight(1f), style = HeaderTitleStyle)
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(CardBorder))
    }
}

@Composable
private fun typeLabel(type: SanitaryEventType?): String = stringResource(
    when (type) {
        SanitaryEventType.VACCINATION -> R.string.sanitary_calendar_type_vaccination
        SanitaryEventType.TREATMENT -> R.string.sanitary_calendar_type_treatment
        SanitaryEventType.DISEASE -> R.string.sanitary_calendar_type_disease
        SanitaryEventType.CHECKUP -> R.string.sanitary_calendar_type_checkup
        SanitaryEventType.OTHER, null -> R.string.sanitary_calendar_type_other
    }
)

/** "Vence hoy", "Vence en 3 dias", "Vencio hace 2 dias"... segun los dias que faltan. */
@Composable
private fun proximityLabel(daysUntil: Long): String = when {
    daysUntil < -1 -> stringResource(R.string.upcoming_events_overdue_days, -daysUntil)
    daysUntil == -1L -> stringResource(R.string.upcoming_events_overdue_yesterday)
    daysUntil == 0L -> stringResource(R.string.upcoming_events_due_today)
    daysUntil == 1L -> stringResource(R.string.upcoming_events_due_tomorrow)
    else -> stringResource(R.string.upcoming_events_due_in_days, daysUntil)
}

@Composable
private fun UpcomingEventCard(event: UpcomingEventItem) {
    val locale = LocalConfiguration.current.locales[0]
    val type = typeLabel(event.type)
    val animal = event.animalLabel ?: stringResource(R.string.upcoming_events_unknown_animal)
    val date = event.scheduledDate.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale))
    val proximity = proximityLabel(event.daysUntil)
    val (chipText, chipColor, chipBg) = when (event.urgency) {
        EventUrgency.OVERDUE -> Triple(
            stringResource(R.string.upcoming_events_badge_overdue),
            MaterialTheme.colorScheme.error,
            MaterialTheme.colorScheme.error.copy(alpha = 0.1f)
        )
        EventUrgency.URGENT -> Triple(stringResource(R.string.upcoming_events_badge_urgent), AlertText, AlertBg)
        EventUrgency.UPCOMING -> Triple(stringResource(R.string.upcoming_events_badge_upcoming), TextSecondary, NeutralBg)
    }
    val spoken = listOfNotNull(chipText, type, animal, date, proximity, event.description).joinToString(", ")

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = CardShape,
        color = Color.White,
        border = BorderStroke(1.dp, CardBorder),
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 14.dp)
                .clearAndSetSemantics { contentDescription = spoken }
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = type, modifier = Modifier.weight(1f), style = EventTitleStyle)
                Text(
                    text = chipText,
                    modifier = Modifier
                        .background(chipBg, ChipShape)
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                    style = ChipStyle.copy(color = chipColor)
                )
            }
            Text(text = animal, modifier = Modifier.padding(top = 4.dp), style = EventSubtitleStyle.copy(color = TextPrimary))
            Text(text = "$date · $proximity", modifier = Modifier.padding(top = 2.dp), style = EventSubtitleStyle)
            event.description?.let { Text(text = it, modifier = Modifier.padding(top = 2.dp), style = EventSubtitleStyle) }
        }
    }
}

/** Sin alertas: muestra el message del backend tal cual o, si no envio ninguno, upcoming_events_empty. */
@Composable
private fun EmptyMessage(message: String) {
    Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Text(text = message, style = EmptyStyle, textAlign = TextAlign.Center)
    }
}
