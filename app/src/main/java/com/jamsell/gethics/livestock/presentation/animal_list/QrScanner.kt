package com.jamsell.gethics.livestock.presentation.animal_list

import android.content.Context
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode

/**
 * US-29. Escaner de QR de Google Play Services (code scanner): no pide permiso de camara ni pantalla propia.
 * Si el escaner no esta disponible o el usuario lo cancela, no pasa nada: queda el campo para escribir el codigo.
 */
fun scanQrCode(context: Context, onCode: (String) -> Unit, onUnavailable: () -> Unit) {
    val options = GmsBarcodeScannerOptions.Builder()
        .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
        .build()
    GmsBarcodeScanning.getClient(context, options)
        .startScan()
        .addOnSuccessListener { barcode -> barcode.rawValue?.let(onCode) }
        .addOnFailureListener { onUnavailable() }
}
