package com.jamsell.gethics.shared.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.jamsell.gethics.R
import kotlin.math.roundToInt

/**
 * Logo del Figma (Login 41:1001 y Crear Cuenta 41:996): gethics_logo.png recortado por su imageTransform
 * (solo el toro, sin el texto), 104dp con radio 24. Decorativo: siempre va junto al nombre o titulo de la pantalla.
 */
@Composable
fun GethicsLogoMark(modifier: Modifier = Modifier) {
    Image(
        painter = logoMarkPainter(),
        contentDescription = null,
        modifier = modifier
            .size(104.dp)
            .clip(RoundedCornerShape(24.dp)),
        contentScale = ContentScale.FillBounds
    )
}

/** Recorte por proporcion para que funcione igual en cualquier densidad. */
@Composable
private fun logoMarkPainter(): BitmapPainter {
    val bitmap = ImageBitmap.imageResource(R.drawable.gethics_logo)
    return remember(bitmap) {
        BitmapPainter(
            image = bitmap,
            srcOffset = IntOffset((bitmap.width * 0.24395251f).roundToInt(), (bitmap.height * 0.09729159f).roundToInt()),
            srcSize = IntSize((bitmap.width * 0.56830138f).roundToInt(), (bitmap.height * 0.6171875f).roundToInt())
        )
    }
}
