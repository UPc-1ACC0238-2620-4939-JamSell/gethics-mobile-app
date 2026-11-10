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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import com.jamsell.gethics.R
import com.jamsell.gethics.sanitary.data.ScheduledEventDto
import com.jamsell.gethics.shared.ui.components.ErrorMessage
import com.jamsell.gethics.shared.ui.components.LoadingBox
import com.jamsell.gethics.shared.ui.theme.FigmaLineHeight
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
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

// Mes y dias en el idioma activo de la app (LocalConfiguration): nombres desde java.time, sin listas manuales.
private const val MONTH_PATTERN = "MMMM yyyy"

private fun String.capitalized(locale: Locale) = replaceFirstChar { it.titlecase(locale) }

@Composable
private fun currentLocale(): Locale = LocalConfiguration.current.locales[0]

/** Tipo del evento en el idioma de la app (SanitaryEventType.label queda en espanol para el registro de eventos). */
@Composable
private fun typeLabel(type: SanitaryEventType): String = stringResource(
    when (type) {
        SanitaryEventType.VACCINATION -> R.string.sanitary_calendar_type_vaccination
        SanitaryEventType.TREATMENT -> R.string.sanitary_calendar_type_treatment
        SanitaryEventType.DISEASE -> R.string.sanitary_calendar_type_disease
        SanitaryEventType.CHECKUP -> R.string.sanitary_calendar_type_checkup
        SanitaryEventType.OTHER -> R.string.sanitary_calendar_type_other
    }
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SanitaryCalendarScreen(
    viewModel: SanitaryCalendarViewModel,
    // null = sin acceso a Ajustes (mismo boton que Finanzas/Reportes)
    onOpenSettings: (() -> Unit)? = null
) {
    val state = viewModel.state.value
    val events = state.data
    val completing = viewModel.completing.value
    val actionMessage = viewModel.actionMessage.value
    // US13: vacunacion cuya fecha de aplicacion se esta eligiendo (null = dialogo cerrado)
    var eventToComplete by remember { mutableStateOf<ScheduledEventDto?>(null) }
    // US33: evento cuya cancelacion se esta confirmando (null = dialogo cerrado)
    var eventToCancel by remember { mutableStateOf<ScheduledEventDto?>(null) }
    val busy = completing || viewModel.cancelling.value

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PageBackground)
            .padding(horizontal = ScreenHorizontalPadding)
    ) {
        Spacer(Modifier.height(20.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = stringResource(R.string.sanitary_calendar_title), modifier = Modifier.weight(1f), style = TitleStyle)
            if (onOpenSettings != null) {
                IconButton(onClick = onOpenSettings) {
                    Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.settings_open))
                }
            }
        }

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
            events.isEmpty() -> EmptyMessage(message = state.message.ifBlank { stringResource(R.string.sanitary_calendar_empty) })
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
                        // US33: cualquier evento pendiente (SCHEDULED) puede cancelarse
                        onCancel = if (event.status == "SCHEDULED") {
                            { eventToCancel = event }
                        } else null,
                        busy = busy
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

    // US33: confirmacion para evitar una cancelacion por un toque accidental
    eventToCancel?.let { event ->
        AlertDialog(
            onDismissRequest = { eventToCancel = null },
            title = { Text(stringResource(R.string.sanitary_calendar_cancel_confirm)) },
            confirmButton = {
                TextButton(onClick = {
                    eventToCancel = null
                    viewModel.cancelEvent(event)
                }) { Text(stringResource(R.string.sanitary_calendar_cancel_event)) }
            },
            dismissButton = {
                TextButton(onClick = { eventToCancel = null }) { Text(stringResource(R.string.sanitary_calendar_cancel_keep)) }
            }
        )
    }
}

/** Icono de calendario en el acento + mes/ano (como la etiqueta del Figma) y botones circulares de 40dp para cambiar de mes. */
@Composable
private fun MonthSelector(month: YearMonth, onPrevious: () -> Unit, onNext: () -> Unit) {
    val locale = currentLocale()
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Icon(painterResource(R.drawable.ic_calendar), contentDescription = null, modifier = Modifier.size(16.dp), tint = Accent)
        Spacer(Modifier.width(8.dp))
        Text(text = month.format(DateTimeFormatter.ofPattern(MONTH_PATTERN, locale)).capitalized(locale), modifier = Modifier.weight(1f), style = MonthStyle)
        // Cada boton ocupa 48dp de layout (area tactil minima) con el circulo de 40dp centrado: pegados dejan 8dp entre
        // circulos, y el offset de 4dp alinea el circulo derecho con el margen de 20dp.
        Row(modifier = Modifier.offset(x = 4.dp)) {
            MonthButton(rotation = 90f, description = stringResource(R.string.sanitary_calendar_previous_month), onClick = onPrevious)
            MonthButton(rotation = -90f, description = stringResource(R.string.sanitary_calendar_next_month), onClick = onNext)
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
private fun ScheduledEventCard(event: ScheduledEventDto, onComplete: (() -> Unit)?, onCancel: (() -> Unit)?, busy: Boolean) {
    val date = LocalDate.parse(event.scheduledDate)
    val locale = currentLocale()
    val typeLabel = event.type?.let { typeLabel(it) }
    val description = event.description?.takeIf { it.isNotBlank() }
    val spoken = listOfNotNull(date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(locale)), typeLabel, description).joinToString(", ")

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
                    Text(text = weekdayAbbreviation(date, locale), style = WeekdayStyle)
                }
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    typeLabel?.let { Text(text = it, style = EventTitleStyle) }
                    description?.let { Text(text = it, modifier = Modifier.padding(top = 2.dp), style = EventSubtitleStyle) }
                }
            }
            onComplete?.let { CompleteControl(enabled = !busy, onClick = it) }
            // US33: abre la confirmacion; IconButton ya ocupa 48dp (misma altura que CompleteControl)
            onCancel?.let {
                IconButton(onClick = it, enabled = !busy) {
                    Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.sanitary_calendar_cancel_event), tint = TextSecondary)
                }
            }
        }
    }
}

/**
 * US13: control circular del Figma ("Checkbox - Mark ... as completed", nodo 13:301): 28dp, blanco, borde 2dp #66736A al 40%.
 * Abre la eleccion de la fecha de aplicacion; area tactil de 48dp.
 */
@Composable
private fun CompleteControl(enabled: Boolean, onClick: () -> Unit) {
    val label = stringResource(R.string.sanitary_calendar_mark_applied)
    Box(
        modifier = Modifier
            .minimumInteractiveComponentSize()
            .size(28.dp)
            .clip(CircleShape)
            .background(Color.White)
            .border(2.dp, TextSecondary.copy(alpha = 0.4f), CircleShape)
            .clickable(enabled = enabled, role = Role.Button, onClickLabel = label, onClick = onClick)
            .semantics { contentDescription = label }
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
            }) { Text(stringResource(R.string.sanitary_calendar_register)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) } }
    ) {
        DatePicker(
            state = pickerState,
            title = { Text(text = stringResource(R.string.sanitary_calendar_applied_on), modifier = Modifier.padding(start = 24.dp, end = 12.dp, top = 16.dp)) }
        )
    }
}

/** "mar", "mie"... en mayusculas y sin punto final. */
private fun weekdayAbbreviation(date: LocalDate, locale: Locale): String =
    date.dayOfWeek.getDisplayName(JavaTextStyle.SHORT, locale).removeSuffix(".").uppercase(locale)

/** Mes sin actividades: muestra el message del backend tal cual o, si no envia ninguno, sanitary_calendar_empty. */
@Composable
private fun EmptyMessage(message: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = message, style = EmptyStyle, textAlign = TextAlign.Center)
    }
}
