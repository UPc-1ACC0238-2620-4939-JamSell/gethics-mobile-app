package com.jamsell.gethics.shared.ui

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext

/**
 * US-36. Llama a onAvailable (en el hilo principal) cuando el dispositivo recupera la red, solo mientras enabled sea true.
 * Se usa con enabled = "la pantalla esta mostrando datos guardados por falta de conexion".
 */
@Composable
fun OnNetworkAvailable(enabled: Boolean, onAvailable: () -> Unit) {
    val context = LocalContext.current
    val latest = rememberUpdatedState(onAvailable)
    DisposableEffect(enabled) {
        val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        if (!enabled || manager == null) return@DisposableEffect onDispose { }
        val main = Handler(Looper.getMainLooper())
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                main.post { latest.value() }
            }
        }
        runCatching { manager.registerDefaultNetworkCallback(callback) }
        onDispose { runCatching { manager.unregisterNetworkCallback(callback) } }
    }
}
