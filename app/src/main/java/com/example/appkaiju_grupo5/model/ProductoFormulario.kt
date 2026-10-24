package com.example.appkaiju_grupo5.model

data class ProductoErrores(
    val nombre: String? = null,
    val categoria: String? = null,
    val stock: String? = null,
    val stockMinimo: String? = null,
    val ubicacion: String? = null
) {
    val hayErrores: Boolean
        get() = listOf(nombre, categoria, stock, stockMinimo, ubicacion).any { it != null }
}

data class ProductoFormulario(
    val id: Int = 0,
    val nombre: String = "",
    val categoria: String = "",
    val stock: String = "0",
    val stockMinimo: String = "0",
    val ubicacion: String = ""
) {
    fun validar() = ProductoErrores(
        nombre = if (nombre.isBlank()) "Ingresa el nombre del producto." else null,
        categoria = if (categoria.isBlank()) "Ingresa la categoría." else null,
        stock = validarCantidad(stock),
        stockMinimo = validarCantidad(stockMinimo),
        ubicacion = if (ubicacion.isBlank()) "Ingresa la ubicación." else null
    )

    fun aProducto(): Producto {
        require(!validar().hayErrores)
        return Producto(id, nombre.trim(), categoria.trim(), stock.trim().toInt(),
            stockMinimo.trim().toInt(), ubicacion.trim())
    }

    private fun validarCantidad(texto: String): String? =
        if (texto.trim().toIntOrNull()?.let { it >= 0 } == true) null
        else "Ingresa un entero entre 0 y ${Int.MAX_VALUE}."

    companion object {
        fun desde(producto: Producto) = ProductoFormulario(
            producto.id, producto.nombre, producto.categoria,
            producto.stock.toString(), producto.stockMinimo.toString(), producto.ubicacion
        )
    }
}
