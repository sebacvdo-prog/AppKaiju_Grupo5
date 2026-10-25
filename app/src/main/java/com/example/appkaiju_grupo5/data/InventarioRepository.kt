package com.example.appkaiju_grupo5.data

import com.example.appkaiju_grupo5.model.Producto
import kotlinx.coroutines.flow.Flow

interface InventarioRepository {
    fun observarProductos(): Flow<List<Producto>>
    suspend fun guardar(producto: Producto): Long
    suspend fun cambiarStock(id: Int, cantidad: Int)
    suspend fun actualizarFoto(id: Int, archivo: String): String?
}
