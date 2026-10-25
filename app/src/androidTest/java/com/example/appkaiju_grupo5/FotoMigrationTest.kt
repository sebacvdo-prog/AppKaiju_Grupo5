package com.example.appkaiju_grupo5

import android.database.sqlite.SQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.appkaiju_grupo5.data.ProductosIniciales
import com.example.appkaiju_grupo5.data.RoomInventarioRepository
import com.example.appkaiju_grupo5.data.local.KaijuDatabase
import com.example.appkaiju_grupo5.model.Producto
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FotoMigrationTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val nombre = "ep2-migracion-foto-${UUID.randomUUID()}.db"

    @After fun limpiar() { context.deleteDatabase(nombre) }

    @Test fun migrationPreservesAllVersionOneProductsAndStockWithoutReseeding() = runBlocking {
        val originales = ProductosIniciales.productos.map { it.copy(stock = it.id * 3) } +
            Producto(7, "Producto propio", "Prueba", 43, 4, "C-07")
        // Usa el JSON exportado de v1 sin el serializador de MigrationTestHelper:
        // su runtime actual falla con AbstractMethodError antes de abrir la base.
        // Room ejecuta la migración real y valida el esquema v2 al reabrir abajo.
        val schema = instrumentation.context.assets.open(
            "com.example.appkaiju_grupo5.data.local.KaijuDatabase/1.json"
        ).bufferedReader().use { JSONObject(it.readText()).getJSONObject("database") }
        val path = context.getDatabasePath(nombre)
        path.parentFile!!.mkdirs()
        SQLiteDatabase.openOrCreateDatabase(path, null).use {
            val entities = schema.getJSONArray("entities")
            for (index in 0 until entities.length()) {
                val entity = entities.getJSONObject(index)
                it.execSQL(entity.getString("createSql").replace("\${TABLE_NAME}", entity.getString("tableName")))
            }
            val queries = schema.getJSONArray("setupQueries")
            for (index in 0 until queries.length()) it.execSQL(queries.getString(index))
            originales.forEach { p ->
                it.execSQL("INSERT INTO productos (id,nombre,categoria,stock,stockMinimo,ubicacion) VALUES (?,?,?,?,?,?)",
                    arrayOf<Any>(p.id, p.nombre, p.categoria, p.stock, p.stockMinimo, p.ubicacion))
            }
            it.version = 1
        }
        repeat(2) {
            val db = KaijuDatabase.crear(context, nombre)
            try {
                assertEquals(originales, RoomInventarioRepository(db.productoDao()).observarProductos().first())
            } finally { db.close() }
        }
    }

    @Test fun photoSurvivesReopenAndStaleEditorCannotEraseItOrChangeOtherProducts() = runBlocking {
        var db = KaijuDatabase.crear(context, nombre)
        try {
            val repository = RoomInventarioRepository(db.productoDao())
            val original = repository.observarProductos().first()
            assertNull(repository.actualizarFoto(2, "producto_primera.jpg"))
            assertEquals("producto_primera.jpg", repository.actualizarFoto(2, "producto_segunda.jpg"))
            repository.guardar(original[1].copy(nombre = "Editado con borrador previo"))
            repository.cambiarStock(2, 1)
            db.close()
            db = KaijuDatabase.crear(context, nombre)
            val actual = RoomInventarioRepository(db.productoDao()).observarProductos().first()
            assertEquals(original.map { if (it.id == 2) it.copy(nombre = "Editado con borrador previo",
                stock = it.stock + 1, fotoRuta = "producto_segunda.jpg") else it }, actual)
        } finally { db.close() }
    }
}
