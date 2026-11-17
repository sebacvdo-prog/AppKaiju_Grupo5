package com.example.appkaiju_grupo5

import com.example.appkaiju_grupo5.data.external.ClienteFrankfurter
import com.example.appkaiju_grupo5.data.external.ConsultaCotizacionException
import com.example.appkaiju_grupo5.data.external.ErrorCotizacion
import com.example.appkaiju_grupo5.data.external.FrankfurterApi
import com.example.appkaiju_grupo5.data.external.TipoCambioRepository
import com.example.appkaiju_grupo5.viewmodel.CotizacionViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.math.BigDecimal
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit

class CotizacionTest {
    @get:Rule val dispatcher = MainDispatcherRule()
    @get:Rule val servidor = MockWebServer()

    private fun repository() = TipoCambioRepository(ClienteFrankfurter.crear(servidor.url("/").toString()))

    @Test fun consultaElParUsdClpPorGetSinCredenciales() = runTest {
        encolarJson(cotizacionValida)
        val cotizacion = repository().consultar()
        val solicitud = requireNotNull(servidor.takeRequest(2, TimeUnit.SECONDS))
        assertEquals("GET", solicitud.method)
        assertEquals("/v2/rate/USD/CLP", solicitud.path)
        assertNull(solicitud.getHeader("Authorization"))
        assertEquals(BigDecimal("997.08"), cotizacion.pesosPorDolar)
        assertEquals("2026-11-16", cotizacion.fechaReferencia)
    }

    @Test fun rechazaJsonMalformadoOMonedaFechaOTasaInvalidas() = runTest {
        val repository = repository()
        val invalidas = listOf(
            "{",
            cotizacionValida.replace("CLP", "EUR"),
            cotizacionValida.replace("2026-11-16", "2026-02-29"),
            cotizacionValida.replace("997.08", "-3"),
            cotizacionValida.replace("997.08", "\"997.08\""),
            cotizacionValida.replace("997.08", "null")
        )
        for (respuesta in invalidas) {
            encolarJson(respuesta)
            assertEquals(ErrorCotizacion.RespuestaInvalida, motivo { repository.consultar() })
        }
    }

    @Test fun diferenciaFalloDeRedYRespuestaHttpDelProveedor() = runTest {
        val desconectado = TipoCambioRepository(FrankfurterApi { throw UnknownHostException() })
        assertEquals(ErrorCotizacion.Conexion, motivo { desconectado.consultar() })
        servidor.enqueue(MockResponse().setResponseCode(503))
        assertEquals(ErrorCotizacion.ServicioNoDisponible, motivo { repository().consultar() })
    }

    @Test fun viewModelPublicaCargaErrorYExitoTrasReintento() = runTest {
        servidor.enqueue(MockResponse().setResponseCode(503))
        encolarJson(cotizacionValida)
        val vm = CotizacionViewModel(repository())
        assertTrue(vm.uiState.value.cargando)
        vm.actualizar()
        vm.esperar()
        assertEquals(1, servidor.requestCount)
        assertEquals(ErrorCotizacion.ServicioNoDisponible, vm.uiState.value.error)
        assertNull(vm.uiState.value.cotizacion)

        vm.actualizar()
        assertTrue(vm.uiState.value.cargando)
        vm.esperar()
        assertEquals(2, servidor.requestCount)
        assertNull(vm.uiState.value.error)
        assertEquals(BigDecimal("997.08"), vm.uiState.value.cotizacion?.pesosPorDolar)
    }

    @Test fun siFallaLaActualizacionConservaLaUltimaCotizacionConSuFecha() = runTest {
        encolarJson(cotizacionValida)
        servidor.enqueue(MockResponse().setResponseCode(429))
        val vm = CotizacionViewModel(repository())
        vm.esperar()
        val anterior = vm.uiState.value.cotizacion
        assertNotNull(anterior)
        vm.actualizar()
        vm.esperar()
        assertEquals(ErrorCotizacion.ServicioNoDisponible, vm.uiState.value.error)
        assertEquals(anterior, vm.uiState.value.cotizacion)
    }

    private suspend fun CotizacionViewModel.esperar() {
        withContext(Dispatchers.Default) { withTimeout(5_000) { uiState.first { !it.cargando } } }
    }

    private suspend fun motivo(consulta: suspend () -> Unit): ErrorCotizacion {
        try {
            consulta()
        } catch (error: ConsultaCotizacionException) {
            return error.motivo
        }
        throw AssertionError("Se esperaba un error de cotización.")
    }

    private fun encolarJson(contenido: String) {
        servidor.enqueue(MockResponse().setHeader("Content-Type", "application/json").setBody(contenido))
    }

    private val cotizacionValida = """{"date":"2026-11-16","base":"USD","quote":"CLP","rate":997.08}"""
}
