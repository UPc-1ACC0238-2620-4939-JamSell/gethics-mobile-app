package com.jamsell.gethics.shared.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = Wine,
    onPrimary = White,
    primaryContainer = Beige,
    onPrimaryContainer = Wine,
    secondary = Navy,
    onSecondary = White,
    secondaryContainer = Mint,
    onSecondaryContainer = Navy,
    background = White,
    onBackground = Ink,
    surface = White,
    onSurface = Ink
)

private val DarkColorScheme = darkColorScheme(
    primary = Beige,
    onPrimary = Ink,
    secondary = Cream,
    background = Ink,
    surface = Ink
)

@Composable
fun GethicsTheme(
    // El mockup solo existe en claro. Cuando el equipo disene el oscuro, cambiar a isSystemInDarkTheme().
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    // Sin dynamic color: queremos la identidad de marca (vino/verde/beige) en todos los celulares.
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = Typography,
        content = content
    )
}
