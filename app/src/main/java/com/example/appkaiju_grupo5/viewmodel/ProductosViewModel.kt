package com.example.appkaiju_grupo5.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.example.appkaiju_grupo5.data.InventarioRepository
import com.example.appkaiju_grupo5.model.Producto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ProductosUiState(
    val productos: List<Producto>,
    val soloStockBajo: Boolean,
    val totalProductos: Int,
    val productosConStockBajo: Int
)

class ProductosViewModel(private val savedStateHandle: SavedStateHandle) : ViewModel() {
    private val inventario = InventarioRepository.obtenerProductos()
    private val state = MutableStateFlow(
        crearEstado(savedStateHandle[SOLO_STOCK_BAJO] ?: false)
    )
    val uiState = state.asStateFlow()

    fun alternarFiltroStock() {
        val soloStockBajo = !state.value.soloStockBajo
        savedStateHandle[SOLO_STOCK_BAJO] = soloStockBajo
        state.value = crearEstado(soloStockBajo)
    }

    private fun crearEstado(soloStockBajo: Boolean) = ProductosUiState(
        productos = if (soloStockBajo) inventario.filter { it.requiereReposicion } else inventario,
        soloStockBajo = soloStockBajo,
        totalProductos = inventario.size,
        productosConStockBajo = inventario.count { it.requiereReposicion }
    )

    private companion object {
        const val SOLO_STOCK_BAJO = "soloStockBajo"
    }
}
