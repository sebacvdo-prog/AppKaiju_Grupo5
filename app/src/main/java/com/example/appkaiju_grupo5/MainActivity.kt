package com.example.appkaiju_grupo5

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.appkaiju_grupo5.ui.screens.AdaptiveHomeScreen
import com.example.appkaiju_grupo5.ui.theme.AppKaiju_Grupo5Theme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            AppKaiju_Grupo5Theme {
                AdaptiveHomeScreen()
            }
        }
    }
}
