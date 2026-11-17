package com.example.appkaiju_grupo5.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.appkaiju_grupo5.data.remote.ConsultaRemotaException
import com.example.appkaiju_grupo5.data.remote.ErrorInventarioRemoto
import com.example.appkaiju_grupo5.data.remote.InventarioRemotoRepository
import com.example.appkaiju_grupo5.model.PaginaProductosRemotos
import com.example.appkaiju_grupo5.model.ProductoConEtag
import com.example.appkaiju_grupo5.model.ProductoErrores
import com.example.appkaiju_grupo5.model.ProductoFormulario
import com.example.appkaiju_grupo5.model.datosEditables
import com.example.appkaiju_grupo5.model.datosRemotos
import com.example.appkaiju_grupo5.model.validarRemoto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

internal sealed interface InventarioRemotoUiState {
    data object Pendiente : InventarioRemotoUiState
    data object Cargando : InventarioRemotoUiState
    data class Disponible(val resultado: PaginaProductosRemotos) : InventarioRemotoUiState
    data class Error(val pagina: Int, val motivo: ErrorInventarioRemoto) : InventarioRemotoUiState
}

internal data class EditorRemotoState(
    val uuid: UUID,
    val etag: String? = null,
    val formulario: ProductoFormulario = ProductoFormulario(),
    val errores: ProductoErrores = ProductoErrores(),
    val requiereRevision: Boolean = false,
    val revisionServidor: ProductoConEtag? = null
)

internal data class EstadoInventarioRemoto(
    val listado: InventarioRemotoUiState = InventarioRemotoUiState.Pendiente,
    val detalle: ProductoConEtag? = null,
    val editor: EditorRemotoState? = null,
    val confirmarArchivo: Boolean = false,
    val requiereActualizarDetalle: Boolean = false,
    val procesando: Boolean = false,
    val reintento: OperacionInventarioRemoto? = null,
    val error: ConsultaRemotaException? = null,
    val confirmacion: String? = null
)

internal class InventarioRemotoViewModel(
    private val repository: InventarioRemotoRepository,
    private val savedState: SavedStateHandle = SavedStateHandle()
) : ViewModel() {
    private val operacionRestaurada = savedState.get<OperacionInventarioRemoto>(OPERACION)
    private val state = MutableStateFlow(
        EstadoInventarioRemoto(editor = when (val operacion = operacionRestaurada) {
            is OperacionInventarioRemoto.Alta -> EditorRemotoState(operacion.uuid, formulario = operacion.datos.aFormulario())
            is OperacionInventarioRemoto.Edicion ->
                EditorRemotoState(operacion.uuid, operacion.etag, operacion.datos.aFormulario())
            else -> null
        }, reintento = operacionRestaurada)
    )
    val uiState = state.asStateFlow()
    private var ultimaPagina = 0

    fun abrir() {
        val actual = state.value
        if (actual.editor == null && actual.detalle == null) consultar()
    }

    fun consultar(pagina: Int = 0) {
        if (!puedeActuar()) return
        state.value = state.value.copy(
            listado = InventarioRemotoUiState.Cargando, procesando = true, detalle = null,
            confirmarArchivo = false, requiereActualizarDetalle = false, error = null
        )
        viewModelScope.launch {
            cargarPagina(pagina)
            state.value = state.value.copy(procesando = false)
        }
    }

    fun abrirProducto(uuid: UUID) {
        if (!puedeActuar()) return
        state.value = state.value.copy(procesando = true, confirmarArchivo = false, error = null, confirmacion = null)
        viewModelScope.launch {
            cargarDetalle(uuid)
            state.value = state.value.copy(procesando = false)
        }
    }

    fun abrirAlta() {
        if (!puedeActuar()) return
        state.value = state.value.copy(editor = EditorRemotoState(UUID.randomUUID()), error = null, confirmacion = null)
    }

    fun abrirEdicion() {
        val actual = state.value
        val detalle = actual.detalle ?: return
        if (!puedeActuar() || actual.requiereActualizarDetalle) return
        state.value = actual.copy(
            editor = EditorRemotoState(detalle.producto.uuid, detalle.etag, detalle.producto.datosEditables().aFormulario()),
            error = null, confirmacion = null
        )
    }

    fun cambiarFormulario(formulario: ProductoFormulario) {
        val actual = state.value
        val editor = actual.editor ?: return
        if (!puedeActuar()) return
        val errores = if (editor.errores.hayErrores) formulario.validarRemoto() else editor.errores
        state.value = actual.copy(editor = editor.copy(formulario = formulario, errores = errores))
    }

    fun cancelarEditor() {
        if (!puedeActuar()) return
        state.value = state.value.copy(editor = null, error = null)
    }

    fun guardar() {
        val actual = state.value
        val editor = actual.editor ?: return
        if (!puedeActuar() || editor.requiereRevision || actual.error?.codigo == "uuid_en_uso") return
        val errores = editor.formulario.validarRemoto()
        if (errores.hayErrores) {
            state.value = actual.copy(editor = editor.copy(errores = errores))
            return
        }
        val datos = editor.formulario.datosRemotos()
        enviar(if (editor.etag == null) OperacionInventarioRemoto.Alta(editor.uuid, datos)
            else OperacionInventarioRemoto.Edicion(editor.uuid, editor.etag, datos))
    }

    fun revisarVersion() {
        val editor = state.value.editor ?: return
        if (!puedeActuar() || !editor.requiereRevision) return
        state.value = state.value.copy(procesando = true)
        viewModelScope.launch {
            try {
                val revision = repository.consultarProducto(editor.uuid)
                state.value = state.value.copy(editor = editor.copy(revisionServidor = revision), error = null)
            } catch (error: ConsultaRemotaException) {
                state.value = state.value.copy(error = error)
            } finally {
                state.value = state.value.copy(procesando = false)
            }
        }
    }

    fun aceptarRevision() {
        val actual = state.value
        val editor = actual.editor ?: return
        val revision = editor.revisionServidor ?: return
        if (!puedeActuar()) return
        state.value = actual.copy(detalle = revision,
            editor = editor.copy(etag = revision.etag, requiereRevision = false, revisionServidor = null),
            requiereActualizarDetalle = false, error = null)
    }

    fun asignarOtroUuid() {
        val actual = state.value
        val editor = actual.editor ?: return
        if (!puedeActuar() || editor.etag != null || actual.error?.codigo != "uuid_en_uso") return
        state.value = actual.copy(editor = editor.copy(uuid = UUID.randomUUID()), error = null)
    }

    fun pedirConfirmacionArchivo(confirmar: Boolean) {
        if (!puedeActuar() || state.value.requiereActualizarDetalle) return
        state.value = state.value.copy(confirmarArchivo = confirmar)
    }

    fun archivar() {
        val actual = state.value
        val detalle = actual.detalle ?: return
        if (!puedeActuar() || !actual.confirmarArchivo || actual.requiereActualizarDetalle) return
        enviar(OperacionInventarioRemoto.Archivo(detalle.producto.uuid, detalle.etag))
    }

    fun ajustar(delta: Int) {
        val actual = state.value
        val detalle = actual.detalle ?: return
        if (!puedeActuar() || actual.requiereActualizarDetalle || actual.confirmarArchivo) return
        if (delta != -1 && delta != 1) return
        if (detalle.producto.stock.toLong() + delta !in 0L..Int.MAX_VALUE.toLong()) return
        enviar(OperacionInventarioRemoto.Ajuste(detalle.producto.uuid, UUID.randomUUID(), delta))
    }

    fun volverAlListado() = consultar(ultimaPagina)

    fun reintentar() {
        val actual = state.value
        val operacion = actual.reintento ?: return
        if (!actual.procesando) enviar(operacion)
    }

    private fun puedeActuar(): Boolean = !state.value.procesando && state.value.reintento == null

    private fun enviar(operacion: OperacionInventarioRemoto) {
        savedState[OPERACION] = operacion
        state.value = state.value.copy(procesando = true, reintento = null, error = null, confirmacion = null)
        viewModelScope.launch {
            try {
                aplicar(operacion)
            } catch (error: ConsultaRemotaException) {
                manejarError(operacion, error)
                return@launch
            }
            savedState.remove<OperacionInventarioRemoto>(OPERACION)
            val mensaje = when (operacion) {
                is OperacionInventarioRemoto.Alta -> "Producto registrado en el servidor."
                is OperacionInventarioRemoto.Edicion -> "Cambios guardados en el servidor."
                is OperacionInventarioRemoto.Archivo -> "Producto archivado en el servidor."
                is OperacionInventarioRemoto.Ajuste -> "Stock actualizado en el servidor."
            }
            state.value = state.value.copy(
                editor = null, confirmarArchivo = false, confirmacion = mensaje, requiereActualizarDetalle = false,
                detalle = if (operacion is OperacionInventarioRemoto.Ajuste) state.value.detalle else null
            )
            if (operacion is OperacionInventarioRemoto.Ajuste) cargarDetalle(operacion.uuid)
            if (state.value.detalle == null) cargarPagina(ultimaPagina)
            state.value = state.value.copy(procesando = false)
        }
    }

    private suspend fun aplicar(operacion: OperacionInventarioRemoto) {
        when (operacion) {
            is OperacionInventarioRemoto.Alta -> repository.crear(operacion.uuid, operacion.datos)
            is OperacionInventarioRemoto.Edicion -> repository.editar(operacion.uuid, operacion.etag, operacion.datos)
            is OperacionInventarioRemoto.Archivo -> repository.archivar(operacion.uuid, operacion.etag)
            is OperacionInventarioRemoto.Ajuste -> repository.ajustar(operacion.uuid, operacion.operacionId, operacion.delta)
        }
    }

    private fun manejarError(operacion: OperacionInventarioRemoto, error: ConsultaRemotaException) {
        val incierto = when (error.motivo) {
            ErrorInventarioRemoto.Conexion, ErrorInventarioRemoto.ServidorNoDisponible,
            ErrorInventarioRemoto.RespuestaInvalida -> true
            else -> false
        }
        if (!incierto) savedState.remove<OperacionInventarioRemoto>(OPERACION)
        val actual = state.value
        val conflicto = error.motivo in setOf(ErrorInventarioRemoto.Conflicto, ErrorInventarioRemoto.VersionDesactualizada)
        val revisarEdicion = conflicto && operacion is OperacionInventarioRemoto.Edicion
        state.value = actual.copy(
            procesando = false, error = error, confirmarArchivo = false,
            reintento = operacion.takeIf { incierto },
            requiereActualizarDetalle = actual.detalle != null && !incierto,
            editor = if (revisarEdicion) actual.editor?.copy(requiereRevision = true, revisionServidor = null)
                else actual.editor
        )
    }

    private suspend fun cargarPagina(pagina: Int) {
        val listado = try {
            val resultado = repository.consultar(pagina)
            val ultima = (resultado.totalPaginas - 1).coerceAtLeast(0)
            val actual = if (resultado.productos.isEmpty() && pagina > ultima) repository.consultar(ultima)
                else resultado
            ultimaPagina = actual.pagina
            InventarioRemotoUiState.Disponible(actual)
        } catch (error: ConsultaRemotaException) {
            InventarioRemotoUiState.Error(pagina, error.motivo)
        }
        state.value = state.value.copy(listado = listado)
    }

    private suspend fun cargarDetalle(uuid: UUID) {
        try {
            state.value = state.value.copy(detalle = repository.consultarProducto(uuid), requiereActualizarDetalle = false)
        } catch (error: ConsultaRemotaException) {
            state.value = state.value.copy(error = error, requiereActualizarDetalle = true)
        }
    }

    private companion object {
        const val OPERACION = "operacionInventarioServidor"
    }
}
