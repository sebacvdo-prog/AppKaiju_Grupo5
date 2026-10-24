package com.example.appkaiju_grupo5

import com.example.appkaiju_grupo5.model.ProductoFormulario
import org.junit.Assert.*
import org.junit.Test

class ProductoFormularioTest {
    private val valido = ProductoFormulario(nombre = "  Taladro  ", categoria = "Herramientas", stock = "0", stockMinimo = "3", ubicacion = " B-01 ")

    @Test fun acceptsZeroStockAndTrimsText() {
        assertFalse(valido.validar().hayErrores)
        assertEquals(0, valido.aProducto().stock)
        assertEquals("Taladro", valido.aProducto().nombre)
        assertEquals("B-01", valido.aProducto().ubicacion)
    }

    @Test fun rejectsNegativeDecimalEmptyAndOverflowQuantities() {
        listOf("-1", "1.5", "", "abc", "2147483648").forEach { cantidad ->
            assertNotNull(cantidad, valido.copy(stock = cantidad).validar().stock)
            assertNotNull(cantidad, valido.copy(stockMinimo = cantidad).validar().stockMinimo)
        }
    }

    @Test fun requiresNameCategoryAndLocation() {
        val errores = valido.copy(nombre = " ", categoria = "", ubicacion = "\n").validar()
        assertNotNull(errores.nombre)
        assertNotNull(errores.categoria)
        assertNotNull(errores.ubicacion)
    }

    @Test fun acceptsMaximumIntegerAndStockBelowMinimum() {
        assertFalse(valido.copy(stock = Int.MAX_VALUE.toString()).validar().hayErrores)
        assertTrue(valido.aProducto().requiereReposicion)
    }
}
