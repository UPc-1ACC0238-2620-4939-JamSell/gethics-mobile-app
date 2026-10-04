package com.jamsell.gethics.shared.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Pantalla "pendiente de implementar". Muestra titulo, historia de usuario y que hay que construir.
 * Al implementar una pantalla: BORRAR el uso de ScreenPlaceholder y construir la UI real.
 */
@Composable
fun ScreenPlaceholder(
    title: String,
    userStory: String,
    description: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit = {}
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary
        )
        Text(text = userStory, style = MaterialTheme.typography.labelLarge)
        Text(text = description, style = MaterialTheme.typography.bodyMedium)
        content()
    }
}
