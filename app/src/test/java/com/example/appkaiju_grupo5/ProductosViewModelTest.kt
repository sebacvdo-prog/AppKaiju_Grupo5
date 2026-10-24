package com.example.appkaiju_grupo5

import androidx.lifecycle.SavedStateHandle
import com.example.appkaiju_grupo5.data.InventarioRepository
import com.example.appkaiju_grupo5.data.ProductosIniciales
import com.example.appkaiju_grupo5.model.Producto
import com.example.appkaiju_grupo5.model.ProductoFormulario
import com.example.appkaiju_grupo5.viewmodel.InventarioConfirmacion
import com.example.appkaiju_grupo5.viewmodel.ProductosViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProductosViewModelTest {
    @get:Rule val mainDispatcherRule = MainDispatcherRule()
    private val repository = FakeInventarioRepository()
    private val formulario = ProductoFormulario(nombre = "Martillo", categoria = "Herramientas", stock = "4", stockMinimo = "2", ubicacion = "B-03")

    @Test fun changingFilterDuringInitialReadKeepsLoadingUntilDataArrives() = runTest {
        val viewModel = ProductosViewModel(SavedStateHandle(), repository)
        viewModel.alternarFiltroStock()
        assertTrue(viewModel.uiState.value.cargando)
        runCurrent()
        assertFalse(viewModel.uiState.value.cargando)
        assertEquals(6, viewModel.uiState.value.totalProductos)
    }

    @Test fun stockFilterOnlyShowsProductsRequiringReplenishment() = runTest {
        val viewModel = ProductosViewModel(SavedStateHandle(), repository)
        runCurrent()
        val fullInventory = viewModel.uiState.value
        assertEquals(6, fullInventory.totalProductos)
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

    @Test fun filterIsRestoredFromSavedState() = runTest {
        val savedState = SavedStateHandle()
        val original = ProductosViewModel(savedState, repository)
        runCurrent()
        assertFalse(original.uiState.value.soloStockBajo)
        original.alternarFiltroStock()
        val recreated = ProductosViewModel(SavedStateHandle(savedState.keys().associateWith { savedState.get<Any?>(it) }), repository)
        runCurrent()
        assertEquals(original.uiState.value, recreated.uiState.value)
    }

    @Test fun createAndEditUpdateInventoryReactivelyAndConfirmOncePerSave() = runTest {
        val viewModel = ProductosViewModel(SavedStateHandle(), repository)
        val eventos = mutableListOf<InventarioConfirmacion>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.eventos.collect { eventos.add(it) } }
        runCurrent()
        viewModel.abrirEditor()
        viewModel.actualizarFormulario(formulario)
        viewModel.guardarProducto()
        runCurrent()
        assertEquals(7, viewModel.uiState.value.totalProductos)
        val nuevo = viewModel.uiState.value.productos.last()
        assertEquals("Martillo", nuevo.nombre)
        assertNull(viewModel.uiState.value.editor)
        viewModel.abrirEditor(nuevo)
        viewModel.actualizarFormulario(ProductoFormulario.desde(nuevo).copy(stock = "1", nombre = "Martillo editado"))
        viewModel.guardarProducto()
        runCurrent()
        assertEquals(7, viewModel.uiState.value.totalProductos)
        assertEquals(1, viewModel.uiState.value.productos.last().stock)
        assertEquals(3, viewModel.uiState.value.productosConStockBajo)
        assertEquals("Martillo editado", viewModel.uiState.value.productos.last().nombre)
        viewModel.alternarFiltroStock()
        runCurrent()
        assertEquals(2, eventos.size)
    }

    @Test fun invalidFormNeverWritesAndErrorsReactToCorrections() = runTest {
        val viewModel = ProductosViewModel(SavedStateHandle(), repository)
        runCurrent()
        viewModel.abrirEditor()
        viewModel.actualizarFormulario(formulario.copy(stock = "-1", stockMinimo = "1.5", nombre = " "))
        viewModel.guardarProducto()
        runCurrent()
        assertEquals(0, repository.escrituras)
        assertTrue(viewModel.uiState.value.editor!!.errores.hayErrores)
        viewModel.actualizarFormulario(formulario)
        assertFalse(viewModel.uiState.value.editor!!.errores.hayErrores)
    }

    @Test fun repositoryChangesUpdateVisibleProductsAndCounts() = runTest {
        val viewModel = ProductosViewModel(SavedStateHandle(), repository)
        runCurrent()
        viewModel.alternarFiltroStock()
        repository.productos.value = repository.productos.value.map { it.copy(stock = 0) }
        runCurrent()
        assertEquals(6, viewModel.uiState.value.productos.size)
        assertEquals(6, viewModel.uiState.value.productosConStockBajo)
    }

    @Test fun stockChangeEmitsOneConfirmationAndFailureEmitsNone() = runTest {
        val viewModel = ProductosViewModel(SavedStateHandle(), repository)
        val eventos = mutableListOf<InventarioConfirmacion>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.eventos.collect { eventos.add(it) } }
        runCurrent()
        viewModel.cambiarStock(repository.productos.value.first(), 1)
        runCurrent()
        assertEquals(25, viewModel.uiState.value.productos.first().stock)
        assertEquals(1, eventos.size)
        repository.fallar = true
        viewModel.cambiarStock(repository.productos.value.first(), -1)
        runCurrent()
        assertEquals(25, viewModel.uiState.value.productos.first().stock)
        assertEquals(1, eventos.size)
        assertNotNull(viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.guardando)
    }

    @Test fun duplicateSaveIsIgnoredWhileFirstWriteIsPendingAndFailureKeepsDraft() = runTest {
        val viewModel = ProductosViewModel(SavedStateHandle(), repository)
        runCurrent()
        repository.espera = CompletableDeferred()
        viewModel.abrirEditor()
        viewModel.actualizarFormulario(formulario)
        viewModel.guardarProducto()
        viewModel.guardarProducto()
        runCurrent()
        assertTrue(viewModel.uiState.value.guardando)
        assertEquals(1, repository.escrituras)
        repository.fallar = true
        repository.espera!!.complete(Unit)
        runCurrent()
        assertEquals(formulario, viewModel.uiState.value.editor!!.formulario)
        assertNotNull(viewModel.uiState.value.error)
    }
}

private class FakeInventarioRepository : InventarioRepository {
    val productos = MutableStateFlow(ProductosIniciales.productos)
    var escrituras = 0
    var fallar = false
    var espera: CompletableDeferred<Unit>? = null
    override fun observarProductos() = productos
    override suspend fun guardar(producto: Producto): Long {
        escrituras++
        espera?.await()
        check(!fallar)
        val guardado = if (producto.id == 0) producto.copy(id = productos.value.maxOf { it.id } + 1) else producto
        productos.value = if (producto.id == 0) productos.value + guardado else productos.value.map { if (it.id == producto.id) guardado else it }
        return guardado.id.toLong()
    }
    override suspend fun cambiarStock(id: Int, cantidad: Int) {
        check(!fallar)
        productos.value = productos.value.map { if (it.id == id) it.copy(stock = it.stock + cantidad) else it }
    }
}
