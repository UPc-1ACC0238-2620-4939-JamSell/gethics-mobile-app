package com.jamsell.gethics.shared.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.jamsell.gethics.R

/**
 * Campo outlined. leadingIcon y shape son opcionales (sin ellos se ve igual que antes).
 * isPassword oculta el texto y agrega el boton de ojo del Figma para mostrarlo/ocultarlo.
 */
@Composable
fun GethicsTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    isPassword: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    errorMessage: String? = null,
    leadingIcon: (@Composable () -> Unit)? = null,
    shape: Shape = OutlinedTextFieldDefaults.shape
) {
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    val hidden = isPassword && !passwordVisible

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        label = { Text(label) },
        leadingIcon = leadingIcon,
        trailingIcon = if (isPassword) {
            {
                // El Figma solo define el ojo: mismo icono en ambos estados, cambia la descripcion.
                // padding end 5.5dp: deja el icono a 19dp del borde derecho, como en el Figma (medido en emulador).
                IconButton(onClick = { passwordVisible = !passwordVisible }, modifier = Modifier.padding(end = 5.5.dp)) {
                    Icon(
                        painter = painterResource(R.drawable.ic_eye),
                        contentDescription = if (passwordVisible) "Ocultar contraseña" else "Mostrar contraseña"
                    )
                }
            }
        } else null,
        singleLine = true,
        isError = errorMessage != null,
        supportingText = errorMessage?.let { { Text(it) } },
        shape = shape,
        visualTransformation = if (hidden) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = if (isPassword) KeyboardType.Password else keyboardType)
    )
}
