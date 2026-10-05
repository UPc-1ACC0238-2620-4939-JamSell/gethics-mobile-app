package com.jamsell.gethics.iam.presentation.sign_up

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jamsell.gethics.R
import com.jamsell.gethics.iam.domain.model.Role
import com.jamsell.gethics.shared.ui.components.GethicsButton
import com.jamsell.gethics.shared.ui.components.GethicsLogoMark
import com.jamsell.gethics.shared.ui.components.GethicsTextField
import com.jamsell.gethics.shared.ui.theme.FigmaLineHeight

// Valores del Figma oficial: Crear Cuenta, frame 4:143 ("390w light"). Medidas logicas en dp, texto en sp.
// Este frame usa su propia paleta: se aplica solo en esta pantalla, el theme global no cambia.
private val SecondaryText = Color(0xFF5F6A5F) // subtitulo, labels, iconos, helper, footer
private val FieldBorder = Color(0xFF737B72)
private val TitleColor = Color(0xFF0A0A0A)
private val ButtonText = Color(0xFFFFFFFF)

private val ScreenHorizontalPadding = 24.dp
private val FieldShape = RoundedCornerShape(6.dp)
private val OutlinedLabelSpace = 8.dp // OutlinedTextField reserva 8dp sobre el borde para el label flotante
private val TitleStyle = TextStyle(fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.Normal, letterSpacing = 0.sp, lineHeightStyle = FigmaLineHeight)
private val ButtonTextStyle = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.35.sp, color = ButtonText)

@Composable
fun SignUpScreen(viewModel: SignUpViewModel, onBack: () -> Unit) {
    val state = viewModel.state.value
    var name by rememberSaveable { mutableStateOf("") }
    var role by rememberSaveable { mutableStateOf<Role?>(null) } // sin rol por defecto
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }

    // 201: vuelve al login (el registro no inicia sesion)
    LaunchedEffect(state.data) {
        if (state.data == true) onBack()
    }

    val typography = MaterialTheme.typography
    MaterialTheme(
        colorScheme = MaterialTheme.colorScheme.copy(onSurfaceVariant = SecondaryText, outline = FieldBorder),
        // Figma: texto de campos 16/24 y helper 12/16, ambos sin letter-spacing (M3 trae 0.5 y 0.4)
        typography = typography.copy(
            bodyLarge = typography.bodyLarge.copy(letterSpacing = 0.sp),
            bodySmall = typography.bodySmall.copy(letterSpacing = 0.sp)
        )
    ) {
        BoxWithConstraints {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .heightIn(min = maxHeight)
                    .imePadding(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Header(onBack)
                    Column(modifier = Modifier.padding(horizontal = ScreenHorizontalPadding)) {
                        GethicsLogoMark(Modifier.align(Alignment.CenterHorizontally))
                        Spacer(Modifier.height(28.dp))
                        Text(
                            text = "Monitorea la salud de tu ganado en tiempo real.",
                            modifier = Modifier.fillMaxWidth(),
                            style = MaterialTheme.typography.bodyMedium.copy(letterSpacing = 0.sp, lineHeightStyle = FigmaLineHeight),
                            color = SecondaryText,
                            textAlign = TextAlign.Center
                        )

                        Spacer(Modifier.height(32.dp - OutlinedLabelSpace))
                        GethicsTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = "Nombre Completo",
                            modifier = Modifier.fillMaxWidth(),
                            leadingIcon = { FieldIcon(R.drawable.ic_person) },
                            shape = FieldShape
                        )
                        // El rol ocupa el lugar de "Nombre del Fundo/Granja" del Figma (US09, sin contrato en el registro)
                        Spacer(Modifier.height(20.dp))
                        RoleSelector(selected = role, onSelect = { role = it })
                        Spacer(Modifier.height(20.dp - OutlinedLabelSpace))
                        GethicsTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = "Correo Electrónico",
                            modifier = Modifier.fillMaxWidth(),
                            keyboardType = KeyboardType.Email,
                            leadingIcon = { FieldIcon(R.drawable.ic_mail) },
                            shape = FieldShape
                        )
                        Spacer(Modifier.height(20.dp - OutlinedLabelSpace))
                        GethicsTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = "Contraseña",
                            modifier = Modifier.fillMaxWidth(),
                            isPassword = true,
                            leadingIcon = { FieldIcon(R.drawable.ic_lock) },
                            shape = FieldShape,
                            helperText = "Mínimo 8 caracteres" // informativo: la regla la valida el backend
                        )

                        // No existe en el Figma: muestra el message del backend (409, 400, sin conexion).
                        if (state.message.isNotEmpty()) {
                            Text(
                                text = state.message,
                                modifier = Modifier.padding(top = 8.dp),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }

                Column(modifier = Modifier.padding(horizontal = ScreenHorizontalPadding)) {
                    Spacer(Modifier.height(112.dp)) // hueco del Figma entre el helper y el boton
                    GethicsButton(
                        text = "Registrarse",
                        onClick = { viewModel.signUp(name, email, password, role) },
                        modifier = Modifier.fillMaxWidth(),
                        isLoading = state.isLoading,
                        height = 48.dp,
                        shape = CircleShape,
                        textStyle = ButtonTextStyle,
                        elevation = 1.dp, // aproxima la sombra doble del Figma (Android no permite fijar el blur)
                        border = BorderStroke(1.dp, Color.Black)
                    )
                    // En el Figma la caja del footer mide 16 (interlineado 20): se quitan 2dp arriba y abajo.
                    Spacer(Modifier.height(18.dp - 2.dp))
                    SignInFooter(onBack)
                    Spacer(Modifier.height(34.dp - 2.dp))
                }
            }
        }
    }
}

/** Flecha "Volver" (24dp en x 28) + "Crear Cuenta" (x 68), en una franja de 64dp. IconButton mide 48dp (area tactil). */
@Composable
private fun Header(onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .padding(start = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = "Volver", tint = TitleColor)
        }
        Spacer(Modifier.width(4.dp))
        Text(text = "Crear Cuenta", style = TitleStyle, color = TitleColor)
    }
}

/** Icono de 20dp a 15dp del borde del campo, como en el Figma. */
@Composable
private fun FieldIcon(@DrawableRes id: Int) {
    Icon(painterResource(id), contentDescription = null, modifier = Modifier.padding(start = 3.6.dp))
}

/** Dos chips con el mismo alto, radio y borde que los campos. Exactamente un rol seleccionado (ninguno al inicio). */
@Composable
private fun RoleSelector(selected: Role?, onSelect: (Role) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Role.entries.forEach { option ->
            val isSelected = selected == option
            FilterChip(
                selected = isSelected,
                onClick = { onSelect(option) },
                label = {
                    Text(
                        text = option.label,
                        modifier = Modifier.fillMaxWidth(),
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center
                    )
                },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                shape = FieldShape,
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = Color.White,
                    labelColor = SecondaryText,
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = ButtonText
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = isSelected,
                    borderColor = FieldBorder,
                    selectedBorderColor = MaterialTheme.colorScheme.primary,
                    borderWidth = 1.dp,
                    selectedBorderWidth = 1.dp
                )
            )
        }
    }
}

/** "¿Ya tienes cuenta? Inicia sesión": solo "Inicia sesión" es el link (vuelve al login). */
@Composable
private fun SignInFooter(onNavigateToSignIn: () -> Unit) {
    val linkStyle = SpanStyle(fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.primary)
    val text = buildAnnotatedString {
        append("¿Ya tienes cuenta? ")
        withLink(LinkAnnotation.Clickable("sign_in", TextLinkStyles(linkStyle)) { onNavigateToSignIn() }) {
            append("Inicia sesión")
        }
    }
    Text(
        text = text,
        modifier = Modifier.fillMaxWidth(),
        style = MaterialTheme.typography.bodyMedium.copy(letterSpacing = 0.sp, lineHeightStyle = FigmaLineHeight),
        color = SecondaryText,
        textAlign = TextAlign.Center
    )
}
