package com.example.appkaiju_grupo5

import androidx.lifecycle.SavedStateHandle
import com.example.appkaiju_grupo5.viewmodel.ProductosViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProductosViewModelTest {
    @Test
    fun stockFilterOnlyShowsProductsRequiringReplenishment() {
        val viewModel = ProductosViewModel(SavedStateHandle())
        val fullInventory = viewModel.uiState.value

        viewModel.alternarFiltroStock()

        val filtered = viewModel.uiState.value
        assertTrue(filtered.soloStockBajo)
        assertTrue(filtered.productos.isNotEmpty())
        assertTrue(filtered.productos.all { it.requiereReposicion })
        assertTrue(filtered.productos.size < fullInventory.productos.size)
        assertEquals(fullInventory.totalProductos, filtered.totalProductos)
        assertEquals(filtered.productosConStockBajo, filtered.productos.size)

        viewModel.alternarFiltroStock()
        assertEquals(fullInventory, viewModel.uiState.value)
    }

    @Test
    fun filterIsRestoredFromSavedState() {
        val savedState = SavedStateHandle()
        val original = ProductosViewModel(savedState)
        assertFalse(original.uiState.value.soloStockBajo)
        original.alternarFiltroStock()

        val recreated = ProductosViewModel(SavedStateHandle(savedState.keys().associateWith { savedState.get<Any?>(it) }))

        assertEquals(original.uiState.value, recreated.uiState.value)
    }
}
