package com.jamsell.gethics

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.jamsell.gethics.shared.navigation.GethicsApp
import com.jamsell.gethics.shared.ui.theme.GethicsTheme

class MainActivity : ComponentActivity() {
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
