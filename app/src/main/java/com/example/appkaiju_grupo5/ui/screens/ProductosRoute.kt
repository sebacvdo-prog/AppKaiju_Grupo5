package com.example.appkaiju_grupo5.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.example.appkaiju_grupo5.device.VibracionInventario
import com.example.appkaiju_grupo5.ui.components.ProductoEditorDialog
import com.example.appkaiju_grupo5.viewmodel.ProductosViewModel
import kotlinx.coroutines.launch

@Composable
fun ProductosRoute(viewModel: ProductosViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current.applicationContext
    val vibracion = remember(context) { VibracionInventario(context) }
    val snackbar = remember { SnackbarHostState() }
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(viewModel, lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.eventos.collect { confirmacion ->
                vibracion.confirmar()
                launch { snackbar.showSnackbar(confirmacion.mensaje) }
            }
        }
    }
    Column(Modifier.fillMaxSize()) {
        ProductosScreen(
            state = state, onToggleStockFilter = viewModel::alternarFiltroStock,
            modifier = Modifier.weight(1f),
            onAdd = { viewModel.abrirEditor() }, onEdit = viewModel::abrirEditor,
            onStockChange = viewModel::cambiarStock
        )
        // Reserva su espacio: la confirmación no tapa los botones de la última tarjeta.
        SnackbarHost(snackbar)
    }
    state.editor?.let {
        ProductoEditorDialog(it, state.guardando, state.error,
            viewModel::actualizarFormulario, viewModel::guardarProducto, viewModel::cerrarEditor)
    }
}
