package com.jamsell.gethics.livestock.presentation.animal_edit

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jamsell.gethics.R
import com.jamsell.gethics.livestock.domain.model.Animal
import com.jamsell.gethics.livestock.domain.model.AnimalSex
import com.jamsell.gethics.livestock.presentation.NO_SEX
import com.jamsell.gethics.livestock.presentation.animal_register.ANIMAL_BREEDS
import com.jamsell.gethics.livestock.presentation.formatBirthDate
import com.jamsell.gethics.shared.common.UIState
import com.jamsell.gethics.shared.ui.components.ErrorMessage
import com.jamsell.gethics.shared.ui.components.GethicsButton
import com.jamsell.gethics.shared.ui.components.LoadingBox
import com.jamsell.gethics.shared.ui.theme.FigmaLineHeight
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

// Lenguaje visual del frame "Editar animal" del Figma: cada campo es una tarjeta con su titulo. Colores locales, como en US14.
// Limitaciones: el Figma incluye un selector de "Granja", pero la granja se cambia con la asociacion animal-granja (US10) y
// aun no hay forma de listar granjas desde el movil (necesita el id del usuario de IAM), asi que no se muestra. El arete
// es identidad del animal: se muestra pero no se edita. El formulario no pide peso ni foto: conservan su valor.
private val PageBackground = Color.White
private val TextPrimary = Color(0xFF141D16)
private val TextSecondary = Color(0xFF5D665F)
private val Divider = Color(0xFFD9D9D9)
private val Error = Color(0xFFB3261E)

private val CardShape = RoundedCornerShape(12.dp)

private val TitleStyle = TextStyle(fontSize = 24.sp, lineHeight = 32.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.sp, lineHeightStyle = FigmaLineHeight, color = TextPrimary)
private val FieldTitleStyle = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.sp, lineHeightStyle = FigmaLineHeight, color = TextPrimary)
private val FieldValueStyle = TextStyle(fontSize = 13.sp, lineHeight = 20.sp, fontWeight = FontWeight.Normal, letterSpacing = 0.sp, lineHeightStyle = FigmaLineHeight, color = TextSecondary)
private val ErrorStyle = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Normal, letterSpacing = 0.sp, lineHeightStyle = FigmaLineHeight, color = Error)

@Composable
fun AnimalEditScreen(viewModel: AnimalEditViewModel, onBack: () -> Unit, onSaved: () -> Unit) {
    val animalState = viewModel.animal.value
    val saveState = viewModel.saveState.value
    val animal = animalState.data

    LaunchedEffect(saveState.data) {
        if (saveState.data == true) onSaved()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PageBackground)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, end = 16.dp, top = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = "Volver", modifier = Modifier.size(20.dp), tint = TextPrimary)
            }
            Spacer(Modifier.width(8.dp))
            Text(text = "Editar animal", style = TitleStyle)
        }

        when {
            animalState.isLoading -> LoadingBox()
            animal == null -> ErrorMessage(message = animalState.message, onRetry = { viewModel.load() })
            else -> EditForm(
                animal = animal,
                errors = viewModel.errors.value,
                saveState = saveState,
                onSave = viewModel::save,
                onCancel = onBack
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditForm(
    animal: Animal,
    errors: AnimalEditErrors,
    saveState: UIState<Boolean>,
    onSave: (name: String, breed: String, sex: String?, birthDate: LocalDate?) -> Unit,
    onCancel: () -> Unit
) {
    // Se precarga una sola vez por animal. Sin nombre en el backend, la lista usa el arete: el campo se muestra vacio.
    var name by rememberSaveable(animal.id) { mutableStateOf(if (animal.name == animal.tag) "" else animal.name) }
    var breed by rememberSaveable(animal.id) { mutableStateOf(animal.breed) }
    var sex by rememberSaveable(animal.id) { mutableStateOf(animal.sex) }
    var birthDate by rememberSaveable(animal.id) {
        mutableStateOf(animal.birthDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() })
    }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        FieldCard(title = "Identificador / arete") {
            Text(text = animal.tag, style = FieldValueStyle)
            Text(text = "El arete identifica al animal y no se puede cambiar.", style = FieldValueStyle)
        }

        FieldCard(title = "Nombre (opcional)") {
            TextField(
                value = name,
                onValueChange = { name = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Ej. Luna") },
                singleLine = true,
                colors = transparentFieldColors()
            )
        }

        // Si la raza guardada no esta en el catalogo del selector, se agrega para no perderla al abrir el formulario.
        DropdownCard(
            title = "Raza",
            selectedLabel = breed,
            placeholder = "Selecciona una raza",
            options = (ANIMAL_BREEDS + animal.breed).distinct().map { it to it },
            onSelect = { breed = it },
            error = errors.breed
        )

        FieldCard(
            title = "Nacimiento",
            error = errors.birthDate,
            modifier = Modifier.clickable(role = Role.Button, onClickLabel = "Elegir fecha") { showDatePicker = true }
        ) {
            Text(text = birthDate?.toString()?.let(::formatBirthDate) ?: "Selecciona una fecha", style = FieldValueStyle)
        }

        DropdownCard(
            title = "Sexo",
            selectedLabel = AnimalSex.entries.firstOrNull { it.name == sex }?.label.orEmpty(),
            placeholder = NO_SEX,
            options = AnimalSex.entries.map { it.label to it.name },
            onSelect = { sex = it }
        )

        if (saveState.message.isNotEmpty()) {
            Text(text = saveState.message, style = ErrorStyle)
        }

        GethicsButton(
            text = "Guardar cambios",
            onClick = { onSave(name, breed, sex, birthDate) },
            modifier = Modifier.fillMaxWidth(),
            isLoading = saveState.isLoading,
            height = 56.dp
        )
        // Escenario 2: Cancelar descarta los cambios y vuelve a la vista anterior.
        GethicsButton(
            text = "Cancelar",
            onClick = onCancel,
            modifier = Modifier.fillMaxWidth(),
            outlined = true,
            enabled = !saveState.isLoading,
            height = 56.dp
        )
        Spacer(Modifier.heightIn(min = 16.dp))
    }

    if (showDatePicker) {
        // El DatePicker trabaja en milisegundos UTC: convertir con UTC para no correr la fecha un dia.
        // Se permiten fechas futuras a proposito: la validacion ocurre al guardar.
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = birthDate?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let {
                        birthDate = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()
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

/** Tarjeta blanca con borde y el titulo en negrita; el contenido es el valor del campo y, si hay, su error. */
@Composable
private fun FieldCard(
    title: String,
    modifier: Modifier = Modifier,
    error: String? = null,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = CardShape,
        color = Color.White,
        border = BorderStroke(1.dp, if (error != null) Error else Divider)
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text(text = title, style = FieldTitleStyle)
            content()
            if (error != null) Text(text = error, style = ErrorStyle, modifier = Modifier.padding(top = 4.dp))
        }
    }
}

/** Tarjeta con selector: options son pares (texto que se ve, valor que se guarda). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DropdownCard(
    title: String,
    selectedLabel: String,
    placeholder: String,
    options: List<Pair<String, String>>,
    onSelect: (String) -> Unit,
    error: String? = null
) {
    var expanded by rememberSaveable { mutableStateOf(false) }

    FieldCard(title = title, error = error) {
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
            TextField(
                value = selectedLabel,
                onValueChange = {},
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
                readOnly = true,
                placeholder = { Text(placeholder) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                colors = transparentFieldColors()
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEach { (label, value) ->
                    DropdownMenuItem(
                        text = { Text(label) },
                        onClick = {
                            onSelect(value)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

/** Campo sin fondo ni linea inferior: la tarjeta que lo contiene ya hace de borde. */
@Composable
private fun transparentFieldColors() = TextFieldDefaults.colors(
    focusedContainerColor = Color.Transparent,
    unfocusedContainerColor = Color.Transparent,
    disabledContainerColor = Color.Transparent,
    errorContainerColor = Color.Transparent,
    focusedIndicatorColor = Color.Transparent,
    unfocusedIndicatorColor = Color.Transparent,
    disabledIndicatorColor = Color.Transparent,
    errorIndicatorColor = Color.Transparent
)
