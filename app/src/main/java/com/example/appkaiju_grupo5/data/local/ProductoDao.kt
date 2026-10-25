package com.example.appkaiju_grupo5.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductoDao {
    @Query("SELECT * FROM productos ORDER BY id")
    fun observarProductos(): Flow<List<ProductoEntity>>

    @Insert
    suspend fun insertar(producto: ProductoEntity): Long

    // El editor no escribe fotoRuta: una edición pendiente no puede deshacer una captura.
    @Query("""UPDATE productos SET nombre = :nombre, categoria = :categoria,
        stock = :stock, stockMinimo = :stockMinimo, ubicacion = :ubicacion WHERE id = :id""")
    suspend fun actualizar(id: Int, nombre: String, categoria: String, stock: Int, stockMinimo: Int, ubicacion: String): Int

    @Query("SELECT * FROM productos WHERE id = :id")
    suspend fun obtener(id: Int): ProductoEntity?

    @Query("UPDATE productos SET fotoRuta = :archivo WHERE id = :id")
    suspend fun asignarFoto(id: Int, archivo: String): Int

    @Transaction
    suspend fun reemplazarFoto(id: Int, archivo: String): String? {
        val anterior = checkNotNull(obtener(id)) { "El producto no existe." }
        check(asignarFoto(id, archivo) == 1)
        return anterior.fotoRuta
    }

    // La condición y el cambio se ejecutan en una sola sentencia, sin leer-modificar-escribir.
    @Query("""
        UPDATE productos SET stock = stock + :cantidad
        WHERE id = :id AND stock + :cantidad BETWEEN 0 AND 2147483647
    """)
    suspend fun cambiarStock(id: Int, cantidad: Int): Int
}
