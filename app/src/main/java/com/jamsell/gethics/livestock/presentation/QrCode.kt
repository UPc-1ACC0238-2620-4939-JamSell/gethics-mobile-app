package com.jamsell.gethics.livestock.presentation

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

/** US-29. Matriz del QR (true = modulo oscuro) para el codigo del animal; sin datos para el QR devuelve null. */
fun qrMatrix(content: String?): Array<BooleanArray>? {
    if (content.isNullOrBlank()) return null
    val hints = mapOf(EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M, EncodeHintType.MARGIN to 1)
    val matrix = QRCodeWriter().encode(content.trim(), BarcodeFormat.QR_CODE, 0, 0, hints)
    return Array(matrix.height) { y -> BooleanArray(matrix.width) { x -> matrix[x, y] } }
}

/** QR como bitmap cuadrado de sizePx, listo para mostrar en la ficha del animal. */
fun qrBitmap(content: String?, sizePx: Int): Bitmap? {
    val modules = qrMatrix(content) ?: return null
    val scale = (sizePx / modules.size).coerceAtLeast(1)
    val side = modules.size * scale
    val bitmap = Bitmap.createBitmap(side, side, Bitmap.Config.ARGB_8888)
    for (y in 0 until side) for (x in 0 until side) {
        bitmap.setPixel(x, y, if (modules[y / scale][x / scale]) Color.BLACK else Color.WHITE)
    }
    return bitmap
}
