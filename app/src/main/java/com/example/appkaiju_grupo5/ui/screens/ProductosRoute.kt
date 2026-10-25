package com.example.appkaiju_grupo5.ui.screens

import android.content.ActivityNotFoundException
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import com.example.appkaiju_grupo5.device.FotoProductoStorage
import com.example.appkaiju_grupo5.viewmodel.FotoProductoViewModel
import com.example.appkaiju_grupo5.ui.components.ProductoEditorDialog
import com.example.appkaiju_grupo5.viewmodel.ProductosViewModel
import kotlinx.coroutines.launch

@Composable
fun ProductosRoute(viewModel: ProductosViewModel, fotoViewModel: FotoProductoViewModel, fotos: FotoProductoStorage) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val fotoState by fotoViewModel.uiState.collectAsStateWithLifecycle()
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { exito ->
        fotoViewModel.finalizarCaptura(exito)
    }
    LaunchedEffect(fotoState.pendiente) {
        val pendiente = fotoState.pendiente
        if (pendiente != null && !pendiente.lanzada) {
            fotoViewModel.marcarLanzada()
            try { camera.launch(fotos.uriPara(pendiente.archivo)) }
            catch (_: ActivityNotFoundException) {
                fotoViewModel.finalizarCaptura(false, "No hay una aplicación de cámara disponible.")
            } catch (_: Exception) {
                fotoViewModel.finalizarCaptura(false, "No se pudo abrir la cámara. Inténtalo nuevamente.")
            }
        }
    }
    val context = LocalContext.current.applicationContext
    val vibracion = remember(context) { VibracionInventario(context) }
    val snackbar = remember { SnackbarHostState() }
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(fotoViewModel, lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            fotoViewModel.eventos.collect { mensaje -> launch { snackbar.showSnackbar(mensaje) } }
        }
    }
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
            onAdd = { fotoViewModel.limpiarError(); viewModel.abrirEditor() },
            onEdit = { fotoViewModel.limpiarError(); viewModel.abrirEditor(it) },
            onStockChange = viewModel::cambiarStock
        )
        // Reserva su espacio: la confirmación no tapa los botones de la última tarjeta.
        SnackbarHost(snackbar)
    }
    state.editor?.let {
        ProductoEditorDialog(it, state.guardando || fotoState.ocupada, state.error,
            viewModel::actualizarFormulario, viewModel::guardarProducto,
            onDismiss = { if (!fotoState.ocupada) viewModel.cerrarEditor() },
            fotoRuta = state.productos.find { producto -> producto.id == it.formulario.id }?.fotoRuta,
            errorFoto = fotoState.error, capturando = fotoState.ocupada,
            onTakePhoto = { fotoViewModel.prepararCaptura(it.formulario.id) })
    }
}
