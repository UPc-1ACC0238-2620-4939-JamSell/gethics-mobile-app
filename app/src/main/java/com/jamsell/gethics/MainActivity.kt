package com.jamsell.gethics

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.jamsell.gethics.shared.data.local.LanguageManager
import com.jamsell.gethics.shared.navigation.GethicsApp
import com.jamsell.gethics.shared.ui.theme.GethicsTheme

class MainActivity : ComponentActivity() {
    /** Aplica el idioma elegido en Ajustes (ES / EN) a toda la Activity. */
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LanguageManager.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val container = (application as GethicsApplication).container

        enableEdgeToEdge()
        setContent {
            GethicsTheme {
                GethicsApp(container)
            }
        }
    }
}
