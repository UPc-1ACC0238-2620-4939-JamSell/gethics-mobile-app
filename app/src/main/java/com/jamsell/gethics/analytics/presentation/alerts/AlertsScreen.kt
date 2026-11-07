package com.jamsell.gethics.analytics.presentation.alerts

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jamsell.gethics.R
import com.jamsell.gethics.analytics.data.AlertDto
import com.jamsell.gethics.analytics.data.RiskLevel
import com.jamsell.gethics.shared.ui.components.ErrorMessage
import com.jamsell.gethics.shared.ui.components.LoadingBox
import com.jamsell.gethics.shared.ui.theme.FigmaLineHeight
import java.time.ZoneId

// Lenguaje visual de la lista "Alertas Recientes" del frame 6:198 (Inicio) del Figma, nodo 6:247: tarjeta blanca de radio 18
// con filas separadas por un borde de 1dp, icono de 40dp en un recuadro de color y titulo "negrita - texto". El Figma no tiene
// una pantalla de alertas propia ("Ver todas" no tiene frame), asi que esta pantalla es esa lista a pantalla completa.
// NO se copia el chevron: la fila no abre nada porque el backend no tiene detalle ni acciones sobre una alerta.
// Colores locales, como en US14: el theme global no cambia. El Figma usa Geist; aqui se mantiene Roboto con los mismos tamanos.
private val PageBackground = Color(0xFFF8FBF8)
private val TextPrimary = Color(0xFF16211A)
private val TextSecondary = Color(0xFF5F6B62)
private val Divider = Color(0xFFE3E8E1)
private val HighRiskBg = Color(0x1AC62828)  // rgba(198,40,40,0.1)
private val WarningBg = Color(0x26E8A317)   // rgba(232,163,23,0.15)

private val ListShape = RoundedCornerShape(18.dp)
private val IconShape = RoundedCornerShape(14.dp)

private val HeaderTitleStyle = TextStyle(fontSize = 18.sp, lineHeight = 28.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.sp, lineHeightStyle = FigmaLineHeight, color = TextPrimary)
private val TitleStyle = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Normal, letterSpacing = 0.sp, lineHeightStyle = FigmaLineHeight, color = TextSecondary)
private val DetailStyle = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Normal, letterSpacing = 0.sp, lineHeightStyle = FigmaLineHeight, color = TextSecondary)
private val EmptyStyle = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Normal, letterSpacing = 0.sp, lineHeightStyle = FigmaLineHeight, color = TextSecondary)

@Composable
fun AlertsScreen(viewModel: AlertsViewModel, onBack: () -> Unit) {
    val state = viewModel.state.value
    val alerts = state.data

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PageBackground)
    ) {
        Header(onBack)

        when {
            state.isLoading -> LoadingBox()
            alerts == null -> ErrorMessage(message = state.message, onRetry = viewModel::load)
            alerts.isEmpty() -> EmptyMessage(message = state.message)
            // Orden del backend (mas reciente primero): no se reordena aqui.
            else -> Surface(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                shape = ListShape,
                color = Color.White,
                border = BorderStroke(1.dp, Divider),
                shadowElevation = 0.dp
            ) {
                LazyColumn {
                    itemsIndexed(alerts, key = { _, alert -> alert.id }) { index, alert ->
                        AlertRow(alert)
                        if (index != alerts.lastIndex) {
                            Box(Modifier.fillMaxWidth().height(1.dp).background(Divider))
                        }
                    }
                }
            }
        }
    }
}

/** Header 64dp + borde inferior 1dp, igual que Historial clinico (US14). */
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
                    contentDescription = "Volver",
                    modifier = Modifier.size(20.dp),
                    tint = TextPrimary
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(text = "Alertas", modifier = Modifier.weight(1f), style = HeaderTitleStyle)
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(Divider))
    }
}

/**
 * Fila del Figma (nodo 6:249): icono de 40dp, "Tipo - mensaje" y debajo riesgo, estado y fecha.
 * Riesgo alto/critico -> icono rojo del Figma; bajo/medio (o desconocido) -> icono ambar. Es solo visual: el nivel
 * se muestra siempre como texto.
 */
@Composable
private fun AlertRow(alert: AlertDto) {
    val typeLabel = alert.trendType?.label
    val riskLabel = alert.riskLevel?.let { "Riesgo ${it.label.lowercase()}" }
    val statusLabel = alert.status?.label
    val dateText = formatAlertDate(alert.createdAt, ZoneId.systemDefault())
    val detail = listOfNotNull(riskLabel, statusLabel, dateText).joinToString(" · ")
    val spoken = listOfNotNull(typeLabel, alert.message, riskLabel, statusLabel, dateText).joinToString(", ")
    val highRisk = alert.riskLevel == RiskLevel.HIGH || alert.riskLevel == RiskLevel.CRITICAL

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .clearAndSetSemantics { contentDescription = spoken },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(if (highRisk) HighRiskBg else WarningBg, IconShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(if (highRisk) R.drawable.ic_alert_octagon else R.drawable.ic_alert_triangle),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = Color.Unspecified // el color viene del vector exportado del Figma
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = buildAnnotatedString {
                    if (typeLabel != null) {
                        withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = TextPrimary)) { append(typeLabel) }
                        append(" - ")
                    }
                    append(alert.message)
                },
                style = TitleStyle
            )
            Text(text = detail, modifier = Modifier.padding(top = 2.dp), style = DetailStyle)
        }
    }
}

/** Sin alertas: muestra el message del backend ("No hay alertas registradas.") o el fallback del ViewModel. */
@Composable
private fun EmptyMessage(message: String) {
    Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Text(text = message, style = EmptyStyle, textAlign = TextAlign.Center)
    }
}
