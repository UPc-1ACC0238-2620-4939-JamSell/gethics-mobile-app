package com.jamsell.gethics.sanitary.presentation.clinical_history

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.jamsell.gethics.sanitary.data.ClinicalHistoryEventDto
import com.jamsell.gethics.sanitary.data.SanitaryEventStatus
import com.jamsell.gethics.shared.ui.components.ErrorMessage
import com.jamsell.gethics.shared.ui.components.LoadingBox
import com.jamsell.gethics.shared.ui.theme.FigmaLineHeight
import java.time.LocalDate

// Lenguaje visual de la linea de tiempo "Chequeos recientes" del frame 10:2 del Figma. Es la pestana Salud de la ficha del animal:
// de ahi se toman fondo, marcadores, conector y tarjetas. NO se copian autor, dosis, chips ni acciones: US14 no los tiene.
// Colores locales: el theme global no cambia. Limitacion: el Figma usa Montserrat; aqui se mantiene Roboto con los mismos tamanos y pesos.
private val PageBackground = Color(0xFFF8FBF8)
private val TextPrimary = Color(0xFF141D16)
private val TextSecondary = Color(0xFF5D665F)
private val Divider = Color(0xFFDDE3DD) // borde de tarjeta y conector
private val Accent = Color(0xFF733336)

private val CardShape = RoundedCornerShape(14.dp)

private val TitleStyle = TextStyle(fontSize = 18.sp, lineHeight = 28.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.sp, lineHeightStyle = FigmaLineHeight, color = TextPrimary)
private val EventTitleStyle = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.sp, lineHeightStyle = FigmaLineHeight, color = TextPrimary)
private val DateStyle = TextStyle(fontSize = 11.sp, lineHeight = 16.5.sp, fontWeight = FontWeight.Normal, letterSpacing = 0.sp, lineHeightStyle = FigmaLineHeight, color = TextSecondary)
private val StatusStyle = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.sp, lineHeightStyle = FigmaLineHeight, color = TextSecondary)
private val DescriptionStyle = TextStyle(fontSize = 12.sp, lineHeight = 19.5.sp, fontWeight = FontWeight.Normal, letterSpacing = 0.sp, lineHeightStyle = FigmaLineHeight, color = TextPrimary.copy(alpha = 0.8f))
private val EmptyStyle = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Normal, letterSpacing = 0.sp, lineHeightStyle = FigmaLineHeight, color = TextSecondary)

/** Texto que define el Figma (43:1111) para un evento sin fecha utilizable: no se inventa ninguna fecha. */
private const val NO_DATE = "Sin fecha registrada"

// Abreviaturas fijas: iguales en cualquier dispositivo y como en el Figma ("24 sep 2026").
private val MONTHS = listOf("ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "sep", "oct", "nov", "dic")

private fun shortDate(date: LocalDate): String =
    "${date.dayOfMonth.toString().padStart(2, '0')} ${MONTHS[date.monthValue - 1]} ${date.year}"

@Composable
fun ClinicalHistoryScreen(viewModel: ClinicalHistoryViewModel, onBack: () -> Unit) {
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
            events == null -> ErrorMessage(message = state.message, onRetry = viewModel::load)
            events.isEmpty() -> EmptyMessage(message = state.message)
            // Orden del backend (mas antiguo primero): no se reordena aqui.
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 24.dp)
            ) {
                itemsIndexed(events, key = { _, event -> event.id }) { index, event ->
                    TimelineItem(event = event, isLast = index == events.lastIndex)
                }
            }
        }
    }
}

/** Header 64dp + borde inferior 1dp: flecha de 20dp (IconButton de 48dp = area tactil) y titulo. */
@Composable
private fun Header(onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 64.dp) // crece si la fuente escalada obliga a partir el titulo
                .padding(start = 12.dp, end = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                // Misma flecha de Lucide que el registro y US11: a 20dp coincide con la del Figma
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_back),
                    contentDescription = "Volver",
                    modifier = Modifier.size(20.dp),
                    tint = TextPrimary
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(text = "Historial clínico", modifier = Modifier.weight(1f), style = TitleStyle)
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(Divider))
    }
}

/**
 * Marcador de 40dp + conector de 1dp hasta el siguiente marcador + tarjeta (a 12dp del marcador, 20dp entre eventos).
 * El conector ocupa todo el alto del item, incluidos los 20dp de separacion: la linea no se corta entre tarjetas.
 */
@Composable
private fun TimelineItem(event: ClinicalHistoryEventDto, isLast: Boolean) {
    Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
        Column(modifier = Modifier.width(40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Marker(filled = event.status == SanitaryEventStatus.COMPLETED)
            if (!isLast) {
                Box(Modifier.width(1.dp).weight(1f).background(Divider))
            }
        }
        Spacer(Modifier.width(12.dp))
        EventCard(
            event = event,
            modifier = Modifier
                .weight(1f)
                .padding(bottom = if (isLast) 0.dp else 20.dp)
        )
    }
}

/** COMPLETED: relleno `#733336`. SCHEDULED, CANCELLED (y un estado desconocido): solo el borde. Sin iconos. */
@Composable
private fun Marker(filled: Boolean) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .then(if (filled) Modifier.background(Accent) else Modifier)
            .border(2.dp, Accent, CircleShape)
    )
}

/** Tarjeta del Figma (blanca, radio 14, borde 1dp, sin elevacion): tipo, fecha a la derecha, estado y descripcion si existe. */
@Composable
private fun EventCard(event: ClinicalHistoryEventDto, modifier: Modifier = Modifier) {
    val dateText = effectiveDate(event)?.let(::shortDate) ?: NO_DATE
    val typeLabel = event.type?.label
    val statusLabel = event.status?.label
    val description = event.description?.takeIf { it.isNotBlank() }
    val spoken = listOfNotNull(typeLabel, statusLabel, dateText, description).joinToString(", ")

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clearAndSetSemantics { contentDescription = spoken },
        shape = CardShape,
        color = Color.White,
        border = BorderStroke(1.dp, Divider),
        shadowElevation = 0.dp
    ) {
        Column(modifier = Modifier.padding(13.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Text(text = typeLabel.orEmpty(), modifier = Modifier.weight(1f), style = EventTitleStyle)
                Text(text = dateText, modifier = Modifier.padding(start = 8.dp), style = DateStyle, textAlign = TextAlign.End)
            }
            statusLabel?.let { Text(text = it, modifier = Modifier.padding(top = 2.dp), style = StatusStyle) }
            description?.let { Text(text = it, modifier = Modifier.padding(top = 8.dp), style = DescriptionStyle) }
        }
    }
}

/** Sin registros: muestra el message del backend ("Sin registros.") o el fallback del ViewModel. */
@Composable
private fun EmptyMessage(message: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = message, style = EmptyStyle, textAlign = TextAlign.Center)
    }
}
