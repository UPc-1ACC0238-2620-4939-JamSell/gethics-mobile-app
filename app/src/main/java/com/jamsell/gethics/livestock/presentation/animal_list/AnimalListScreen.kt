package com.jamsell.gethics.livestock.presentation.animal_list

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.MonitorWeight
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.jamsell.gethics.livestock.domain.model.Animal
import com.jamsell.gethics.livestock.domain.model.AnimalStatus
import com.jamsell.gethics.shared.ui.components.AlertBanner
import com.jamsell.gethics.shared.ui.components.EmptyState
import com.jamsell.gethics.shared.ui.components.ErrorMessage
import com.jamsell.gethics.shared.ui.components.GethicsLogoMark
import com.jamsell.gethics.shared.ui.components.LoadingBox
import com.jamsell.gethics.shared.ui.theme.FigmaLineHeight
import com.skydoves.landscapist.ImageOptions
import com.skydoves.landscapist.glide.GlideImage
import java.util.Locale

// Lenguaje visual del frame "Inventario de Ganado" (390w light) del Figma. Colores locales, como en US14: el theme global no cambia.
// Limitaciones: el Figma muestra estado de salud (Sano/Enfermo), "requieren atencion" y la campana de notificaciones; el backend
// de Livestock no tiene esos datos todavia, asi que no se muestran. El Figma usa Montserrat; aqui se mantiene Roboto.
private val PageBackground = Color(0xFFF8FBF8)
private val TextPrimary = Color(0xFF141D16)
private val TextSecondary = Color(0xFF5D665F)
private val Divider = Color(0xFFDDE3DD)
private val Accent = Color(0xFF733336)

private val CardShape = RoundedCornerShape(14.dp)
private val FieldShape = RoundedCornerShape(12.dp)

private val TitleStyle = TextStyle(fontSize = 18.sp, lineHeight = 28.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.sp, lineHeightStyle = FigmaLineHeight, color = TextPrimary)
private val SubtitleStyle = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Normal, letterSpacing = 0.sp, lineHeightStyle = FigmaLineHeight, color = TextSecondary)
private val SectionLabelStyle = TextStyle(fontSize = 11.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.5.sp, lineHeightStyle = FigmaLineHeight, color = TextSecondary)
private val TagStyle = TextStyle(fontSize = 16.sp, lineHeight = 24.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.sp, lineHeightStyle = FigmaLineHeight, color = TextPrimary)
private val DetailStyle = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Normal, letterSpacing = 0.sp, lineHeightStyle = FigmaLineHeight, color = TextSecondary)

/** "542" si es entero, "542.5" si tiene decimales. */
fun formatWeight(kg: Double): String =
    if (kg % 1.0 == 0.0) kg.toLong().toString() else String.format(Locale.US, "%.1f", kg)

private fun countLabel(count: Int): String = if (count == 1) "1 animal" else "$count animales"

private fun resultsLabel(count: Int): String = if (count == 1) "1 RESULTADO" else "$count RESULTADOS"

@Composable
fun AnimalListScreen(viewModel: AnimalListViewModel, onAnimalClick: (String) -> Unit, onAddAnimal: () -> Unit) {
    val state = viewModel.state.value
    val animals = state.data
    val query = viewModel.query.value
    val status = viewModel.status.value

    // La pantalla vive en el back stack: al volver del registro hay que refrescar para ver el animal nuevo.
    LifecycleResumeEffect(viewModel) {
        viewModel.load()
        onPauseOrDispose { }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PageBackground)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Header(total = viewModel.total.value)
            SearchRow(
                query = query,
                onQueryChange = viewModel::onQueryChange,
                status = status,
                onStatusChange = viewModel::onStatusChange
            )
            if (viewModel.offline.value) {
                AlertBanner(
                    text = "Sin conexión: mostrando los datos guardados",
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp)
                )
            }

            when {
                state.isLoading && animals == null -> LoadingBox()
                animals == null -> ErrorMessage(message = state.message, onRetry = { viewModel.load() })
                animals.isEmpty() -> EmptyState(message = state.message)
                else -> {
                    Text(
                        text = resultsLabel(animals.size),
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
                        style = SectionLabelStyle
                    )
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        // Espacio final para que el boton flotante no tape la ultima tarjeta
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 88.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(animals, key = { it.id }) { animal ->
                            AnimalCard(animal = animal, onClick = { onAnimalClick(animal.id) })
                        }
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = onAddAnimal,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            containerColor = Accent,
            contentColor = Color.White
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Registrar animal")
        }
    }
}

/** Logo + titulo + total de animales del hato (sin "requieren atencion": no hay datos de salud). */
@Composable
private fun Header(total: Int?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        GethicsLogoMark(size = 40.dp, shape = CircleShape)
        Spacer(Modifier.width(12.dp))
        Column {
            Text(text = "Inventario de Ganado", style = TitleStyle)
            if (total != null) Text(text = countLabel(total), style = SubtitleStyle)
        }
    }
}

/** Buscador por arete o raza + boton de filtro por estado. */
@Composable
private fun SearchRow(
    query: String,
    onQueryChange: (String) -> Unit,
    status: AnimalStatus,
    onStatusChange: (AnimalStatus) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.weight(1f),
            placeholder = { Text("Buscar por arete o raza") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            trailingIcon = if (query.isNotEmpty()) {
                {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(Icons.Filled.Clear, contentDescription = "Borrar búsqueda")
                    }
                }
            } else null,
            singleLine = true,
            shape = FieldShape,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                unfocusedBorderColor = Divider
            )
        )
        StatusFilter(status = status, onStatusChange = onStatusChange)
    }
}

/** Boton de filtro: abre un menu con los estados. Cambia de color cuando hay un filtro distinto al de por defecto. */
@Composable
private fun StatusFilter(status: AnimalStatus, onStatusChange: (AnimalStatus) -> Unit) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val filtering = status != AnimalStatus.ACTIVE

    Box {
        Surface(
            shape = FieldShape,
            color = if (filtering) Accent else Color.White,
            border = BorderStroke(1.dp, if (filtering) Accent else Divider)
        ) {
            IconButton(onClick = { expanded = true }, modifier = Modifier.size(56.dp)) {
                Icon(
                    Icons.Filled.Tune,
                    contentDescription = "Filtrar por estado: ${status.label}",
                    tint = if (filtering) Color.White else TextPrimary
                )
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            AnimalStatus.entries.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.label) },
                    onClick = {
                        expanded = false
                        onStatusChange(option)
                    },
                    trailingIcon = if (option == status) {
                        { Icon(Icons.Filled.Check, contentDescription = "Seleccionado", tint = Accent) }
                    } else null
                )
            }
        }
    }
}

/** Avatar + arete + raza y peso + flecha. */
@Composable
private fun AnimalCard(animal: Animal, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = CardShape,
        color = Color.White,
        border = BorderStroke(1.dp, Divider),
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Avatar(photoUrl = animal.photoUrl)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = animal.tag, style = TagStyle)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = animal.breed, style = DetailStyle)
                    animal.weightKg?.let { kg ->
                        Text(text = "  ·  ", style = DetailStyle)
                        Icon(
                            Icons.Outlined.MonitorWeight,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = TextSecondary
                        )
                        Text(text = " ${formatWeight(kg)} kg", style = DetailStyle)
                    }
                }
            }
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = TextSecondary
            )
        }
    }
}

/** Foto del animal en circulo; sin foto (o si no carga) se muestra el toro del logo. */
@Composable
private fun Avatar(photoUrl: String?) {
    val ringModifier = Modifier.border(1.dp, Divider, CircleShape)
    if (photoUrl == null) {
        GethicsLogoMark(modifier = ringModifier, size = 56.dp, shape = CircleShape)
    } else {
        GlideImage(
            imageModel = { photoUrl },
            modifier = ringModifier
                .size(56.dp)
                .clip(CircleShape),
            imageOptions = ImageOptions(contentScale = ContentScale.Crop),
            failure = { GethicsLogoMark(size = 56.dp, shape = CircleShape) }
        )
    }
}
