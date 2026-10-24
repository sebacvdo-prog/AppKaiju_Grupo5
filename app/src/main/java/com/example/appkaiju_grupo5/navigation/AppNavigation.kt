package com.example.appkaiju_grupo5.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.example.appkaiju_grupo5.ui.screens.RegistroScreen
import com.example.appkaiju_grupo5.ui.screens.ResumenScreen
import com.example.appkaiju_grupo5.viewmodel.UsuarioViewModel

internal const val REGISTRO_GRAPH_ROUTE = "registro_usuario"

// Extiende el único NavHost de Kaiju y comparte el ViewModel del grafo de registro.
internal fun NavGraphBuilder.registroNavigation(navController: NavHostController) {
    navigation(startDestination = Screen.Register.route, route = REGISTRO_GRAPH_ROUTE) {
        composable(Screen.Register.route) { entry ->
            val usuarioViewModel = sharedUsuarioViewModel(navController, entry)
            RegistroScreen(viewModel = usuarioViewModel, onContinue = {
                navController.navigate(Screen.Summary.route) { launchSingleTop = true }
            })
        }
        composable(Screen.Summary.route) { entry ->
            val usuarioViewModel = sharedUsuarioViewModel(navController, entry)
            val state by usuarioViewModel.uiState.collectAsStateWithLifecycle()
            // Si Android restauró la ruta tras finalizar el proceso, vuelve a pedir
            // el formulario: sus datos personales no se guardan en el estado persistido.
            if (state.formularioValido) {
                ResumenScreen(viewModel = usuarioViewModel, onEdit = { navController.popBackStack() })
            } else {
                LaunchedEffect(Unit) {
                    navController.popBackStack(Screen.Register.route, inclusive = false)
                }
            }
        }
    }
}

@Composable
private fun sharedUsuarioViewModel(
    navController: NavHostController,
    entry: NavBackStackEntry
): UsuarioViewModel {
    val owner = remember(entry) { navController.getBackStackEntry(REGISTRO_GRAPH_ROUTE) }
    return viewModel(viewModelStoreOwner = owner)
}
