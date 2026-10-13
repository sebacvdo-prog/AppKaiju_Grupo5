package com.example.appkaiju_grupo5.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.appkaiju_grupo5.ui.screens.AdaptiveHomeScreen
import com.example.appkaiju_grupo5.ui.screens.PerfilScreen
import com.example.appkaiju_grupo5.ui.screens.ProductosScreen
import com.example.appkaiju_grupo5.viewmodel.ProductosViewModel

@Composable
fun KaijuNavHost(
    navController: NavHostController,
    onProductsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        modifier = modifier
    ) {
        composable(Screen.Home.route) {
            AdaptiveHomeScreen(onProductsClick = onProductsClick)
        }
        composable(Screen.Products.route) {
            val productsViewModel: ProductosViewModel = viewModel()
            val state by productsViewModel.uiState.collectAsStateWithLifecycle()
            ProductosScreen(state = state, onToggleStockFilter = productsViewModel::alternarFiltroStock)
        }
        composable(Screen.Profile.route) {
            PerfilScreen()
        }
    }
}

internal fun NavHostController.navigateToTopLevel(screen: Screen) {
    if (currentDestination?.route == screen.route) return

    navigate(screen.route) {
        // Las secciones siempre vuelven a la raíz, sin acumular copias en la pila.
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
