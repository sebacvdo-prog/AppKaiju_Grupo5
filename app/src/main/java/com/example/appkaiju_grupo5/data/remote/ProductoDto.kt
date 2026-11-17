package com.example.appkaiju_grupo5.data.remote

import com.google.gson.annotations.SerializedName
import java.util.UUID

internal data class ProductoDto(
    @SerializedName("uuid") val uuid: String?,
    @SerializedName("nombre") val nombre: String?,
    @SerializedName("categoria") val categoria: String?,
    @SerializedName("ubicacion") val ubicacion: String?,
    @SerializedName("stock") val stock: Int?,
    @SerializedName("stockMinimo") val stockMinimo: Int?,
    @SerializedName("archivado") val archivado: Boolean?,
    @SerializedName("version") val version: Long?
)

internal data class PaginaProductosDto(
    @SerializedName("productos") val productos: List<ProductoDto?>?,
    @SerializedName("pagina") val pagina: Int?,
    @SerializedName("tamano") val tamano: Int?,
    @SerializedName("totalProductos") val totalProductos: Long?,
    @SerializedName("totalPaginas") val totalPaginas: Int?
)

internal data class ProductoEscrituraDto(
    @SerializedName("uuid") val uuid: UUID?,
    @SerializedName("nombre") val nombre: String,
    @SerializedName("categoria") val categoria: String,
    @SerializedName("ubicacion") val ubicacion: String,
    @SerializedName("stock") val stock: Int,
    @SerializedName("stockMinimo") val stockMinimo: Int
)

internal data class AjusteStockDto(
    @SerializedName("operacionId") val operacionId: UUID,
    @SerializedName("delta") val delta: Int
)

internal data class AjusteAplicadoDto(
    @SerializedName("uuid") val uuid: String?,
    @SerializedName("operacionId") val operacionId: String?,
    @SerializedName("delta") val delta: Int?,
    @SerializedName("stock") val stock: Int?,
    @SerializedName("version") val version: Long?
)
