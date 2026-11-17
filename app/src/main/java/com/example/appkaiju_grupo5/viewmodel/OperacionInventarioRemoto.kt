package com.example.appkaiju_grupo5.viewmodel

import com.example.appkaiju_grupo5.model.DatosProductoRemoto
import java.io.Serializable
import java.util.UUID

internal sealed interface OperacionInventarioRemoto : Serializable {
    val uuid: UUID

    data class Alta(override val uuid: UUID, val datos: DatosProductoRemoto) : OperacionInventarioRemoto
    data class Edicion(override val uuid: UUID, val etag: String, val datos: DatosProductoRemoto) : OperacionInventarioRemoto
    data class Archivo(override val uuid: UUID, val etag: String) : OperacionInventarioRemoto
    data class Ajuste(override val uuid: UUID, val operacionId: UUID, val delta: Int) : OperacionInventarioRemoto
}
