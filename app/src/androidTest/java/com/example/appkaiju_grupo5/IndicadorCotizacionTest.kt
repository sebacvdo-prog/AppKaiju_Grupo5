package com.example.appkaiju_grupo5

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.appkaiju_grupo5.data.external.ErrorCotizacion
import com.example.appkaiju_grupo5.model.CotizacionUsdClp
import com.example.appkaiju_grupo5.ui.components.IndicadorCotizacion
import com.example.appkaiju_grupo5.ui.theme.AppKaiju_Grupo5Theme
import com.example.appkaiju_grupo5.viewmodel.CotizacionUiState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.math.BigDecimal

@RunWith(AndroidJUnit4::class)
class IndicadorCotizacionTest {
    @get:Rule val compose = createComposeRule()

    @Test fun muestraCargaYDeshabilitaActualizar() {
        compose.setContent {
            AppKaiju_Grupo5Theme { IndicadorCotizacion(CotizacionUiState(cargando = true)) {} }
        }
        compose.onNodeWithText("Consultando cotización…").assertIsDisplayed()
        compose.onNodeWithTag("actualizar_cotizacion").assertIsNotEnabled()
    }

    @Test fun reintentarTrasErrorMuestraTasaYFechaDeReferencia() {
        val estado = mutableStateOf(CotizacionUiState(error = ErrorCotizacion.Conexion))
        var reintentos = 0
        compose.setContent {
            AppKaiju_Grupo5Theme {
                IndicadorCotizacion(estado.value) {
                    reintentos++
                    estado.value = CotizacionUiState(cotizacion = CotizacionUsdClp(BigDecimal("997.08"), "2026-11-16"))
                }
            }
        }
        compose.onNodeWithTag("error_cotizacion").assertIsDisplayed()
        compose.onNodeWithText("Reintentar cotización").performClick()
        compose.onNodeWithText("1 USD = 997,08 CLP").assertIsDisplayed()
        compose.onNodeWithText("Fecha de referencia: 16/11/2026").assertIsDisplayed()
        compose.onNodeWithText("Actualizar cotización").assertIsDisplayed()
        compose.runOnIdle { assertEquals(1, reintentos) }
    }
}
