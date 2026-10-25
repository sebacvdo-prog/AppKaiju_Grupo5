package com.example.appkaiju_grupo5.device

/** El ViewModel maneja nombres relativos; no conserva Context, Bitmap ni URI de terceros. */
interface FotosProducto {
    suspend fun crearTemporal(): String
    suspend fun esValida(archivo: String): Boolean
    suspend fun eliminar(archivo: String)

    companion object {
        fun nombreSeguro(archivo: String) = archivo.matches(Regex("producto_[A-Za-z0-9_-]+\\.jpg"))
    }
}
