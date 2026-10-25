package com.example.appkaiju_grupo5

import androidx.lifecycle.SavedStateHandle
import com.example.appkaiju_grupo5.device.FotosProducto
import com.example.appkaiju_grupo5.viewmodel.FotoProductoViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FotoProductoViewModelTest {
    @get:Rule val mainDispatcherRule = MainDispatcherRule()
    private val repository = FakeInventarioRepository()
    private val fotos = FakeFotos()
    private val anterior = "producto_anterior.jpg"

    private fun conFotoAnterior() {
        repository.productos.value = repository.productos.value.map { if (it.id == 1) it.copy(fotoRuta = anterior) else it }
    }

    @Test fun successfulCaptureOnlyChangesSelectedPhotoAndDeletesPreviousFile() = runTest {
        conFotoAnterior()
        val original = repository.productos.value
        val vm = FotoProductoViewModel(SavedStateHandle(), repository, fotos)
        vm.prepararCaptura(1)
        runCurrent()
        vm.finalizarCaptura(true)
        vm.finalizarCaptura(true)
        runCurrent()
        assertEquals(original.map { if (it.id == 1) it.copy(fotoRuta = fotos.nueva) else it }, repository.productos.value)
        assertEquals(listOf(anterior), fotos.eliminadas)
        assertFalse(vm.uiState.value.ocupada)
        assertNull(vm.uiState.value.error)
    }

    @Test fun cancelPreservesOldPhotoAndDeletesOnlyTemporaryFile() = runTest {
        conFotoAnterior()
        val original = repository.productos.value
        val vm = FotoProductoViewModel(SavedStateHandle(), repository, fotos)
        vm.prepararCaptura(1)
        runCurrent()
        vm.finalizarCaptura(false)
        runCurrent()
        assertEquals(original, repository.productos.value)
        assertEquals(listOf(fotos.nueva), fotos.eliminadas)
        assertNull(vm.uiState.value.error)
    }

    @Test fun invalidCameraImageDoesNotReplacePreviousPhoto() = runTest {
        conFotoAnterior()
        fotos.valida = false
        val vm = FotoProductoViewModel(SavedStateHandle(), repository, fotos)
        vm.prepararCaptura(1)
        runCurrent()
        vm.finalizarCaptura(true)
        runCurrent()
        assertEquals(anterior, repository.productos.value.first().fotoRuta)
        assertEquals(listOf(fotos.nueva), fotos.eliminadas)
        assertNotNull(vm.uiState.value.error)
        assertFalse(vm.uiState.value.ocupada)
    }

    @Test fun databaseFailurePreservesOldPhotoAndRemovesNewFile() = runTest {
        conFotoAnterior()
        repository.fallar = true
        val vm = FotoProductoViewModel(SavedStateHandle(), repository, fotos)
        vm.prepararCaptura(1)
        runCurrent()
        vm.finalizarCaptura(true)
        runCurrent()
        assertEquals(anterior, repository.productos.value.first().fotoRuta)
        assertEquals(listOf(fotos.nueva), fotos.eliminadas)
        assertNotNull(vm.uiState.value.error)
    }

    @Test fun openingFailureReleasesPendingCaptureAndShowsError() = runTest {
        val vm = FotoProductoViewModel(SavedStateHandle(), repository, fotos)
        vm.prepararCaptura(1)
        runCurrent()
        vm.finalizarCaptura(false, "No hay cámara")
        runCurrent()
        assertEquals("No hay cámara", vm.uiState.value.error)
        assertFalse(vm.uiState.value.ocupada)
        assertEquals(listOf(fotos.nueva), fotos.eliminadas)
    }

    @Test fun pendingCaptureRestoresSameProductFileAndLaunchedFlag() = runTest {
        val handle = SavedStateHandle()
        val original = FotoProductoViewModel(handle, repository, fotos)
        original.prepararCaptura(2)
        runCurrent()
        original.marcarLanzada()
        val restoredHandle = SavedStateHandle(handle.keys().associateWith { handle.get<Any?>(it) })
        val restored = FotoProductoViewModel(restoredHandle, repository, fotos)
        assertEquals(original.uiState.value.pendiente, restored.uiState.value.pendiente)
        assertTrue(restored.uiState.value.pendiente!!.lanzada)
        restored.finalizarCaptura(true)
        runCurrent()
        assertEquals(fotos.nueva, repository.productos.value.single { it.id == 2 }.fotoRuta)
        assertNull(repository.productos.value.first().fotoRuta)
        assertTrue(restoredHandle.keys().isEmpty())
    }

    @Test fun duplicateClickCreatesOnlyOneFileAndUnsavedProductCannotCapture() = runTest {
        val vm = FotoProductoViewModel(SavedStateHandle(), repository, fotos)
        vm.prepararCaptura(0)
        runCurrent()
        assertEquals(0, fotos.creadas)
        assertNotNull(vm.uiState.value.error)
        vm.prepararCaptura(1)
        vm.prepararCaptura(2)
        runCurrent()
        assertEquals(1, fotos.creadas)
        assertEquals(1, vm.uiState.value.pendiente!!.productoId)
    }

    @Test fun storageFailureAllowsRetryWithoutChangingInventory() = runTest {
        fotos.fallarCreacion = true
        val vm = FotoProductoViewModel(SavedStateHandle(), repository, fotos)
        vm.prepararCaptura(1)
        runCurrent()
        assertFalse(vm.uiState.value.ocupada)
        assertNotNull(vm.uiState.value.error)
        fotos.fallarCreacion = false
        vm.prepararCaptura(1)
        runCurrent()
        assertNotNull(vm.uiState.value.pendiente)
        assertTrue(repository.productos.value.all { it.fotoRuta == null })
    }

    @Test fun cleanupFailureNeverRollsBackSuccessfullyAssociatedPhoto() = runTest {
        conFotoAnterior()
        fotos.fallarLimpieza = true
        val vm = FotoProductoViewModel(SavedStateHandle(), repository, fotos)
        vm.prepararCaptura(1)
        runCurrent()
        vm.finalizarCaptura(true)
        runCurrent()
        assertEquals(fotos.nueva, repository.productos.value.first().fotoRuta)
        assertNull(vm.uiState.value.error)
        assertFalse(vm.uiState.value.ocupada)
    }

    @Test fun fileNamesCannotEscapePrivatePhotoDirectory() {
        assertTrue(FotosProducto.nombreSeguro("producto_123-abc.jpg"))
        listOf("../producto_a.jpg", "/producto_a.jpg", "producto_a.png", "producto_a/b.jpg", "").forEach {
            assertFalse(FotosProducto.nombreSeguro(it))
        }
    }

    private class FakeFotos : FotosProducto {
        val nueva = "producto_nueva.jpg"
        val eliminadas = mutableListOf<String>()
        var valida = true
        var creadas = 0
        var fallarCreacion = false
        var fallarLimpieza = false
        override suspend fun crearTemporal(): String { check(!fallarCreacion); creadas++; return nueva }
        override suspend fun esValida(archivo: String) = valida
        override suspend fun eliminar(archivo: String) { check(!fallarLimpieza); eliminadas.add(archivo) }
    }
}
