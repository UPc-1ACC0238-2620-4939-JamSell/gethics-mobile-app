package com.jamsell.gethics.sanitary.presentation.register_event

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jamsell.gethics.R
import com.jamsell.gethics.sanitary.data.SanitaryEventType
import com.jamsell.gethics.shared.ui.components.GethicsButton
import com.jamsell.gethics.shared.ui.theme.FigmaLineHeight
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

// Lenguaje visual del formulario interno del Figma "Registrar Nueva Res" (frame 12:196). NO existe un frame de US11:
// se toman de ahi header, labels, campos, selector, fecha y barra inferior. Colores locales: el theme global no cambia.
// Limitacion: ese frame usa Montserrat/Inter; aqui se mantiene Roboto con los mismos tamanos, pesos e interlineados.
private val TextPrimary = Color(0xFF141D16)
private val TextSecondary = Color(0xFF636B64)
private val FieldBorder = Color(0xFFD3DAD3)
private val BarDivider = Color(0xFFDAE0DA)
private val ButtonText = Color(0xFFFCFCFC)

private val ScreenHorizontalPadding = 20.dp
private val FieldShape = RoundedCornerShape(14.dp)
private val FieldHeight = 48.dp

private val TitleStyle = TextStyle(fontSize = 18.sp, lineHeight = 28.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.sp, lineHeightStyle = FigmaLineHeight, color = TextPrimary)
private val LabelStyle = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.sp, lineHeightStyle = FigmaLineHeight, color = TextPrimary)
private val InputStyle = TextStyle(fontSize = 16.sp, lineHeight = 24.sp, fontWeight = FontWeight.Normal, letterSpacing = 0.sp, lineHeightStyle = FigmaLineHeight, color = TextPrimary)
private val ButtonTextStyle = TextStyle(fontSize = 16.sp, lineHeight = 24.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.sp, lineHeightStyle = FigmaLineHeight, color = ButtonText)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterEventScreen(viewModel: RegisterEventViewModel, animalId: String, onSaved: () -> Unit, onBack: () -> Unit) {
    val state = viewModel.state.value
    var type by rememberSaveable { mutableStateOf(SanitaryEventType.VACCINATION) }
    var date by rememberSaveable { mutableStateOf(LocalDate.now()) }
    var description by rememberSaveable { mutableStateOf("") }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(state.data) {
        if (state.data == true) onSaved()
    }

    Column(modifier = Modifier.fillMaxSize().imePadding()) {
        Header(onBack)

        // Con scroll: pantallas chicas, teclado abierto o fuente escalada. La barra del boton queda siempre visible.
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = ScreenHorizontalPadding)
                .padding(top = 24.dp, bottom = 16.dp)
        ) {
            EventTypeField(selected = type, onSelect = { type = it })

            Spacer(Modifier.height(20.dp))
            FieldLabel("Fecha del evento")
            Spacer(Modifier.height(8.dp))
            DateField(date = date, onClick = { showDatePicker = true })

            Spacer(Modifier.height(20.dp))
            Column(modifier = Modifier.semantics(mergeDescendants = true) {}) {
                FieldLabel("Observaciones (opcional)")
                Spacer(Modifier.height(8.dp))
                DescriptionField(value = description, onValueChange = { description = it })
            }

            // No existe en el Figma: muestra la validacion local de fecha futura o el message del backend.
            if (state.message.isNotEmpty()) {
                Text(
                    text = state.message,
                    modifier = Modifier.padding(top = 16.dp),
                    style = InputStyle.copy(fontSize = 14.sp, lineHeight = 20.sp, color = MaterialTheme.colorScheme.error)
                )
            }
        }

        BottomBar {
            GethicsButton(
                text = "Registrar evento",
                onClick = { viewModel.save(animalId, type, date, description) },
                modifier = Modifier.fillMaxWidth(),
                isLoading = state.isLoading,
                height = 56.dp,
                shape = FieldShape,
                textStyle = ButtonTextStyle,
                elevation = 1.dp // aproxima la sombra doble del Figma (Android no permite fijar el blur)
            )
        }
    }

    if (showDatePicker) {
        // El DatePicker trabaja en milisegundos UTC: convertir con UTC para no correr la fecha un dia.
        // Se permiten fechas futuras a proposito: la validacion ocurre al guardar (escenario 2).
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let {
                        date = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()
                    }
                    showDatePicker = false
                }) { Text("Aceptar") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancelar") }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }
}

/** Header 64dp + borde inferior 1dp: flecha de 20dp en x 26 y titulo en x 68 (IconButton de 48dp = area tactil). */
@Composable
private fun Header(onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().background(Color.White)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 64.dp) // crece si la fuente escalada obliga a partir el titulo
                .padding(start = 12.dp, end = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                // Misma flecha de Lucide que el registro: a 20dp su geometria coincide con la del nodo 12:256
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_back),
                    contentDescription = "Volver",
                    modifier = Modifier.size(20.dp),
                    tint = TextPrimary
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(text = "Registrar evento sanitario", modifier = Modifier.weight(1f), style = TitleStyle)
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(BarDivider))
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(text = text, style = LabelStyle)
}

/** Caja de 48dp minimo (crece con la fuente escalada) con radio 14, borde 1dp y fondo blanco; contenido a 17dp del borde.
 *  endPadding: el Figma deja el chevron a 16dp del borde derecho y el calendario a 12dp. */
@Composable
private fun FieldBox(modifier: Modifier = Modifier, endPadding: Dp = 17.dp, content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = FieldHeight)
            .background(Color.White, FieldShape)
            .border(BorderStroke(1.dp, FieldBorder), FieldShape)
            .padding(start = 17.dp, top = 12.dp, end = endPadding, bottom = 12.dp), // 12 + 24 (interlineado) + 12 = 48
        verticalAlignment = Alignment.CenterVertically,
        content = content
    )
}

/** Selector desplegable: exactamente los 5 tipos que acepta el backend (VACCINATION, TREATMENT, DISEASE, CHECKUP, OTHER). */
@Composable
private fun EventTypeField(selected: SanitaryEventType, onSelect: (SanitaryEventType) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    var fieldWidthPx by remember { mutableIntStateOf(0) }

    FieldLabel("Tipo de evento")
    Spacer(Modifier.height(8.dp))
    Box {
        FieldBox(
            endPadding = 16.dp,
            modifier = Modifier
                .onSizeChanged { fieldWidthPx = it.width }
                .clickable(role = Role.DropdownList, onClickLabel = "Elegir tipo de evento") { expanded = true }
                .semantics { contentDescription = "Tipo de evento, ${selected.label}" }
        ) {
            Text(text = selected.label, modifier = Modifier.weight(1f), style = InputStyle)
            Icon(painterResource(R.drawable.ic_chevron_down), contentDescription = null, tint = TextSecondary)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.width(with(LocalDensity.current) { fieldWidthPx.toDp() }),
            containerColor = Color.White,
            shape = FieldShape
        ) {
            SanitaryEventType.entries.forEach { option ->
                DropdownMenuItem(
                    text = { Text(text = option.label, style = InputStyle) },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    },
                    modifier = Modifier.semantics { this.selected = option == selected }
                )
            }
        }
    }
}

/** Campo de fecha: abre el mismo DatePicker de siempre. Muestra la fecha tal cual (yyyy-MM-dd) con el icono de calendario. */
@Composable
private fun DateField(date: LocalDate, onClick: () -> Unit) {
    FieldBox(
        endPadding = 12.dp,
        modifier = Modifier
            .clickable(role = Role.Button, onClickLabel = "Elegir fecha", onClick = onClick)
            .semantics { contentDescription = "Fecha del evento, $date" }
    ) {
        Text(text = date.toString(), modifier = Modifier.weight(1f), style = InputStyle)
        Icon(painterResource(R.drawable.ic_calendar), contentDescription = null, tint = TextSecondary)
    }
}

@Composable
private fun DescriptionField(value: String, onValueChange: (String) -> Unit) {
    FieldBox {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f),
            singleLine = true,
            textStyle = InputStyle,
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary)
        )
    }
}

/** Barra inferior fija del formulario: borde superior 1dp, 16dp sobre y bajo el boton, margenes de 20dp. */
@Composable
private fun BottomBar(content: @Composable () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().background(Color.White)) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(BarDivider))
        Box(modifier = Modifier.padding(start = ScreenHorizontalPadding, end = ScreenHorizontalPadding, top = 16.dp, bottom = 16.dp)) {
            content()
        }
    }
}
