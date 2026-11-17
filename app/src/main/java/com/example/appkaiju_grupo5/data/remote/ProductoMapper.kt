package com.example.appkaiju_grupo5.data.remote

import com.example.appkaiju_grupo5.model.PaginaProductosRemotos
import com.example.appkaiju_grupo5.model.DatosProductoRemoto
import com.example.appkaiju_grupo5.model.ProductoRemoto
import java.util.UUID

internal fun PaginaProductosDto.aPaginaRemota(paginaSolicitada: Int): PaginaProductosRemotos {
    require(pagina == paginaSolicitada)
    val productos = requireNotNull(productos).map { requireNotNull(it).aProductoRemoto() }
    val tamano = requireNotNull(tamano)
    val totalProductos = requireNotNull(totalProductos)
    val totalPaginas = requireNotNull(totalPaginas)
    require(tamano > 0 && productos.size <= tamano && totalProductos >= productos.size && totalPaginas >= 0)
    require(productos.map { it.uuid }.toSet().size == productos.size)
    return PaginaProductosRemotos(productos, paginaSolicitada, totalProductos, totalPaginas)
}

internal fun ProductoDto.aProductoRemoto(): ProductoRemoto {
    val identificador = requireNotNull(uuid)
    val uuid = UUID.fromString(identificador)
    require(uuid.toString().equals(identificador, ignoreCase = true))
    return ProductoRemoto(
        uuid = uuid,
        nombre = requireNotNull(nombre).also { require(it.isNotBlank()) },
        categoria = requireNotNull(categoria).also { require(it.isNotBlank()) },
        ubicacion = requireNotNull(ubicacion).also { require(it.isNotBlank()) },
        stock = requireNotNull(stock).also { require(it >= 0) },
        stockMinimo = requireNotNull(stockMinimo).also { require(it >= 0) },
        archivado = requireNotNull(archivado),
        version = requireNotNull(version).also { require(it >= 0) }
    )
}

internal fun DatosProductoRemoto.aDto(uuid: UUID? = null) =
    ProductoEscrituraDto(uuid, nombre, categoria, ubicacion, stock, stockMinimo)
