package com.example.appkaiju_grupo5

import androidx.lifecycle.SavedStateHandle
import com.example.appkaiju_grupo5.data.remote.ClienteInventario
import com.example.appkaiju_grupo5.data.remote.ErrorInventarioRemoto
import com.example.appkaiju_grupo5.data.remote.InventarioRemotoRepository
import com.example.appkaiju_grupo5.model.ProductoFormulario
import com.example.appkaiju_grupo5.viewmodel.InventarioRemotoUiState
import com.example.appkaiju_grupo5.viewmodel.InventarioRemotoViewModel
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.util.UUID
import java.util.concurrent.TimeUnit

class InventarioRemotoEscrituraTest {
    @get:Rule val dispatcher = MainDispatcherRule()
    @get:Rule val servidor = MockWebServer()

    private fun viewModel(savedState: SavedStateHandle = SavedStateHandle()) =
        InventarioRemotoViewModel(InventarioRemotoRepository(ClienteInventario.crear(servidor.url("/").toString())), savedState)

    @Test fun validaAntesDeRegistrarYRecargaLaListaConfirmada() = runTest {
        val vm = viewModel()
        vm.abrirAlta()
        vm.guardar()
        assertTrue(vm.uiState.value.editor!!.errores.hayErrores)
        vm.cambiarFormulario(formulario.copy(stock = "2147483648"))
        vm.guardar()
        assertNotNull(vm.uiState.value.editor!!.errores.stock)
        assertEquals(0, servidor.requestCount)

        vm.cambiarFormulario(formulario)
        val uuid = vm.uiState.value.editor!!.uuid
        encolarProducto(uuid, version = 0, codigo = 201)
        encolarListado(producto(uuid, version = 0))
        vm.guardar()
        vm.guardar()
        vm.esperar()
        val alta = solicitud()
        assertEquals("POST", alta.method)
        assertEquals("/api/v1/productos", alta.path)
        assertEquals(uuid.toString(), JsonParser.parseString(alta.body.readUtf8()).asJsonObject["uuid"].asString)
        assertEquals("GET", solicitud().method)
        assertNull(vm.uiState.value.editor)
        assertEquals("Producto registrado en el servidor.", vm.uiState.value.confirmacion)
    }

    @Test fun conservaUuidYPayloadDelAltaCuandoSePierdeLaRespuesta() = runTest {
        val savedState = SavedStateHandle()
        val vm = viewModel(savedState)
        vm.abrirAlta()
        vm.cambiarFormulario(formulario)
        servidor.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AFTER_REQUEST))
        vm.guardar()
        vm.esperar()
        val original = solicitud().body.readUtf8()
        assertNotNull(vm.uiState.value.reintento)

        val restaurado = viewModel(savedState)
        assertEquals(vm.uiState.value.editor!!.uuid, restaurado.uiState.value.editor!!.uuid)
        restaurado.cambiarFormulario(formulario.copy(nombre = "Otro"))
        encolarProducto(restaurado.uiState.value.editor!!.uuid, version = 0, codigo = 201)
        encolarListado()
        restaurado.reintentar()
        restaurado.reintentar()
        restaurado.esperar()
        assertEquals(original, solicitud().body.readUtf8())
        assertEquals("GET", solicitud().method)
        assertNull(restaurado.uiState.value.reintento)
        assertNull(viewModel(savedState).uiState.value.reintento)
    }

    @Test fun altaCon409ConservaFormularioYExigeAsignacionExplicitaDeOtroUuid() = runTest {
        val vm = viewModel()
        vm.abrirAlta()
        vm.cambiarFormulario(formulario)
        val original = vm.uiState.value.editor!!.uuid
        encolarError(409, "uuid_en_uso")
        vm.guardar()
        vm.esperar()
        solicitud()
        assertEquals(ErrorInventarioRemoto.Conflicto, vm.uiState.value.error!!.motivo)
        assertEquals(formulario, vm.uiState.value.editor!!.formulario)
        assertEquals(original, vm.uiState.value.editor!!.uuid)
        assertNull(vm.uiState.value.reintento)
        vm.asignarOtroUuid()
        assertNotEquals(original, vm.uiState.value.editor!!.uuid)
        assertEquals(formulario, vm.uiState.value.editor!!.formulario)
        assertEquals(1, servidor.requestCount)
    }

    @Test fun edicion412ConservaBorradorYRequiereRevisarAntesDeActualizarEtag() = runTest {
        val vm = viewModel()
        vm.abrirDetalle()
        vm.abrirEdicion()
        val borrador = formulario.copy(nombre = "Mi borrador", stock = "8")
        vm.cambiarFormulario(borrador)
        encolarError(412, "version_desactualizada")
        vm.guardar()
        vm.esperar()
        val primero = solicitud()
        assertEquals("PUT", primero.method)
        assertEquals("/api/v1/productos/$ID", primero.path)
        assertEquals("\"1\"", primero.getHeader("If-Match"))
        assertFalse(JsonParser.parseString(primero.body.readUtf8()).asJsonObject.has("uuid"))
        assertEquals(borrador, vm.uiState.value.editor!!.formulario)
        assertTrue(vm.uiState.value.editor!!.requiereRevision)
        vm.guardar()
        assertEquals(2, servidor.requestCount)

        encolarProducto(ID, nombre = "Cambio concurrente", version = 2)
        vm.revisarVersion()
        vm.esperar()
        assertEquals("GET", solicitud().method)
        assertEquals("Cambio concurrente", vm.uiState.value.editor!!.revisionServidor!!.producto.nombre)
        assertEquals(borrador, vm.uiState.value.editor!!.formulario)
        vm.aceptarRevision()
        assertEquals(3, servidor.requestCount)

        encolarProducto(ID, nombre = "Mi borrador", stock = 8, version = 3)
        encolarListado(producto(ID, nombre = "Mi borrador", stock = 8, version = 3))
        vm.guardar()
        vm.esperar()
        assertEquals("\"2\"", solicitud().getHeader("If-Match"))
        assertEquals("GET", solicitud().method)
        assertEquals("Cambios guardados en el servidor.", vm.uiState.value.confirmacion)
    }

    @Test fun archivaConEtagYActualizaDespuesDel204() = runTest {
        val vm = viewModel()
        vm.abrirDetalle()
        vm.archivar()
        assertEquals(1, servidor.requestCount)
        vm.pedirConfirmacionArchivo(true)
        servidor.enqueue(MockResponse().setResponseCode(204))
        encolarListado()
        vm.archivar()
        vm.esperar()
        val archivo = solicitud()
        assertEquals("DELETE", archivo.method)
        assertEquals("/api/v1/productos/$ID", archivo.path)
        assertEquals("\"1\"", archivo.getHeader("If-Match"))
        assertEquals("GET", solicitud().method)
        assertNull(vm.uiState.value.detalle)
        assertEquals("Producto archivado en el servidor.", vm.uiState.value.confirmacion)
        assertTrue((vm.uiState.value.listado as InventarioRemotoUiState.Disponible).resultado.productos.isEmpty())
    }

    @Test fun archivoCon412ExigeConsultarYConfirmarDeNuevo() = runTest {
        val vm = viewModel()
        vm.abrirDetalle()
        vm.pedirConfirmacionArchivo(true)
        encolarError(412, "version_desactualizada")
        vm.archivar()
        vm.esperar()
        assertEquals("\"1\"", solicitud().getHeader("If-Match"))
        assertTrue(vm.uiState.value.requiereActualizarDetalle)
        vm.pedirConfirmacionArchivo(true)
        vm.archivar()
        assertEquals(2, servidor.requestCount)

        encolarProducto(ID, version = 2)
        vm.abrirProducto(ID)
        vm.esperar()
        assertEquals("GET", solicitud().method)
        assertFalse(vm.uiState.value.confirmarArchivo)
        vm.pedirConfirmacionArchivo(true)
        servidor.enqueue(MockResponse().setResponseCode(204))
        encolarListado()
        vm.archivar()
        vm.esperar()
        assertEquals("\"2\"", solicitud().getHeader("If-Match"))
        assertEquals("GET", solicitud().method)
    }

    @Test fun falloAlRecargarNoReenviaUnAltaConfirmada() = runTest {
        val vm = viewModel()
        vm.abrirAlta()
        vm.cambiarFormulario(formulario)
        encolarProducto(vm.uiState.value.editor!!.uuid, version = 0, codigo = 201)
        servidor.enqueue(MockResponse().setResponseCode(503))
        vm.guardar()
        vm.esperar()
        assertEquals("POST", solicitud().method)
        assertEquals("GET", solicitud().method)
        assertEquals("Producto registrado en el servidor.", vm.uiState.value.confirmacion)
        assertNull(vm.uiState.value.reintento)
        assertTrue(vm.uiState.value.listado is InventarioRemotoUiState.Error)
        vm.reintentar()
        assertEquals(2, servidor.requestCount)
    }

    @Test fun reintentaAjusteConElMismoIdYLeeElSaldoActualEnVezDelReciboAnterior() = runTest {
        val vm = viewModel()
        vm.abrirDetalle()
        servidor.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AFTER_REQUEST))
        vm.ajustar(1)
        vm.ajustar(1)
        vm.esperar()
        val original = solicitud()
        assertEquals("POST", original.method)
        assertEquals("/api/v1/productos/$ID/ajustes-stock", original.path)
        val cuerpo = original.body.readUtf8()
        val ajuste = JsonParser.parseString(cuerpo).asJsonObject
        assertEquals(1, ajuste["delta"].asInt)
        val op = ajuste["operacionId"].asString
        encolarJson("""{"uuid":"$ID","operacionId":"$op","delta":1,"stock":6,"version":2}""", etag = "\"2\"")
        encolarProducto(ID, stock = 9, version = 4)
        vm.reintentar()
        vm.esperar()
        assertEquals(cuerpo, solicitud().body.readUtf8())
        assertEquals("GET", solicitud().method)
        assertEquals(9, vm.uiState.value.detalle!!.producto.stock)
        assertEquals("\"4\"", vm.uiState.value.detalle!!.etag)
        assertNull(vm.uiState.value.reintento)
    }

    @Test fun rechazaAjuste409YActualizaDetalleAntesDeAceptarOtraEdicion() = runTest {
        val vm = viewModel()
        vm.abrirDetalle()
        encolarError(409, "stock_fuera_de_rango")
        vm.ajustar(-1)
        vm.esperar()
        assertEquals(-1, JsonParser.parseString(solicitud().body.readUtf8()).asJsonObject["delta"].asInt)
        assertEquals("stock_fuera_de_rango", vm.uiState.value.error!!.codigo)
        assertTrue(vm.uiState.value.requiereActualizarDetalle)
        vm.abrirEdicion()
        assertNull(vm.uiState.value.editor)
        encolarProducto(ID, stock = 0, version = 2)
        vm.abrirProducto(ID)
        vm.esperar()
        solicitud()
        assertFalse(vm.uiState.value.requiereActualizarDetalle)
        vm.ajustar(-1)
        assertEquals(3, servidor.requestCount)
    }

    @Test fun devuelveErrorSiElEtagDelDetalleNoCoincideConLaVersion() = runTest {
        val vm = viewModel()
        encolarJson(producto(ID), etag = "\"9\"")
        vm.abrirProducto(ID)
        vm.esperar()
        assertEquals(ErrorInventarioRemoto.RespuestaInvalida, vm.uiState.value.error!!.motivo)
        assertNull(vm.uiState.value.detalle)
    }

    private suspend fun InventarioRemotoViewModel.esperar() {
        withContext(Dispatchers.Default) { withTimeout(5_000) { uiState.first { !it.procesando } } }
    }

    private suspend fun InventarioRemotoViewModel.abrirDetalle() {
        encolarProducto(ID)
        abrirProducto(ID)
        esperar()
        val detalle = solicitud()
        assertEquals("GET", detalle.method)
        assertEquals("/api/v1/productos/$ID", detalle.path)
    }

    private fun solicitud() = requireNotNull(servidor.takeRequest(2, TimeUnit.SECONDS))
    private fun encolarError(codigo: Int, error: String) = encolarJson("""{"codigo":"$error"}""", codigo)

    private fun encolarProducto(uuid: UUID, nombre: String = "Remoto", stock: Int = 5, version: Int = 1, codigo: Int = 200) =
        encolarJson(producto(uuid, nombre, stock, version), codigo, "\"$version\"")

    private fun encolarListado(producto: String? = null) {
        val total = if (producto == null) 0 else 1
        val contenido = producto.orEmpty()
        encolarJson("""{"productos":[$contenido],"pagina":0,"tamano":20,"totalProductos":$total,"totalPaginas":$total}""")
    }

    private fun encolarJson(body: String, codigo: Int = 200, etag: String? = null) {
        val respuesta = MockResponse().setResponseCode(codigo).setHeader("Content-Type", "application/json").setBody(body)
        etag?.let { respuesta.setHeader("ETag", it) }
        servidor.enqueue(respuesta)
    }

    private fun producto(uuid: UUID, nombre: String = "Remoto", stock: Int = 5, version: Int = 1) =
        """{"uuid":"$uuid","nombre":"$nombre","categoria":"Equipo","ubicacion":"Bodega","stock":$stock,"stockMinimo":2,"archivado":false,"version":$version}"""

    private val formulario = ProductoFormulario(nombre = "Remoto", categoria = "Equipo", ubicacion = "Bodega",
        stock = "5", stockMinimo = "2")

    private companion object {
        val ID = UUID.fromString("51ebafcd-c28e-44a1-8763-6a16f96591b1")
    }
}
