package com.example.appkaiju_grupo5.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.appkaiju_grupo5.ui.utils.WindowSize
import com.example.appkaiju_grupo5.ui.utils.getWindowSize

@Composable
fun AdaptiveHomeScreen() {

    val windowSize = getWindowSize()

    Scaffold { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (windowSize) {
                WindowSize.Compact -> HomeCompactScreen()
                WindowSize.Medium -> HomeMediumScreen()
                WindowSize.Expanded -> HomeExpandedScreen()
            }
        }
    }
}
