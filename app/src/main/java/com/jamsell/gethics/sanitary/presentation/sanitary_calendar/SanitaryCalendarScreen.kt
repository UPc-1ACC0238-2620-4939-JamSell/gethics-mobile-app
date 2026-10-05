package com.jamsell.gethics.sanitary.presentation.sanitary_calendar

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import com.jamsell.gethics.sanitary.data.SanitaryEventType
import java.time.Instant
import java.time.ZoneOffset
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jamsell.gethics.R
import com.jamsell.gethics.sanitary.data.ScheduledEventDto
import com.jamsell.gethics.shared.ui.components.ErrorMessage
import com.jamsell.gethics.shared.ui.components.LoadingBox
import com.jamsell.gethics.shared.ui.theme.FigmaLineHeight
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.time.format.TextStyle as JavaTextStyle

// Lenguaje visual del frame 13:262 del Figma ("Calendario de Salud"). Ese frame es una pantalla de TAREAS: aqui solo se toman
// fondo, tarjetas, selector de mes y tipografia. Contrato y estados son los de US12 (year/month, solo scheduledDate).
// Colores locales: el theme global no cambia. Limitacion: el Figma usa Inter; aqui se mantiene Roboto con los mismos tamanos y pesos.
private val PageBackground = Color(0xFFF5F7F4)
private val TextPrimary = Color(0xFF16211A)
private val TextSecondary = Color(0xFF66736A)
private val CardBorder = Color(0xFFE2E7DF)
private val Accent = Color(0xFF733336)

private val ScreenHorizontalPadding = 20.dp
private val CardShape = RoundedCornerShape(21.6.dp)

private val TitleStyle = TextStyle(fontSize = 20.sp, lineHeight = 25.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.sp, lineHeightStyle = FigmaLineHeight, color = TextPrimary)
private val MonthStyle = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.sp, lineHeightStyle = FigmaLineHeight, color = TextPrimary)
private val DayNumberStyle = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.sp, lineHeightStyle = FigmaLineHeight, color = TextPrimary)
private val WeekdayStyle = TextStyle(fontSize = 10.sp, lineHeight = 15.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.sp, lineHeightStyle = FigmaLineHeight, color = TextSecondary)
private val EventTitleStyle = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.sp, lineHeightStyle = FigmaLineHeight, color = TextPrimary)
private val EventSubtitleStyle = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Normal, letterSpacing = 0.sp, lineHeightStyle = FigmaLineHeight, color = TextSecondary)
private val ActionMessageStyle = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Normal, letterSpacing = 0.sp, lineHeightStyle = FigmaLineHeight, color = Color(0xFFB3261E))
private val EmptyStyle = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Normal, letterSpacing = 0.sp, lineHeightStyle = FigmaLineHeight, color = TextSecondary)

private val SPANISH = Locale.forLanguageTag("es")
private val MONTH_FORMAT = DateTimeFormatter.ofPattern("MMMM yyyy", SPANISH)
private val SPOKEN_DAY_FORMAT = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", SPANISH)

private fun String.capitalized() = replaceFirstChar { it.titlecase(SPANISH) }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SanitaryCalendarScreen(viewModel: SanitaryCalendarViewModel) {
    val state = viewModel.state.value
    val events = state.data
    val completing = viewModel.completing.value
    val actionMessage = viewModel.actionMessage.value
    // US13: vacunacion cuya fecha de aplicacion se esta eligiendo (null = dialogo cerrado)
    var eventToComplete by remember { mutableStateOf<ScheduledEventDto?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PageBackground)
            .padding(horizontal = ScreenHorizontalPadding)
    ) {
        Spacer(Modifier.height(20.dp))
        Text(text = "Calendario sanitario", style = TitleStyle)

        Spacer(Modifier.height(24.dp))
        MonthSelector(
            month = viewModel.selectedMonth.value,
            onPrevious = viewModel::previousMonth,
            onNext = viewModel::nextMonth
        )

        Spacer(Modifier.height(16.dp))
        // US13: validacion local o message del backend al registrar una vacuna como aplicada
        if (actionMessage.isNotEmpty()) {
            Text(text = actionMessage, modifier = Modifier.padding(bottom = 12.dp), style = ActionMessageStyle)
        }
        when {
            state.isLoading -> LoadingBox()
            events == null -> ErrorMessage(message = state.message, onRetry = viewModel::load)
            events.isEmpty() -> EmptyMessage(message = state.message)
            else -> LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(events, key = { it.id }) { event ->
                    ScheduledEventCard(
                        event = event,
                        // Solo vacunaciones programadas: es el flujo que cubre el backend de US13
                        onComplete = if (event.type == SanitaryEventType.VACCINATION && event.status == "SCHEDULED") {
                            { eventToComplete = event }
                        } else null,
                        completing = completing
                    )
                }
            }
        }
    }

    eventToComplete?.let { event ->
        AppliedOnDialog(
            onDismiss = { eventToComplete = null },
            onConfirm = { appliedOn ->
                eventToComplete = null
                viewModel.completeVaccination(event, appliedOn)
            }
        )
    }
}

/** Icono de calendario en el acento + mes/ano (como la etiqueta del Figma) y botones circulares de 40dp para cambiar de mes. */
@Composable
private fun MonthSelector(month: YearMonth, onPrevious: () -> Unit, onNext: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Icon(painterResource(R.drawable.ic_calendar), contentDescription = null, modifier = Modifier.size(16.dp), tint = Accent)
        Spacer(Modifier.width(8.dp))
        Text(text = month.format(MONTH_FORMAT).capitalized(), modifier = Modifier.weight(1f), style = MonthStyle)
        // Cada boton ocupa 48dp de layout (area tactil minima) con el circulo de 40dp centrado: pegados dejan 8dp entre
        // circulos, y el offset de 4dp alinea el circulo derecho con el margen de 20dp.
        Row(modifier = Modifier.offset(x = 4.dp)) {
            MonthButton(rotation = 90f, description = "Mes anterior", onClick = onPrevious)
            MonthButton(rotation = -90f, description = "Mes siguiente", onClick = onNext)
        }
    }
}

/** Mismo chevron del selector de US11 rotado: 90 = apunta a la izquierda, -90 = a la derecha. */
@Composable
private fun MonthButton(rotation: Float, description: String, onClick: () -> Unit) {
    OutlinedIconButton(
        onClick = onClick,
        colors = IconButtonDefaults.outlinedIconButtonColors(containerColor = Color.White),
        border = BorderStroke(1.dp, CardBorder)
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_chevron_down),
            contentDescription = description,
            modifier = Modifier.rotate(rotation),
            tint = TextPrimary
        )
    }
}

/**
 * Tarjeta del Figma (blanca, radio 21.6, borde 1dp, sin elevacion). Columna de fecha (dia y dia de la semana, solo de
 * scheduledDate: el backend no entrega hora), titulo = tipo y subtitulo = descripcion si existe. Sin animalId.
 */
@Composable
private fun ScheduledEventCard(event: ScheduledEventDto, onComplete: (() -> Unit)?, completing: Boolean) {
    val date = LocalDate.parse(event.scheduledDate)
    val typeLabel = event.type?.label
    val description = event.description?.takeIf { it.isNotBlank() }
    val spoken = listOfNotNull(date.format(SPOKEN_DAY_FORMAT), typeLabel, description).joinToString(", ")

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = CardShape,
        color = Color.White,
        border = BorderStroke(1.dp, CardBorder),
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clearAndSetSemantics { contentDescription = spoken },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.width(40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = date.dayOfMonth.toString(), style = DayNumberStyle)
                    Text(text = weekdayAbbreviation(date), style = WeekdayStyle)
                }
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    typeLabel?.let { Text(text = it, style = EventTitleStyle) }
                    description?.let { Text(text = it, modifier = Modifier.padding(top = 2.dp), style = EventSubtitleStyle) }
                }
            }
            onComplete?.let { CompleteControl(enabled = !completing, onClick = it) }
        }
    }
}

/**
 * US13: control circular del Figma ("Checkbox - Mark ... as completed", nodo 13:301): 28dp, blanco, borde 2dp #66736A al 40%.
 * Abre la eleccion de la fecha de aplicacion; area tactil de 48dp.
 */
@Composable
private fun CompleteControl(enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .minimumInteractiveComponentSize()
            .size(28.dp)
            .clip(CircleShape)
            .background(Color.White)
            .border(2.dp, TextSecondary.copy(alpha = 0.4f), CircleShape)
            .clickable(enabled = enabled, role = Role.Button, onClickLabel = "Registrar vacuna como aplicada", onClick = onClick)
            .semantics { contentDescription = "Registrar vacuna como aplicada" }
    )
}

/**
 * US13: fecha de aplicacion (hoy por defecto). Solo hoy o fechas pasadas: el backend rechaza una aplicacion futura.
 * El DatePicker trabaja en milisegundos UTC: se convierte con UTC para no correr la fecha un dia.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppliedOnDialog(onDismiss: () -> Unit, onConfirm: (LocalDate) -> Unit) {
    val today = LocalDate.now()
    val pickerState = rememberDatePickerState(
        initialSelectedDateMillis = today.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long) =
                !Instant.ofEpochMilli(utcTimeMillis).atZone(ZoneOffset.UTC).toLocalDate().isAfter(today)

            override fun isSelectableYear(year: Int) = year <= today.year
        }
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                pickerState.selectedDateMillis?.let { onConfirm(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()) }
            }) { Text("Registrar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    ) {
        DatePicker(
            state = pickerState,
            title = { Text(text = "Fecha de aplicación", modifier = Modifier.padding(start = 24.dp, end = 12.dp, top = 16.dp)) }
        )
    }
}

/** "mar", "mie"... en mayusculas y sin punto final. */
private fun weekdayAbbreviation(date: LocalDate): String =
    date.dayOfWeek.getDisplayName(JavaTextStyle.SHORT, SPANISH).removeSuffix(".").uppercase(SPANISH)

/** Mes sin actividades: muestra el message del backend ("No hay actividades pendientes.") o el fallback del ViewModel. */
@Composable
private fun EmptyMessage(message: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = message, style = EmptyStyle, textAlign = TextAlign.Center)
    }
}
