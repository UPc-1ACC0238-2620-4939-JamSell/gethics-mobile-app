package com.jamsell.gethics.shared.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Boton primario (relleno vino) o secundario (outlined) como en el mockup. */
@Composable
fun GethicsButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    outlined: Boolean = false,
    enabled: Boolean = true,
    isLoading: Boolean = false
) {
    val shape = MaterialTheme.shapes.small
    if (outlined) {
        OutlinedButton(
            onClick = onClick,
            modifier = modifier,
            enabled = enabled && !isLoading,
            shape = shape
        ) {
            ButtonContent(text, isLoading)
        }
    } else {
        Button(
            onClick = onClick,
            modifier = modifier,
            enabled = enabled && !isLoading,
            shape = shape
        ) {
            ButtonContent(text, isLoading)
        }
    }
}

@Composable
private fun ButtonContent(text: String, isLoading: Boolean) {
    if (isLoading) {
        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
    } else {
        Text(text)
    }
}
