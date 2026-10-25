package com.example.appkaiju_grupo5

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.isEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextReplacement
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.appkaiju_grupo5.data.RoomInventarioRepository
import com.example.appkaiju_grupo5.data.local.KaijuDatabase
import com.example.appkaiju_grupo5.ui.screens.ProductosRoute
import com.example.appkaiju_grupo5.ui.theme.AppKaiju_Grupo5Theme
import com.example.appkaiju_grupo5.viewmodel.ProductosViewModel
import com.example.appkaiju_grupo5.viewmodel.FotoProductoViewModel
import com.example.appkaiju_grupo5.device.FotoProductoStorage
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProductosScreenTest {
    @get:Rule val composeRule = createComposeRule()
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val nombre = "ep2-ui-${UUID.randomUUID()}.db"
    private lateinit var database: KaijuDatabase
    private lateinit var repository: RoomInventarioRepository

    @Before fun iniciar() {
        database = KaijuDatabase.crear(context, nombre)
        repository = RoomInventarioRepository(database.productoDao())
        // Abre y siembra antes de mostrar la pantalla; el flujo sigue siendo el de Room.
        runBlocking { repository.observarProductos().first() }
        composeRule.setContent {
            AppKaiju_Grupo5Theme {
                val vm: ProductosViewModel = viewModel(factory = viewModelFactory {
                    initializer { ProductosViewModel(SavedStateHandle(), repository) }
                })
                val fotos = FotoProductoStorage(context)
                val fotoVm: FotoProductoViewModel = viewModel(factory = viewModelFactory {
                    initializer { FotoProductoViewModel(SavedStateHandle(), repository, fotos) }
                })
                ProductosRoute(vm, fotoVm, fotos)
            }
        }
    }

    @After fun cerrar() {
        composeRule.waitForIdle()
        database.close()
        context.deleteDatabase(nombre)
    }

    @Test fun createEditAndStockControlsWriteToRoomAndShowConfirmation() {
        composeRule.onNodeWithTag("producto_nuevo").performClick()
        reemplazar("producto_nombre", "Producto EP2")
        reemplazar("producto_categoria", "Prueba")
        reemplazar("producto_stock", "1")
        reemplazar("producto_minimo", "0")
        reemplazar("producto_ubicacion", "Estante EP2")
        composeRule.onNodeWithTag("producto_guardar").performClick()
        composeRule.waitUntil(5_000) { runBlocking { repository.observarProductos().first().size == 7 } }
        composeRule.onNodeWithTag("producto_editor").assertDoesNotExist()
        composeRule.onNodeWithText("Producto guardado").assertIsDisplayed()
        val producto = runBlocking { repository.observarProductos().first().last() }
        ver("producto_editar_${producto.id}")
        composeRule.onNodeWithTag("producto_editar_${producto.id}").performClick()
        reemplazar("producto_nombre", "Producto editado EP2")
        composeRule.onNodeWithTag("producto_guardar").performClick()
        composeRule.waitUntil(5_000) { runBlocking { repository.observarProductos().first().last().nombre == "Producto editado EP2" } }
        pulsarStock("stock_menos_${producto.id}")
        esperarStock(producto.id, 0)
        composeRule.onNodeWithTag("stock_menos_${producto.id}").assertIsNotEnabled()
        pulsarStock("stock_mas_${producto.id}")
        esperarStock(producto.id, 1)
        assertEquals(7, runBlocking { repository.observarProductos().first().size })
    }

    @Test fun invalidStockStaysInEditorAndDoesNotWrite() {
        composeRule.onNodeWithTag("producto_nuevo").performClick()
        reemplazar("producto_nombre", "Producto inválido")
        reemplazar("producto_categoria", "Prueba")
        reemplazar("producto_stock", "-1")
        reemplazar("producto_ubicacion", "A-01")
        composeRule.onNodeWithTag("producto_guardar").performClick()
        composeRule.onNodeWithTag("producto_editor").assertIsDisplayed()
        composeRule.onNodeWithText("Ingresa un entero entre 0 y 2147483647.").assertExists()
        assertEquals(6, runBlocking { repository.observarProductos().first().size })
    }

    private fun reemplazar(tag: String, valor: String) {
        composeRule.onNodeWithTag(tag).performScrollTo().performTextReplacement(valor)
    }

    private fun ver(tag: String) {
        composeRule.onNodeWithTag("screen_products").performScrollToNode(hasTestTag(tag))
    }

    private fun pulsarStock(tag: String) {
        ver(tag)
        // Room es asíncrono: espera el fin de la escritura anterior y la habilitación real.
        composeRule.waitUntil(5_000) {
            isEnabled().matches(composeRule.onNodeWithTag(tag).fetchSemanticsNode())
        }
        composeRule.onNodeWithTag(tag).performClick()
    }

    private fun esperarStock(id: Int, cantidad: Int) {
        composeRule.waitUntil(5_000) { runBlocking { repository.observarProductos().first().single { it.id == id }.stock == cantidad } }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("stock_$id").assertTextEquals("Stock: $cantidad unidades")
    }
}
