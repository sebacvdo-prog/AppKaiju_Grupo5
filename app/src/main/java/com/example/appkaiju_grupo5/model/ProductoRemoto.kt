package com.example.appkaiju_grupo5.model

import java.io.Serializable
import java.util.UUID

data class ProductoRemoto(
    val uuid: UUID,
    val nombre: String,
    val categoria: String,
    val ubicacion: String,
    val stock: Int,
    val stockMinimo: Int,
    val archivado: Boolean,
    val version: Long
)

data class PaginaProductosRemotos(
    val productos: List<ProductoRemoto>,
    val pagina: Int,
    val totalProductos: Long,
    val totalPaginas: Int
) {
    val tieneSiguiente: Boolean get() = pagina < totalPaginas - 1
}

internal data class ProductoConEtag(val producto: ProductoRemoto, val etag: String)

internal data class DatosProductoRemoto(
    val nombre: String,
    val categoria: String,
    val ubicacion: String,
    val stock: Int,
    val stockMinimo: Int
) : Serializable {
    fun aFormulario() = ProductoFormulario(nombre = nombre, categoria = categoria, ubicacion = ubicacion,
        stock = stock.toString(), stockMinimo = stockMinimo.toString())
}

internal fun ProductoRemoto.datosEditables() = DatosProductoRemoto(nombre, categoria, ubicacion, stock, stockMinimo)

internal fun ProductoFormulario.datosRemotos() = DatosProductoRemoto(
    nombre.trim(), categoria.trim(), ubicacion.trim(), stock.trim().toInt(), stockMinimo.trim().toInt()
)

internal fun ProductoFormulario.validarRemoto(): ProductoErrores {
    fun validarTexto(texto: String, error: String?): String? =
        error ?: "El texto contiene un carácter no válido.".takeIf { '\u0000' in texto }
    val errores = validar()
    return errores.copy(
        nombre = validarTexto(nombre, errores.nombre),
        categoria = validarTexto(categoria, errores.categoria),
        ubicacion = validarTexto(ubicacion, errores.ubicacion)
    )
}
