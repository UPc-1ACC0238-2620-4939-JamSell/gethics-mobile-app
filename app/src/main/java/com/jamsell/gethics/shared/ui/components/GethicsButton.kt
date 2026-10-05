package com.jamsell.gethics.shared.ui.components

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Boton primario (relleno vino) o secundario (outlined).
 * height/shape/textStyle/elevation son opcionales: cada pantalla pasa los valores de su frame de Figma.
 * Sin ellos se ve igual que antes (radio shapes.small, texto labelLarge, elevacion por defecto).
 */
@Composable
fun GethicsButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    outlined: Boolean = false,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    height: Dp = Dp.Unspecified,
    shape: Shape = MaterialTheme.shapes.small,
    textStyle: TextStyle? = null,
    elevation: Dp? = null
) {
    val sizedModifier = if (height != Dp.Unspecified) modifier.height(height) else modifier
    if (outlined) {
        OutlinedButton(
            onClick = onClick,
            modifier = sizedModifier,
            enabled = enabled && !isLoading,
            shape = shape,
            elevation = elevation?.let { ButtonDefaults.buttonElevation(defaultElevation = it) }
        ) {
            ButtonContent(text, isLoading, textStyle)
        }
    } else {
        Button(
            onClick = onClick,
            modifier = sizedModifier,
            enabled = enabled && !isLoading,
            shape = shape,
            elevation = elevation?.let { ButtonDefaults.buttonElevation(defaultElevation = it) }
                ?: ButtonDefaults.buttonElevation()
        ) {
            ButtonContent(text, isLoading, textStyle)
        }
    }
}

@Composable
private fun ButtonContent(text: String, isLoading: Boolean, textStyle: TextStyle?) {
    if (isLoading) {
        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
    } else {
        Text(text, style = textStyle ?: LocalTextStyle.current)
    }
}
