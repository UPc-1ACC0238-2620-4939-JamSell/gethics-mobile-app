package com.jamsell.gethics.livestock.presentation.animal_register

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.LocalOffer
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jamsell.gethics.shared.ui.components.GethicsButton
import com.jamsell.gethics.shared.ui.theme.Mint
import com.skydoves.landscapist.ImageOptions
import com.skydoves.landscapist.glide.GlideImage
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

// Figma: "Registrar Nueva Res" (390w light). Campos de 18dp de radio, boton circular de 56dp, como en Sign In.
private val FieldShape = RoundedCornerShape(18.dp)
private val PhotoShape = RoundedCornerShape(16.dp)
private val CameraGreen = Color(0xFF2D6A3E)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnimalRegisterScreen(viewModel: AnimalRegisterViewModel, onBack: () -> Unit, onSaved: () -> Unit) {
    val state = viewModel.state.value
    val errors = viewModel.errors.value
    var tag by rememberSaveable { mutableStateOf("") }
    var breed by rememberSaveable { mutableStateOf("") }
    var birthDate by rememberSaveable { mutableStateOf<LocalDate?>(null) }
    var weight by rememberSaveable { mutableStateOf("") }
    var photoUri by rememberSaveable { mutableStateOf<String?>(null) }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }

    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) photoUri = uri.toString()
    }

    LaunchedEffect(state.data) {
        if (state.data == true) onSaved()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
            }
            Text(
                text = "Registrar Nueva Res",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 24.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            LabeledField("Número de Arete (Tag)") {
                OutlinedTextField(
                    value = tag,
                    onValueChange = { tag = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Ej. MX-00123") },
                    leadingIcon = { Icon(Icons.Outlined.LocalOffer, contentDescription = null) },
                    singleLine = true,
                    isError = errors.tag != null,
                    supportingText = errors.tag?.let { { Text(it) } },
                    shape = FieldShape
                )
            }

            LabeledField("Raza") {
                var expanded by rememberSaveable { mutableStateOf(false) }
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                    OutlinedTextField(
                        value = breed,
                        onValueChange = {},
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                        readOnly = true,
                        placeholder = { Text("Selecciona una raza") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        isError = errors.breed != null,
                        supportingText = errors.breed?.let { { Text(it) } },
                        shape = FieldShape
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        ANIMAL_BREEDS.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    breed = option
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                LabeledField("Fecha de Nacimiento", Modifier.weight(1f)) {
                    // Campo de solo lectura: el toque abre el DatePicker (el clickable va sobre un Box transparente).
                    Box {
                        OutlinedTextField(
                            value = birthDate?.let { "%02d/%02d/%d".format(it.monthValue, it.dayOfMonth, it.year) }.orEmpty(),
                            onValueChange = {},
                            modifier = Modifier.fillMaxWidth(),
                            readOnly = true,
                            placeholder = { Text("mm/dd/yyyy") },
                            trailingIcon = { Icon(Icons.Outlined.CalendarMonth, contentDescription = null) },
                            singleLine = true,
                            isError = errors.birthDate != null,
                            supportingText = errors.birthDate?.let { { Text(it) } },
                            shape = FieldShape
                        )
                        Box(
                            Modifier
                                .matchParentSize()
                                .clickable(role = Role.Button, onClickLabel = "Elegir fecha") { showDatePicker = true }
                        )
                    }
                }
                LabeledField("Peso Inicial", Modifier.weight(1f)) {
                    OutlinedTextField(
                        value = weight,
                        onValueChange = { weight = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("0.0") },
                        suffix = { Text("kg") },
                        singleLine = true,
                        isError = errors.weight != null,
                        supportingText = errors.weight?.let { { Text(it) } },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = FieldShape
                    )
                }
            }

            LabeledField("Foto") {
                PhotoPicker(photoUri = photoUri, onClick = {
                    photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                })
            }

            if (state.message.isNotEmpty()) {
                Text(text = state.message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
            }
        }

        GethicsButton(
            text = "Guardar Registro",
            onClick = { viewModel.save(tag, breed, birthDate, weight, photoUri) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            isLoading = state.isLoading,
            height = 56.dp,
            shape = CircleShape
        )
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

/** Etiqueta encima del campo (el Figma no usa label flotante). */
@Composable
private fun LabeledField(label: String, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
        content()
    }
}

/** Caja punteada verde: sin foto muestra el icono de camara; con foto, la vista previa. */
@Composable
private fun PhotoPicker(photoUri: String?, onClick: () -> Unit) {
    val dashColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.35f)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(192.dp)
            .clip(PhotoShape)
            .background(Mint.copy(alpha = 0.6f))
            .drawBehind {
                drawRoundRect(
                    color = dashColor,
                    size = Size(size.width, size.height),
                    cornerRadius = CornerRadius(16.dp.toPx()),
                    style = Stroke(width = 1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f)))
                )
            }
            .clickable(role = Role.Button, onClickLabel = "Subir foto", onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (photoUri != null) {
            GlideImage(
                imageModel = { photoUri },
                modifier = Modifier.fillMaxSize(),
                imageOptions = ImageOptions(contentScale = ContentScale.Crop, contentDescription = "Foto de la res")
            )
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(Mint, CircleShape)
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.PhotoCamera, contentDescription = null, tint = CameraGreen)
                }
                Text("Subir foto de la vaca", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
                Text(
                    text = "Toca para tomar una foto o elegir de la galería",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            }
        }
    }
}
