package com.jamsell.gethics.livestock.presentation.animal_detail

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.Female
import androidx.compose.material.icons.outlined.Male
import androidx.compose.material.icons.outlined.MonitorWeight
import androidx.compose.material.icons.outlined.Pets
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.jamsell.gethics.R
import com.jamsell.gethics.livestock.domain.model.Animal
import com.jamsell.gethics.livestock.domain.model.AnimalSex
import com.jamsell.gethics.livestock.presentation.NO_DATA
import com.jamsell.gethics.livestock.presentation.formatAge
import com.jamsell.gethics.livestock.presentation.formatBirthDate
import com.jamsell.gethics.livestock.presentation.sexLabel
import com.jamsell.gethics.livestock.presentation.animal_list.formatWeight
import com.jamsell.gethics.shared.ui.components.AlertBanner
import com.jamsell.gethics.shared.ui.components.ErrorMessage
import com.jamsell.gethics.shared.ui.components.GethicsButton
import com.jamsell.gethics.shared.ui.components.GethicsLogoMark
import com.jamsell.gethics.shared.ui.components.LoadingBox
import com.jamsell.gethics.shared.ui.theme.FigmaLineHeight
import com.skydoves.landscapist.ImageOptions
import com.skydoves.landscapist.glide.GlideImage
import java.time.LocalDate

// Lenguaje visual de la ficha del animal (pestana "Informacion") del Figma. Colores locales, como en US14: el theme global no cambia.
// Limitaciones: el Figma muestra estado de salud ("Saludable"), sensor, potrero, ubicacion, dieta y las pestanas Salud y
// Vacunas; el backend de Livestock aun no tiene esos datos (la ubicacion necesita un GET de granja por id), asi que no se
// muestran. El historial clinico y los eventos sanitarios siguen accesibles con los botones de abajo. Roboto en lugar de Montserrat.
private val PageBackground = Color(0xFFFAF7F4)
private val TextPrimary = Color(0xFF141D16)
private val TextSecondary = Color(0xFF5D665F)
private val Divider = Color(0xFFE6DFDA)
private val Accent = Color(0xFF733336)
private val IconBackground = Color(0xFFF3E7E2)
private val HeroBackground = Color(0xFFE8F3E8)

private val HeroHeight = 220.dp
private val SummaryOverlap = 44.dp

private val SummaryLineStyle = TextStyle(fontSize = 11.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.5.sp, lineHeightStyle = FigmaLineHeight, color = TextSecondary)
private val TagStyle = TextStyle(fontSize = 24.sp, lineHeight = 32.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.sp, lineHeightStyle = FigmaLineHeight, color = TextPrimary)
private val SectionLabelStyle = TextStyle(fontSize = 11.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.5.sp, lineHeightStyle = FigmaLineHeight, color = TextSecondary)
private val SectionTitleStyle = TextStyle(fontSize = 20.sp, lineHeight = 28.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.sp, lineHeightStyle = FigmaLineHeight, color = TextPrimary)
private val TileLabelStyle = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Normal, letterSpacing = 0.sp, lineHeightStyle = FigmaLineHeight, color = TextSecondary)
private val TileValueStyle = TextStyle(fontSize = 16.sp, lineHeight = 24.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.sp, lineHeightStyle = FigmaLineHeight, color = TextPrimary)

/** "HOLSTEIN · HEMBRA · 4 AÑOS": solo lo que se conoce (sin sexo o sin edad no se escribe "Sin especificar"). */
fun summaryLine(animal: Animal, today: LocalDate): String =
    listOfNotNull(
        animal.breed,
        animal.sex?.let { sexLabel(it) },
        formatAge(animal.birthDate, today).takeIf { it != NO_DATA }
    ).joinToString(" · ").uppercase()

@Composable
fun AnimalDetailScreen(
    viewModel: AnimalDetailViewModel,
    animalId: String,
    onBack: () -> Unit,
    onEdit: (String) -> Unit,
    onOpenClinicalHistory: (String) -> Unit,
    onRegisterEvent: (String) -> Unit,
    onScheduleVaccination: (String) -> Unit
) {
    val state = viewModel.state.value
    val animal = state.data

    // La pantalla vive en el back stack: al volver de editar hay que recargar para ver los cambios.
    LifecycleResumeEffect(viewModel) {
        viewModel.load()
        onPauseOrDispose { }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PageBackground)
    ) {
        when {
            state.isLoading && animal == null -> LoadingBox()
            animal == null -> {
                ErrorMessage(message = state.message, onRetry = { viewModel.load() })
                RoundIconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(16.dp)
                ) { Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = "Volver", modifier = Modifier.size(20.dp), tint = TextPrimary) }
            }
            else -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                Hero(animal = animal, onBack = onBack, onEdit = { onEdit(animalId) })
                // El resumen se solapa SummaryOverlap sobre la foto: este espacio compensa lo que sobresale del hero.
                Spacer(Modifier.height(SummaryOverlap + 24.dp))

                Column(modifier = Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (viewModel.offline.value) {
                        AlertBanner(text = "Sin conexión: mostrando los datos guardados")
                    }
                    Text(text = "PERFIL", style = SectionLabelStyle)
                    Text(text = "Datos generales", style = SectionTitleStyle)
                    GeneralData(animal = animal, today = LocalDate.now())

                    Spacer(Modifier.height(4.dp))
                    GethicsButton(
                        text = "Ver historial clínico",
                        onClick = { onOpenClinicalHistory(animalId) },
                        modifier = Modifier.fillMaxWidth(),
                        outlined = true
                    )
                    GethicsButton(
                        text = "Registrar evento sanitario",
                        onClick = { onRegisterEvent(animalId) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    GethicsButton(
                        text = "Programar vacuna",
                        onClick = { onScheduleVaccination(animalId) },
                        modifier = Modifier.fillMaxWidth(),
                        outlined = true
                    )
                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    }
}

/** Foto (o el toro del logo), botones circulares de volver y editar, y la tarjeta resumen solapada abajo. */
@Composable
private fun Hero(animal: Animal, onBack: () -> Unit, onEdit: () -> Unit) {
    Box(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(HeroHeight)
                .background(HeroBackground),
            contentAlignment = Alignment.Center
        ) {
            if (animal.photoUrl == null) {
                GethicsLogoMark(size = 120.dp, shape = CircleShape)
            } else {
                GlideImage(
                    imageModel = { animal.photoUrl },
                    modifier = Modifier.fillMaxSize(),
                    imageOptions = ImageOptions(contentScale = ContentScale.Crop, contentDescription = "Foto del animal"),
                    failure = { GethicsLogoMark(size = 120.dp, shape = CircleShape) }
                )
            }
        }

        RoundIconButton(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
        ) { Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = "Volver", modifier = Modifier.size(20.dp), tint = TextPrimary) }
        RoundIconButton(
            onClick = onEdit,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        ) { Icon(Icons.Outlined.Edit, contentDescription = "Editar animal", modifier = Modifier.size(20.dp), tint = TextPrimary) }

        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 16.dp)
                .offset(y = SummaryOverlap),
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            shadowElevation = 6.dp
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp)) {
                Text(text = summaryLine(animal, LocalDate.now()), style = SummaryLineStyle)
                Spacer(Modifier.height(8.dp))
                Text(text = "Tag ID #${animal.tag}", style = TagStyle)
            }
        }
    }
}

@Composable
private fun RoundIconButton(onClick: () -> Unit, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Surface(modifier = modifier, shape = CircleShape, color = Color.White.copy(alpha = 0.92f), shadowElevation = 2.dp) {
        IconButton(onClick = onClick, modifier = Modifier.size(40.dp), content = content)
    }
}

/** Cuadricula de dos columnas: Raza, Edad, Genero, Nacimiento, Peso y Codigo QR. */
@Composable
private fun GeneralData(animal: Animal, today: LocalDate) {
    val genderIcon = if (animal.sex == AnimalSex.MALE.name) Icons.Outlined.Male else Icons.Outlined.Female
    val tiles = listOf(
        Tile(Icons.Outlined.Pets, "Raza", animal.breed),
        Tile(Icons.Outlined.CalendarMonth, "Edad", formatAge(animal.birthDate, today)),
        Tile(genderIcon, "Género", sexLabel(animal.sex)),
        Tile(Icons.Outlined.Event, "Nacimiento", formatBirthDate(animal.birthDate)),
        Tile(Icons.Outlined.MonitorWeight, "Peso", animal.weightKg?.let { "${formatWeight(it)} kg" } ?: "Sin registrar"),
        Tile(Icons.Outlined.QrCode2, "Código QR", animal.qrCode ?: NO_DATA)
    )
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        tiles.chunked(2).forEach { row ->
            Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { tile -> InfoTile(tile, Modifier.weight(1f).fillMaxHeight()) }
            }
        }
    }
}

private data class Tile(val icon: ImageVector, val label: String, val value: String)

@Composable
private fun InfoTile(tile: Tile, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, shape = RoundedCornerShape(16.dp), color = Color.White, border = BorderStroke(1.dp, Divider)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Surface(shape = RoundedCornerShape(10.dp), color = IconBackground) {
                Icon(tile.icon, contentDescription = null, modifier = Modifier.padding(8.dp).size(20.dp), tint = Accent)
            }
            Spacer(Modifier.height(12.dp))
            Text(text = tile.label, style = TileLabelStyle)
            Text(text = tile.value, style = TileValueStyle)
        }
    }
}
