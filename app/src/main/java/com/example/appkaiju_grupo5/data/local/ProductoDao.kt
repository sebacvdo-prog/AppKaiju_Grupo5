package com.example.appkaiju_grupo5.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductoDao {
    @Query("SELECT * FROM productos ORDER BY id")
    fun observarProductos(): Flow<List<ProductoEntity>>

    @Insert
    suspend fun insertar(producto: ProductoEntity): Long

    @Update
    suspend fun actualizar(producto: ProductoEntity): Int

    // La condición y el cambio se ejecutan en una sola sentencia, sin leer-modificar-escribir.
    @Query("""
        UPDATE productos SET stock = stock + :cantidad
        WHERE id = :id AND stock + :cantidad BETWEEN 0 AND 2147483647
    """)
    suspend fun cambiarStock(id: Int, cantidad: Int): Int
}
