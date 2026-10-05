package com.jamsell.gethics.iam.presentation.sign_in

import androidx.annotation.DrawableRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role as SemanticsRole
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
import com.jamsell.gethics.shared.ui.theme.Navy

// Valores del Figma oficial: Login, frame 3:113 ("390w light"). Medidas logicas en dp, texto en sp.
private val ScreenHorizontalPadding = 24.dp
private val FieldShape = RoundedCornerShape(18.dp)
private val OutlinedLabelSpace = 8.dp
private val ButtonTextStyle = TextStyle(fontSize = 16.sp, lineHeight = 24.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.4.sp)

@Composable
fun SignInScreen(
    viewModel: SignInViewModel,
    onSignedIn: (Role) -> Unit,
    onNavigateToSignUp: () -> Unit,
    onNavigateToForgotPassword: () -> Unit
) {
    val state = viewModel.state.value
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(state.data) {
        state.data?.let(onSignedIn)
    }

    // El footer va anclado abajo; si no entra (pantalla chica o teclado abierto) todo hace scroll.
    BoxWithConstraints {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .heightIn(min = maxHeight)
                .imePadding()
                .padding(horizontal = ScreenHorizontalPadding),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Spacer(Modifier.height(83.dp))
                BrandHeader()
                Spacer(Modifier.height(48.dp))

                Text(text = "Inicia sesión", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground)
                // OutlinedTextField reserva 8dp sobre el borde para el label flotante: se descuentan de los 24/16 del Figma.
                Spacer(Modifier.height(24.dp - OutlinedLabelSpace))

                // Figma: texto de los campos 16/24 sin letter-spacing (M3 trae 0.5); se ajusta solo en este formulario.
                MaterialTheme(typography = MaterialTheme.typography.copy(
                    bodyLarge = MaterialTheme.typography.bodyLarge.copy(letterSpacing = 0.sp)
                )) {
                    GethicsTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = "Email",
                        modifier = Modifier.fillMaxWidth(),
                        keyboardType = KeyboardType.Email,
                        leadingIcon = { FieldIcon(R.drawable.ic_mail) },
                        shape = FieldShape
                    )
                    Spacer(Modifier.height(16.dp - OutlinedLabelSpace))
                    GethicsTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = "Password",
                        modifier = Modifier.fillMaxWidth(),
                        isPassword = true,
                        leadingIcon = { FieldIcon(R.drawable.ic_lock) },
                        shape = FieldShape
                    )
                }

                // No existe en el Figma: el escenario 2 exige mostrar el error del backend.
                if (state.message.isNotEmpty()) {
                    Text(
                        text = state.message,
                        modifier = Modifier.padding(top = 8.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                // En el Figma la caja del link mide 16 (interlineado 20): se quitan 2dp arriba y abajo para centrarlo igual.
                Spacer(Modifier.height(22.dp - 2.dp))
                Text(
                    text = "¿Olvidaste tu contraseña?",
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(end = 11.67.dp)
                        .clickable(role = SemanticsRole.Button, onClick = onNavigateToForgotPassword),
                    style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 0.sp, lineHeightStyle = FigmaLineHeight),
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(34.dp - 2.dp))

                GethicsButton(
                    text = "Ingresar",
                    onClick = { viewModel.signIn(email, password) },
                    modifier = Modifier.fillMaxWidth(),
                    isLoading = state.isLoading,
                    height = 56.dp,
                    shape = CircleShape,
                    textStyle = ButtonTextStyle,
                    elevation = 1.dp // aproxima la sombra doble del Figma (Android no permite fijar el blur)
                )
            }

            SignUpFooter(onNavigateToSignUp)
        }
    }
}

/** Icono de 20dp a 17dp del borde, como en el Figma (el slot de M3 lo centraria a 14dp). */
@Composable
private fun FieldIcon(@DrawableRes id: Int) {
    Icon(painterResource(id), contentDescription = null, modifier = Modifier.padding(start = 6.dp))
}

/** Logo + "Gethics" + subtitulo. */
@Composable
private fun BrandHeader() {
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        GethicsLogoMark()
        Spacer(Modifier.height(13.dp))
        Text(
            text = "Gethics",
            style = MaterialTheme.typography.headlineMedium,
            color = Navy,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Salud y manejo de tu ganado, en un solo lugar",
            style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.75.sp, letterSpacing = 0.sp, lineHeightStyle = FigmaLineHeight),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

/** "¿No tienes cuenta? Regístrate": solo "Regístrate" es el link. */
@Composable
private fun SignUpFooter(onNavigateToSignUp: () -> Unit) {
    val linkStyle = SpanStyle(fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.primary)
    val text = buildAnnotatedString {
        append("¿No tienes cuenta? ")
        withLink(LinkAnnotation.Clickable("sign_up", TextLinkStyles(linkStyle)) { onNavigateToSignUp() }) {
            append("Regístrate")
        }
    }
    Text(
        text = text,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 42.dp),
        style = MaterialTheme.typography.bodyMedium.copy(letterSpacing = 0.sp, lineHeightStyle = FigmaLineHeight),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center
    )
}
