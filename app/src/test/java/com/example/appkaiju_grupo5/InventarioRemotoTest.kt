package com.example.appkaiju_grupo5

import com.example.appkaiju_grupo5.data.remote.ClienteInventario
import com.example.appkaiju_grupo5.data.remote.AjusteStockDto
import com.example.appkaiju_grupo5.data.remote.ConsultaRemotaException
import com.example.appkaiju_grupo5.data.remote.ErrorInventarioRemoto
import com.example.appkaiju_grupo5.data.remote.InventarioRemotoRepository
import com.example.appkaiju_grupo5.data.remote.PaginaProductosDto
import com.example.appkaiju_grupo5.data.remote.ProductoApiService
import com.example.appkaiju_grupo5.data.remote.ProductoEscrituraDto
import com.example.appkaiju_grupo5.viewmodel.InventarioRemotoUiState
import com.example.appkaiju_grupo5.viewmodel.InventarioRemotoViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import retrofit2.Response
import java.net.ConnectException
import java.net.UnknownHostException
import java.util.UUID
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalCoroutinesApi::class)
class InventarioRemotoTest {
    @get:Rule val dispatcher = MainDispatcherRule()
    @get:Rule val server = MockWebServer()

    private fun conServidor() = InventarioRemotoRepository(ClienteInventario.crear(server.url("/").toString()))

    @Test fun consultaGetYConservaIdentidadStockYVersion() = runTest {
        encolarJson(paginaConProducto())
        val resultado = conServidor().consultar(0)

        val solicitud = requireNotNull(server.takeRequest(2, TimeUnit.SECONDS))
        assertEquals("GET", solicitud.method)
        assertEquals("/api/v1/productos?pagina=0", solicitud.path)
        val producto = resultado.productos.single()
        assertEquals(UUID.fromString(UUID_PRODUCTO), producto.uuid)
        assertEquals("Casco remoto", producto.nombre)
        assertEquals("Protección", producto.categoria)
        assertEquals("Bodega", producto.ubicacion)
        assertEquals(7, producto.stock)
        assertEquals(2, producto.stockMinimo)
        assertEquals(1L, producto.version)
        assertFalse(producto.archivado)
        assertEquals(21L, resultado.totalProductos)
        assertTrue(resultado.tieneSiguiente)
    }

    @Test fun paginaVaciaEsUnaConsultaExitosa() = runTest {
        encolarJson("""{"productos":[],"pagina":0,"tamano":20,"totalProductos":0,"totalPaginas":0}""")
        val resultado = conServidor().consultar(0)
        assertTrue(resultado.productos.isEmpty())
        assertEquals(0L, resultado.totalProductos)
        assertFalse(resultado.tieneSiguiente)
    }

    @Test fun clasificaServicioNoDisponibleYRespuestaHttpInesperada() = runTest {
        val repository = conServidor()
        server.enqueue(MockResponse().setResponseCode(503))
        server.enqueue(MockResponse().setResponseCode(404))
        assertEquals(ErrorInventarioRemoto.ServidorNoDisponible, errorDe { repository.consultar(0) })
        assertEquals(ErrorInventarioRemoto.NoEncontrado, errorDe { repository.consultar(0) })
    }

    @Test fun rechazaJsonInvalidoYCamposAusentes() = runTest {
        val repository = conServidor()
        encolarJson("{")
        encolarJson(paginaConProducto().replace("\"stock\":7,", ""))
        encolarJson(paginaConProducto().replace(UUID_PRODUCTO, "uuid-invalido"))
        encolarJson(paginaConProducto().replace("\"pagina\":0", "\"pagina\":1"))
        repeat(4) {
            assertEquals(ErrorInventarioRemoto.RespuestaInvalida, errorDe { repository.consultar(0) })
        }
    }

    @Test fun diferenciaConexionRechazadaYFalloDeRed() = runTest {
        val api = apiConRespuesta { pagina ->
            if (pagina == 0) throw ConnectException()
            throw UnknownHostException()
        }
        val repository = InventarioRemotoRepository(api)
        assertEquals(ErrorInventarioRemoto.ServidorNoDisponible, errorDe { repository.consultar(0) })
        assertEquals(ErrorInventarioRemoto.Conexion, errorDe { repository.consultar(1) })
    }

    @Test fun propagaCancelacion() = runTest {
        val repository = InventarioRemotoRepository(apiConRespuesta { throw CancellationException() })
        val error = runCatching { repository.consultar(0) }.exceptionOrNull()
        assertTrue(error is CancellationException)
    }

    @Test fun viewModelCargaAlSolicitarMantieneLaPaginaFallidaYReintenta() = runTest {
        val primeraRespuesta = CompletableDeferred<Response<PaginaProductosDto>>()
        val llamadas = mutableListOf<Int>()
        val api = apiConRespuesta { pagina ->
            llamadas.add(pagina)
            if (llamadas.size == 1) primeraRespuesta.await()
            else Response.success(PaginaProductosDto(emptyList(), pagina, 20, 21, 2))
        }
        val viewModel = InventarioRemotoViewModel(InventarioRemotoRepository(api))
        assertEquals(InventarioRemotoUiState.Pendiente, viewModel.uiState.value.listado)

        viewModel.consultar(1)
        assertEquals(InventarioRemotoUiState.Cargando, viewModel.uiState.value.listado)
        viewModel.consultar(2)
        runCurrent()
        assertEquals(listOf(1), llamadas)
        primeraRespuesta.completeExceptionally(ConnectException())
        runCurrent()
        assertEquals(
            InventarioRemotoUiState.Error(1, ErrorInventarioRemoto.ServidorNoDisponible),
            viewModel.uiState.value.listado
        )

        viewModel.consultar(1)
        runCurrent()
        val resultado = viewModel.uiState.value.listado as InventarioRemotoUiState.Disponible
        assertEquals(1, resultado.resultado.pagina)
        assertEquals(listOf(1, 1), llamadas)
    }

    private fun encolarJson(json: String) {
        server.enqueue(MockResponse().setHeader("Content-Type", "application/json").setBody(json))
    }

    private fun paginaConProducto() = """
        {"productos":[{"uuid":"$UUID_PRODUCTO","nombre":"Casco remoto","categoria":"Protección",
        "ubicacion":"Bodega","stock":7,"stockMinimo":2,"archivado":false,"version":1}],
        "pagina":0,"tamano":20,"totalProductos":21,"totalPaginas":2}
    """.trimIndent()

    private fun apiConRespuesta(responder: suspend (Int) -> Response<PaginaProductosDto>) =
        object : ProductoApiService {
            override suspend fun listar(pagina: Int) = responder(pagina)
            override suspend fun consultar(uuid: UUID) = error("Consulta de detalle inesperada.")
            override suspend fun crear(producto: ProductoEscrituraDto) = error("Alta inesperada.")
            override suspend fun editar(uuid: UUID, etag: String, producto: ProductoEscrituraDto) =
                error("Edición inesperada.")
            override suspend fun archivar(uuid: UUID, etag: String) = error("Archivo inesperado.")
            override suspend fun ajustar(uuid: UUID, ajuste: AjusteStockDto) = error("Ajuste inesperado.")
        }

    private suspend fun errorDe(operacion: suspend () -> Unit): ErrorInventarioRemoto {
        try {
            operacion()
        } catch (error: ConsultaRemotaException) {
            return error.motivo
        }
        throw AssertionError("La consulta debió fallar.")
    }

    private companion object {
        const val UUID_PRODUCTO = "51ebafcd-c28e-44a1-8763-6a16f96591b1"
    }
}
