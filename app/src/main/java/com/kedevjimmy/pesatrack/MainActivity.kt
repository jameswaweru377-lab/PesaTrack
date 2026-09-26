package com.kedevjimmy.pesatrack

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.kedevjimmy.pesatrack.ui.PesaTrackApp
import com.kedevjimmy.pesatrack.ui.theme.PesaTrackTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PesaTrackTheme {
                PesaTrackApp()
            }
        }
    }
}
