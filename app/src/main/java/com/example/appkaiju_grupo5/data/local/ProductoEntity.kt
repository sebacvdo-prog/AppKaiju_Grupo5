package com.example.appkaiju_grupo5.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.appkaiju_grupo5.model.Producto

@Entity(tableName = "productos")
data class ProductoEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val nombre: String,
    val categoria: String,
    val stock: Int,
    val stockMinimo: Int,
    val ubicacion: String
)

fun ProductoEntity.toProducto() = Producto(id, nombre, categoria, stock, stockMinimo, ubicacion)
fun Producto.toEntity() = ProductoEntity(id, nombre, categoria, stock, stockMinimo, ubicacion)
