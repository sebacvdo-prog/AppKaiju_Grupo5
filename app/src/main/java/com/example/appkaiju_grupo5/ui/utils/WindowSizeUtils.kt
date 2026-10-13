package com.example.appkaiju_grupo5.ui.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration

enum class WindowSize {
    Compact,
    Medium,
    Expanded
}

@Composable
fun getWindowSize(): WindowSize {

    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp

    return when {
        screenWidth < 600 -> WindowSize.Compact
        screenWidth < 840 -> WindowSize.Medium
        else -> WindowSize.Expanded
    }
}