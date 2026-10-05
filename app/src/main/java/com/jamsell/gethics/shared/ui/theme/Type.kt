package com.jamsell.gethics.shared.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.sp

/**
 * Interlineado como en el Figma (exportado de CSS): el espacio extra se reparte mitad arriba y mitad abajo.
 * El default de Compose (Trim.Both) lo recorta y desplaza el texto respecto al frame.
 */
val FigmaLineHeight = LineHeightStyle(alignment = LineHeightStyle.Alignment.Center, trim = LineHeightStyle.Trim.None)

// Roboto (fuente del sistema), igual que el Figma. Solo se redefinen slots que ninguna pantalla existente usaba,
// para no cambiar US11/US12/US14; el resto queda con los valores por defecto de Material 3.
val Typography = Typography(
    // Titulo de marca "Gethics" (Figma 3:116)
    headlineMedium = TextStyle(fontWeight = FontWeight.Medium, fontSize = 30.sp, lineHeight = 36.sp, letterSpacing = (-0.75).sp, lineHeightStyle = FigmaLineHeight),
    // Titulo de seccion, ej. "Inicia sesión" (Figma 3:119)
    titleLarge = TextStyle(fontWeight = FontWeight.Medium, fontSize = 20.sp, lineHeight = 28.sp, letterSpacing = 0.sp, lineHeightStyle = FigmaLineHeight)
)
