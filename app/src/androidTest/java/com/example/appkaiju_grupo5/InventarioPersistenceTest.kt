package com.example.appkaiju_grupo5

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.appkaiju_grupo5.data.RoomInventarioRepository
import com.example.appkaiju_grupo5.data.local.KaijuDatabase
import com.example.appkaiju_grupo5.model.Producto
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class InventarioPersistenceTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val nombre = "ep2-test-${UUID.randomUUID()}.db"
    private lateinit var database: KaijuDatabase
    private lateinit var repository: RoomInventarioRepository

    @Before fun abrir() {
        database = KaijuDatabase.crear(context, nombre)
        repository = RoomInventarioRepository(database.productoDao())
    }

    @After fun cerrar() {
        database.close()
        // Solo elimina la base temporal de esta prueba; nunca el inventario de la app.
        context.deleteDatabase(nombre)
    }

    @Test fun seedsExactlyOnceAndKeepsNewProductsEditsAndStockAfterReopen() = runBlocking {
        val iniciales = repository.observarProductos().first()
        assertEquals(6, iniciales.size)
        assertEquals(6, iniciales.map { it.id }.distinct().size)
        val id = repository.guardar(Producto(0, "Martillo", "Herramientas", 7, 2, "B-03")).toInt()
        repository.guardar(iniciales.first().copy(nombre = "Casco editado", stock = 0))
        repository.cambiarStock(id, -2)
        repeat(3) {
            database.close()
            abrir()
            val lista = repository.observarProductos().first()
            assertEquals(7, lista.size)
            assertEquals(7, lista.map { it.id }.distinct().size)
            assertEquals(0, lista.first().stock)
            assertEquals("Casco editado", lista.first().nombre)
            assertEquals(5, lista.single { it.id == id }.stock)
        }
    }

    @Test fun flowEmitsCreatedAndEditedProductsWithoutReloadingRepository() = runBlocking {
        repository.observarProductos().first()
        val creado = async { withTimeout(5_000) { repository.observarProductos().first { it.size == 7 } } }
        val id = repository.guardar(Producto(0, "Nuevo", "Prueba", 2, 1, "A-01")).toInt()
        assertEquals(2, creado.await().single { it.id == id }.stock)
        val editado = async { withTimeout(5_000) { repository.observarProductos().first { lista -> lista.any { it.id == id && it.stock == 0 } } } }
        repository.cambiarStock(id, -2)
        assertTrue(editado.await().single { it.id == id }.requiereReposicion)
    }

    @Test fun invalidProductsAndMissingIdsNeverModifyExistingInventory() = runBlocking {
        val anterior = repository.observarProductos().first()
        for (producto in listOf(anterior.first().copy(stock = -1), anterior.first().copy(stockMinimo = -1), anterior.first().copy(nombre = " "))) {
            try { repository.guardar(producto); fail("Debía rechazar el producto") } catch (_: IllegalArgumentException) { }
        }
        try { repository.guardar(anterior.first().copy(id = 999)); fail("Debía rechazar el id") } catch (_: IllegalStateException) { }
        assertEquals(anterior, repository.observarProductos().first())
    }

    @Test fun concurrentStockUpdatesDoNotLoseIncrementsOrAllowNegativeValues() = runBlocking {
        val producto = repository.observarProductos().first().first()
        repository.guardar(producto.copy(stock = 0))
        coroutineScope { List(20) { async(Dispatchers.IO) { repository.cambiarStock(producto.id, 1) } }.awaitAll() }
        assertEquals(20, repository.observarProductos().first().first().stock)
        val resultados = coroutineScope {
            List(30) { async(Dispatchers.IO) { runCatching { repository.cambiarStock(producto.id, -1) }.isSuccess } }.awaitAll()
        }
        assertEquals(20, resultados.count { it })
        assertEquals(0, repository.observarProductos().first().first().stock)
    }

    @Test fun stockOverflowIsRejectedAtomically() = runBlocking {
        val producto = repository.observarProductos().first().first()
        repository.guardar(producto.copy(stock = Int.MAX_VALUE))
        try { repository.cambiarStock(producto.id, 1); fail("Debía impedir desbordamiento") } catch (_: IllegalStateException) { }
        assertEquals(Int.MAX_VALUE, repository.observarProductos().first().first().stock)
    }
}
