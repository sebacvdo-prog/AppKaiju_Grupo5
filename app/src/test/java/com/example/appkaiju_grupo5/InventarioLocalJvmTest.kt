package com.example.appkaiju_grupo5

import android.content.Context
import com.example.appkaiju_grupo5.data.ProductosIniciales
import com.example.appkaiju_grupo5.data.RoomInventarioRepository
import com.example.appkaiju_grupo5.data.local.KaijuDatabase
import com.example.appkaiju_grupo5.model.Producto
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = KaijuApplication::class)
class InventarioLocalJvmTest {
    @Test
    fun conservaDatosDeUnaBaseV1AlMigrar() = runBlocking {
        conBaseTemporal { context, nombre ->
            context.openOrCreateDatabase(nombre, Context.MODE_PRIVATE, null).use { db ->
                db.execSQL("""
                    CREATE TABLE productos (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        nombre TEXT NOT NULL, categoria TEXT NOT NULL, stock INTEGER NOT NULL,
                        stockMinimo INTEGER NOT NULL, ubicacion TEXT NOT NULL
                    )
                """.trimIndent())
                db.execSQL("INSERT INTO productos VALUES (40, 'Muestra', 'Equipo', 17, 4, 'A1')")
                db.version = 1
            }
            usarRoom(context, nombre) { repository ->
                assertEquals(
                    listOf(Producto(40, "Muestra", "Equipo", 17, 4, "A1", fotoRuta = null)),
                    repository.observarProductos().first()
                )
            }
        }
    }

    @Test
    fun guardaEditaYLimitaStockSinPerderLaFoto() = runBlocking {
        conBaseTemporal { context, nombre ->
            usarRoom(context, nombre) { repository ->
                assertEquals(ProductosIniciales.productos, repository.observarProductos().first())
                val entrada = Producto(0, " Casco nuevo ", " Equipo ", 1, 2, " B1 ")
                val id = repository.guardar(entrada).toInt()
                assertNull(repository.actualizarFoto(id, "producto_primera.jpg"))
                assertEquals("producto_primera.jpg", repository.actualizarFoto(id, "producto_segunda.jpg"))
                repository.guardar(entrada.copy(id = id, nombre = "Casco editado"))
                repository.cambiarStock(id, -1)

                assertEquals(
                    Producto(id, "Casco editado", "Equipo", 0, 2, "B1", "producto_segunda.jpg"),
                    repository.observarProductos().first().single { it.id == id }
                )
                falla<IllegalStateException> { repository.cambiarStock(id, -1) }
                falla<IllegalStateException> { repository.guardar(entrada.copy(id = Int.MAX_VALUE)) }
                falla<IllegalStateException> { repository.actualizarFoto(Int.MAX_VALUE, "producto_segura.jpg") }
            }
        }
    }

    @Test
    fun rechazaCamposYOperacionesInvalidasSinCambiarLasFilas() = runBlocking {
        conBaseTemporal { context, nombre ->
            usarRoom(context, nombre) { repository ->
                val antes = repository.observarProductos().first()
                val producto = antes.first()
                listOf(
                    producto.copy(id = -1), producto.copy(nombre = " "), producto.copy(categoria = " "),
                    producto.copy(ubicacion = " "), producto.copy(stock = -1), producto.copy(stockMinimo = -1)
                ).forEach { invalido -> falla<IllegalArgumentException> { repository.guardar(invalido) } }
                falla<IllegalArgumentException> { repository.actualizarFoto(producto.id, "../producto_invalida.jpg") }
                falla<IllegalArgumentException> { repository.cambiarStock(producto.id, 0) }
                assertEquals(antes, repository.observarProductos().first())
            }
        }
    }

    private suspend fun conBaseTemporal(prueba: suspend (Context, String) -> Unit) {
        val context = RuntimeEnvironment.getApplication()
        val nombre = "inventario-jvm-${UUID.randomUUID()}.db"
        try {
            prueba(context, nombre)
        } finally {
            context.deleteDatabase(nombre)
        }
    }

    private suspend fun usarRoom(context: Context, nombre: String, prueba: suspend (RoomInventarioRepository) -> Unit) {
        val db = KaijuDatabase.crear(context, nombre)
        try {
            prueba(RoomInventarioRepository(db.productoDao()))
        } finally {
            db.close()
        }
    }

    private suspend inline fun <reified T : Throwable> falla(crossinline operacion: suspend () -> Unit) {
        val error = runCatching { operacion() }.exceptionOrNull()
        assertTrue("Se esperaba ${T::class.java.simpleName}, se obtuvo $error", error is T)
    }
}
