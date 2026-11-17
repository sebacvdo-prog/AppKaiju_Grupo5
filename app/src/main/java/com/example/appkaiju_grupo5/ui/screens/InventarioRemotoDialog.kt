package com.example.appkaiju_grupo5.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.appkaiju_grupo5.data.remote.ConsultaRemotaException
import com.example.appkaiju_grupo5.data.remote.ErrorInventarioRemoto
import com.example.appkaiju_grupo5.model.PaginaProductosRemotos
import com.example.appkaiju_grupo5.model.ProductoRemoto
import com.example.appkaiju_grupo5.viewmodel.EstadoInventarioRemoto
import com.example.appkaiju_grupo5.viewmodel.InventarioRemotoUiState
import com.example.appkaiju_grupo5.viewmodel.InventarioRemotoViewModel
import java.util.UUID

@Composable
internal fun InventarioRemotoDialog(viewModel: InventarioRemotoViewModel, onDismiss: () -> Unit) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) {
        if (viewModel.uiState.value.listado == InventarioRemotoUiState.Pendiente) viewModel.abrir()
    }

    state.editor?.let { editor ->
        EditorRemotoDialog(editor, state, viewModel, onDismiss)
        return
    }

    val puedeActuar = !state.procesando && state.reintento == null
    val producto = state.detalle?.producto
    AlertDialog(
        onDismissRequest = { if (!state.procesando) onDismiss() },
        title = { Text(if (producto == null) "Inventario del servidor" else "Producto del servidor") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (state.procesando) {
                    Text("Procesando solicitud…", modifier = Modifier.testTag("servidor_estado"))
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                }
                state.confirmacion?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
                EstadoOperacionRemota(state)
                if (producto != null) {
                    DetalleRemoto(producto, state, viewModel)
                } else if (state.reintento == null) {
                    when (val listado = state.listado) {
                        is InventarioRemotoUiState.Disponible ->
                            ResultadoRemoto(listado.resultado, puedeActuar, viewModel::consultar, viewModel::abrirProducto,
                                viewModel::abrirAlta)
                        is InventarioRemotoUiState.Error ->
                            Text(listado.motivo.mensaje(), modifier = Modifier.testTag("servidor_estado"))
                        else -> Unit
                    }
                }
            }
        },
        confirmButton = {
            when {
                state.reintento != null -> TextButton(viewModel::reintentar, enabled = !state.procesando) { Text("Reintentar") }
                state.confirmarArchivo -> TextButton(viewModel::archivar, enabled = puedeActuar) { Text("Confirmar archivo") }
                producto != null -> TextButton(viewModel::volverAlListado, enabled = puedeActuar) { Text("Volver al listado") }
                else -> TextButton(onDismiss, enabled = !state.procesando) { Text("Cerrar") }
            }
        },
        dismissButton = {
            when {
                state.reintento != null -> TextButton(onDismiss, enabled = !state.procesando) { Text("Cerrar") }
                state.confirmarArchivo -> TextButton({ viewModel.pedirConfirmacionArchivo(false) }, enabled = puedeActuar) {
                    Text("Cancelar")
                }
                else -> when (val listado = state.listado) {
                    is InventarioRemotoUiState.Error -> if (producto == null) {
                        TextButton({ viewModel.consultar(listado.pagina) }, enabled = puedeActuar) { Text("Reintentar") }
                    }
                    is InventarioRemotoUiState.Disponible -> if (producto == null) {
                        TextButton({ viewModel.consultar(listado.resultado.pagina) }, enabled = puedeActuar) { Text("Actualizar") }
                    }
                    else -> Unit
                }
            }
        }
    )
}

@Composable
internal fun EstadoOperacionRemota(state: EstadoInventarioRemoto) {
    state.error?.let {
        Text(it.mensaje(), color = MaterialTheme.colorScheme.error, modifier = Modifier.testTag("servidor_error"))
    }
    if (state.reintento != null) Text("El resultado está pendiente de confirmación. Reintenta la misma solicitud.")
}

@Composable
private fun DetalleRemoto(producto: ProductoRemoto, state: EstadoInventarioRemoto, viewModel: InventarioRemotoViewModel) {
    val puedeActuar = !state.procesando && state.reintento == null
    val puedeEditar = puedeActuar && !state.requiereActualizarDetalle && !state.confirmarArchivo
    Column(Modifier.heightIn(max = 340.dp).verticalScroll(rememberScrollState())) {
        FichaRemota(producto)
        if (state.confirmarArchivo) Text("¿Archivar este producto del servidor?")
        if (state.requiereActualizarDetalle) {
            TextButton({ viewModel.abrirProducto(producto.uuid) }, enabled = puedeActuar) { Text("Actualizar detalle") }
        } else {
            Row {
                TextButton({ viewModel.ajustar(-1) }, enabled = puedeEditar && producto.stock > 0) { Text("Restar 1") }
                TextButton({ viewModel.ajustar(1) }, enabled = puedeEditar && producto.stock < Int.MAX_VALUE) { Text("Sumar 1") }
            }
            TextButton(viewModel::abrirEdicion, enabled = puedeEditar) { Text("Editar en servidor") }
            TextButton({ viewModel.pedirConfirmacionArchivo(true) }, enabled = puedeEditar) { Text("Archivar en servidor") }
        }
    }
}

@Composable
private fun ResultadoRemoto(
    pagina: PaginaProductosRemotos,
    habilitado: Boolean,
    onCambiarPagina: (Int) -> Unit,
    onProducto: (UUID) -> Unit,
    onAlta: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Conectado a Kaiju Inventory API", modifier = Modifier.testTag("servidor_estado"))
        Button(onAlta, enabled = habilitado) { Text("Nuevo en servidor") }
        if (pagina.totalProductos == 0L) Text("El servidor no tiene productos.")
        else Text("Productos en el servidor: ${pagina.totalProductos}")
        if (pagina.productos.isNotEmpty()) {
            LazyColumn(Modifier.heightIn(max = 200.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(pagina.productos, key = { it.uuid.toString() }) { producto ->
                    Surface(
                        onClick = { onProducto(producto.uuid) },
                        enabled = habilitado,
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surfaceContainerLow
                    ) {
                        Column(Modifier.padding(8.dp)) { FichaRemota(producto) }
                    }
                }
            }
        }
        if (pagina.tieneSiguiente || pagina.pagina > 0) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton({ onCambiarPagina(pagina.pagina - 1) }, enabled = habilitado && pagina.pagina > 0) {
                    Text("Anterior")
                }
                TextButton({ onCambiarPagina(pagina.pagina + 1) }, enabled = habilitado && pagina.tieneSiguiente) {
                    Text("Siguiente")
                }
            }
        }
    }
}

@Composable
internal fun FichaRemota(producto: ProductoRemoto) {
    Text(producto.nombre, style = MaterialTheme.typography.titleSmall)
    Text("${producto.categoria} · ${producto.ubicacion}")
    Text("Stock: ${producto.stock} · Mínimo: ${producto.stockMinimo}")
}

private fun ConsultaRemotaException.mensaje(): String = when (codigo) {
    "uuid_en_uso" -> "Ese identificador ya corresponde a otra alta. El formulario se conservó."
    "stock_fuera_de_rango" -> "El ajuste dejaría el stock fuera del rango permitido. Consulta el saldo actual."
    "operacion_en_uso" -> "El identificador del ajuste ya está asociado a otra operación."
    else -> motivo.mensaje()
}

private fun ErrorInventarioRemoto.mensaje(): String = when (this) {
    ErrorInventarioRemoto.Conexion -> "Error de conexión. Revisa la red y vuelve a intentarlo."
    ErrorInventarioRemoto.ServidorNoDisponible -> "Servidor no disponible."
    ErrorInventarioRemoto.RespuestaInvalida -> "No se pudo verificar la respuesta del servidor."
    ErrorInventarioRemoto.Validacion -> "El servidor rechazó los datos. Revisa los campos."
    ErrorInventarioRemoto.NoEncontrado -> "El producto ya no está disponible entre los productos activos."
    ErrorInventarioRemoto.Conflicto -> "El servidor detectó un conflicto. Revisa el estado actual."
    ErrorInventarioRemoto.VersionDesactualizada -> "El producto cambió en el servidor. Revisa la versión actual."
}
