package com.example.appkaiju_grupo5.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.example.appkaiju_grupo5.ui.utils.WindowSize
import com.example.appkaiju_grupo5.ui.utils.getWindowSize

@Composable
fun AdaptiveHomeScreen(
    modifier: Modifier = Modifier,
    onProductsClick: () -> Unit = {}
) {

    val windowSize = getWindowSize()

    Box(modifier = modifier.fillMaxSize().testTag("screen_home")) {
        when (windowSize) {
            WindowSize.Compact -> HomeCompactScreen(onProductsClick = onProductsClick)
            WindowSize.Medium -> HomeMediumScreen(onProductsClick = onProductsClick)
            WindowSize.Expanded -> HomeExpandedScreen(onProductsClick = onProductsClick)
        }
    }
}
