package com.example.appkaiju_grupo5.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.appkaiju_grupo5.data.InventarioRepository
import com.example.appkaiju_grupo5.model.Producto
import com.example.appkaiju_grupo5.model.ProductoErrores
import com.example.appkaiju_grupo5.model.ProductoFormulario
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EditorProductoState(
    val formulario: ProductoFormulario = ProductoFormulario(),
    val errores: ProductoErrores = ProductoErrores(),
    val validacionSolicitada: Boolean = false
)

data class ProductosUiState(
    val productos: List<Producto> = emptyList(),
    val soloStockBajo: Boolean = false,
    val totalProductos: Int = 0,
    val productosConStockBajo: Int = 0,
    val cargando: Boolean = true,
    val guardando: Boolean = false,
    val editor: EditorProductoState? = null,
    val error: String? = null
)

data class InventarioConfirmacion(val mensaje: String)

class ProductosViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val repository: InventarioRepository
) : ViewModel() {
    private var inventario = emptyList<Producto>()
    private val state = MutableStateFlow(ProductosUiState(
        soloStockBajo = savedStateHandle[SOLO_STOCK_BAJO] ?: false,
        editor = restaurarEditor()
    ))
    val uiState = state.asStateFlow()
    private val confirmaciones = Channel<InventarioConfirmacion>(Channel.BUFFERED)
    val eventos = confirmaciones.receiveAsFlow()

    init {
        viewModelScope.launch {
            repository.observarProductos()
                .catch { state.update { it.copy(cargando = false, error = "No se pudo cargar el inventario. Reabre la aplicación para reintentar.") } }
                .collect { productos ->
                    inventario = productos
                    actualizarLista(cargando = false)
                }
        }
    }

    fun alternarFiltroStock() {
        val filtro = !state.value.soloStockBajo
        savedStateHandle[SOLO_STOCK_BAJO] = filtro
        state.update { it.copy(soloStockBajo = filtro) }
        actualizarLista()
    }

    fun abrirEditor(producto: Producto? = null) {
        if (state.value.guardando) return
        val formulario = producto?.let(ProductoFormulario::desde) ?: ProductoFormulario()
        guardarEditor(formulario)
        state.update { it.copy(editor = EditorProductoState(formulario), error = null) }
    }

    fun actualizarFormulario(formulario: ProductoFormulario) {
        val editor = state.value.editor ?: return
        if (state.value.guardando) return
        guardarEditor(formulario)
        state.update { it.copy(editor = editor.copy(
            formulario = formulario,
            errores = if (editor.validacionSolicitada) formulario.validar() else ProductoErrores()
        )) }
    }

    fun cerrarEditor() {
        if (!state.value.guardando) {
            savedStateHandle.remove<ArrayList<String>>(EDITOR)
            state.update { it.copy(editor = null, error = null) }
        }
    }

    fun guardarProducto() {
        val editor = state.value.editor ?: return
        if (state.value.guardando) return
        val errores = editor.formulario.validar()
        state.update { it.copy(editor = editor.copy(errores = errores, validacionSolicitada = true)) }
        if (errores.hayErrores) return
        operar("Producto guardado", cerrarEditor = true) { repository.guardar(editor.formulario.aProducto()) }
    }

    fun cambiarStock(producto: Producto, cantidad: Int) {
        if (state.value.guardando) return
        operar("Stock actualizado") { repository.cambiarStock(producto.id, cantidad) }
    }

    private fun operar(mensaje: String, cerrarEditor: Boolean = false, accion: suspend () -> Unit) {
        state.update { it.copy(guardando = true, error = null) }
        viewModelScope.launch {
            try {
                accion()
                if (cerrarEditor) savedStateHandle.remove<ArrayList<String>>(EDITOR)
                state.update { if (cerrarEditor) it.copy(editor = null) else it }
                confirmaciones.send(InventarioConfirmacion(mensaje))
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                state.update { it.copy(error = "No se pudo guardar el cambio. Revisa las cantidades e inténtalo nuevamente.") }
            } finally {
                state.update { it.copy(guardando = false) }
            }
        }
    }

    private fun actualizarLista(cargando: Boolean = state.value.cargando) {
        state.update { it.copy(
            productos = if (it.soloStockBajo) inventario.filter(Producto::requiereReposicion) else inventario,
            totalProductos = inventario.size,
            productosConStockBajo = inventario.count(Producto::requiereReposicion),
            cargando = cargando
        ) }
    }

    private companion object {
        const val SOLO_STOCK_BAJO = "soloStockBajo"
        const val EDITOR = "editorProducto"
    }

    private fun guardarEditor(form: ProductoFormulario) {
        savedStateHandle[EDITOR] = arrayListOf(form.id.toString(), form.nombre, form.categoria,
            form.stock, form.stockMinimo, form.ubicacion)
    }

    private fun restaurarEditor(): EditorProductoState? {
        val values = savedStateHandle.get<ArrayList<String>>(EDITOR)?.takeIf { it.size == 6 } ?: return null
        val id = values[0].toIntOrNull() ?: return null
        return EditorProductoState(ProductoFormulario(id, values[1], values[2], values[3], values[4], values[5]))
    }
}
