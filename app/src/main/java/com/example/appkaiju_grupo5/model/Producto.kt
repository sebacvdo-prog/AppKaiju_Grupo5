package com.example.appkaiju_grupo5.model

data class Producto(
    val id: Int,
    val nombre: String,
    val categoria: String,
    val stock: Int,
    val stockMinimo: Int,
    val ubicacion: String
) {
    val requiereReposicion: Boolean
        get() = stock <= stockMinimo
}
