package com.example.appkaiju_grupo5.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.appkaiju_grupo5.ui.components.CampoProducto
import com.example.appkaiju_grupo5.viewmodel.EditorRemotoState
import com.example.appkaiju_grupo5.viewmodel.EstadoInventarioRemoto
import com.example.appkaiju_grupo5.viewmodel.InventarioRemotoViewModel

@Composable
internal fun EditorRemotoDialog(
    editor: EditorRemotoState,
    state: EstadoInventarioRemoto,
    viewModel: InventarioRemotoViewModel,
    onCerrarInventario: () -> Unit
) {
    val form = editor.formulario
    val bloqueado = state.procesando || state.reintento != null
    val cerrar = if (state.reintento == null) viewModel::cancelarEditor else onCerrarInventario
    AlertDialog(
        onDismissRequest = { if (!state.procesando) cerrar() },
        title = { Text(if (editor.etag == null) "Alta en el servidor" else "Editar producto del servidor") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (state.procesando) {
                    Text("Procesando solicitud…")
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                }
                EstadoOperacionRemota(state)
                if (editor.requiereRevision) {
                    val revision = editor.revisionServidor?.producto
                    if (revision == null) {
                        TextButton(viewModel::revisarVersion, enabled = !bloqueado) { Text("Revisar versión actual") }
                    } else {
                        Text("Datos actuales del servidor · versión ${revision.version}",
                            style = MaterialTheme.typography.labelLarge)
                        FichaRemota(revision)
                        Text("Compara estos datos con tu borrador antes de guardar.")
                        TextButton(viewModel::aceptarRevision, enabled = !bloqueado) { Text("Usar esta revisión") }
                    }
                }
                if (state.error?.codigo == "uuid_en_uso") {
                    TextButton(viewModel::asignarOtroUuid, enabled = !bloqueado) { Text("Usar un UUID nuevo") }
                }
                CampoProducto("Nombre", form.nombre, editor.errores.nombre, "remoto_nombre", bloqueado) {
                    viewModel.cambiarFormulario(form.copy(nombre = it))
                }
                CampoProducto("Categoría", form.categoria, editor.errores.categoria, "remoto_categoria", bloqueado) {
                    viewModel.cambiarFormulario(form.copy(categoria = it))
                }
                CampoProducto("Stock", form.stock, editor.errores.stock, "remoto_stock", bloqueado, true) {
                    viewModel.cambiarFormulario(form.copy(stock = it))
                }
                CampoProducto("Stock mínimo", form.stockMinimo, editor.errores.stockMinimo, "remoto_minimo", bloqueado, true) {
                    viewModel.cambiarFormulario(form.copy(stockMinimo = it))
                }
                CampoProducto("Ubicación", form.ubicacion, editor.errores.ubicacion, "remoto_ubicacion", bloqueado) {
                    viewModel.cambiarFormulario(form.copy(ubicacion = it))
                }
            }
        },
        confirmButton = {
            if (state.reintento != null) TextButton(viewModel::reintentar, enabled = !state.procesando) { Text("Reintentar") }
            else TextButton(viewModel::guardar,
                enabled = !bloqueado && !editor.requiereRevision && state.error?.codigo != "uuid_en_uso") {
                Text("Guardar en servidor")
            }
        },
        dismissButton = {
            TextButton(cerrar, enabled = !state.procesando) { Text(if (state.reintento == null) "Cancelar" else "Cerrar") }
        }
    )
}
