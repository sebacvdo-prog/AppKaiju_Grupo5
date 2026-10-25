package com.example.appkaiju_grupo5.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.appkaiju_grupo5.ui.screens.AdaptiveHomeScreen
import com.example.appkaiju_grupo5.ui.screens.PerfilScreen
import com.example.appkaiju_grupo5.ui.screens.ProductosRoute
import com.example.appkaiju_grupo5.KaijuApplication
import com.example.appkaiju_grupo5.viewmodel.ProductosViewModel
import com.example.appkaiju_grupo5.viewmodel.FotoProductoViewModel

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
            val app = LocalContext.current.applicationContext as KaijuApplication
            val productsViewModel: ProductosViewModel = viewModel(factory = viewModelFactory {
                initializer { ProductosViewModel(createSavedStateHandle(), app.inventarioRepository) }
            })
            val fotoViewModel: FotoProductoViewModel = viewModel(factory = viewModelFactory {
                initializer { FotoProductoViewModel(createSavedStateHandle(), app.inventarioRepository, app.fotosProducto) }
            })
            ProductosRoute(productsViewModel, fotoViewModel, app.fotosProducto)
        }
        composable(Screen.Profile.route) {
            PerfilScreen(onRegisterClick = {
                navController.navigate(REGISTRO_GRAPH_ROUTE) { launchSingleTop = true }
            })
        }
        registroNavigation(navController)
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
