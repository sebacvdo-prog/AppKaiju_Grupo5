package com.example.appkaiju_grupo5.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.appkaiju_grupo5.data.InventarioRepository
import com.example.appkaiju_grupo5.device.FotosProducto
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CapturaPendiente(val productoId: Int, val archivo: String, val lanzada: Boolean = false)
data class FotoProductoUiState(
    val pendiente: CapturaPendiente? = null,
    val preparando: Boolean = false,
    val procesando: Boolean = false,
    val error: String? = null
) {
    val ocupada get() = preparando || procesando || pendiente != null
}

class FotoProductoViewModel(
    private val savedState: SavedStateHandle,
    private val repository: InventarioRepository,
    private val fotos: FotosProducto
) : ViewModel() {
    private val state = MutableStateFlow(FotoProductoUiState(pendiente = restaurarPendiente()))
    val uiState = state.asStateFlow()
    private val mensajes = Channel<String>(Channel.BUFFERED)
    val eventos = mensajes.receiveAsFlow()

    fun prepararCaptura(productoId: Int) {
        if (state.value.ocupada) return
        if (productoId <= 0) {
            state.update { it.copy(error = "Guarda el producto antes de tomar una foto.") }
            return
        }
        state.update { it.copy(preparando = true, error = null) }
        viewModelScope.launch {
            try {
                val pendiente = CapturaPendiente(productoId, fotos.crearTemporal())
                guardarPendiente(pendiente)
                state.update { it.copy(pendiente = pendiente) }
            } catch (cancelled: CancellationException) { throw cancelled }
              catch (_: Exception) { state.update { it.copy(error = "No se pudo preparar la fotografía. Revisa el espacio disponible.") } }
            finally { state.update { it.copy(preparando = false) } }
        }
    }

    fun marcarLanzada() {
        val pendiente = state.value.pendiente ?: return
        val lanzada = pendiente.copy(lanzada = true)
        guardarPendiente(lanzada)
        state.update { it.copy(pendiente = lanzada) }
    }

    fun finalizarCaptura(exito: Boolean, errorApertura: String? = null) {
        val pendiente = state.value.pendiente ?: return
        if (state.value.procesando) return
        state.update { it.copy(procesando = true, error = null) }
        viewModelScope.launch {
            var asociada = false
            try {
                if (!exito) {
                    eliminarSeguro(pendiente.archivo)
                    state.update { it.copy(error = errorApertura) }
                    if (errorApertura == null) mensajes.send("Captura cancelada. Se conserva la foto anterior.")
                } else {
                    check(fotos.esValida(pendiente.archivo)) { "La cámara no entregó una imagen válida." }
                    val anterior = repository.actualizarFoto(pendiente.productoId, pendiente.archivo)
                    asociada = true
                    val limpia = anterior == null || anterior == pendiente.archivo || eliminarSeguro(anterior)
                    mensajes.send(if (limpia) "Fotografía guardada" else "Fotografía guardada. No se pudo limpiar la foto anterior.")
                }
            } catch (cancelled: CancellationException) {
                // Una cancelación puede ocurrir después del commit de Room: conserva el archivo.
                throw cancelled
            } catch (_: Exception) {
                if (!asociada) eliminarSeguro(pendiente.archivo)
                state.update { it.copy(error = "No se pudo guardar la fotografía. Se conserva la foto anterior.") }
            } finally {
                savedState.remove<Int>(PRODUCTO)
                savedState.remove<String>(ARCHIVO)
                savedState.remove<Boolean>(LANZADA)
                state.update { it.copy(pendiente = null, procesando = false) }
            }
        }
    }

    fun limpiarError() { if (!state.value.ocupada) state.update { it.copy(error = null) } }

    private suspend fun eliminarSeguro(archivo: String): Boolean = try {
        fotos.eliminar(archivo)
        true
    } catch (cancelled: CancellationException) { throw cancelled }
      catch (_: Exception) { false }

    private fun guardarPendiente(pendiente: CapturaPendiente) {
        savedState[PRODUCTO] = pendiente.productoId
        savedState[ARCHIVO] = pendiente.archivo
        savedState[LANZADA] = pendiente.lanzada
    }

    private fun restaurarPendiente(): CapturaPendiente? {
        val id = savedState.get<Int>(PRODUCTO) ?: return null
        val archivo = savedState.get<String>(ARCHIVO) ?: return null
        if (id <= 0 || !FotosProducto.nombreSeguro(archivo)) return null
        return CapturaPendiente(id, archivo, savedState[LANZADA] ?: false)
    }

    private companion object {
        const val PRODUCTO = "camaraProductoId"
        const val ARCHIVO = "camaraArchivo"
        const val LANZADA = "camaraLanzada"
    }
}
