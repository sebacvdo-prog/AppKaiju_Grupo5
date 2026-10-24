package com.example.appkaiju_grupo5.model

data class UsuarioErrores(
    val nombre: String? = null,
    val correo: String? = null,
    val clave: String? = null,
    val direccion: String? = null,
    val terminosAceptados: String? = null
) {
    val hayErrores: Boolean
        get() = listOf(nombre, correo, clave, direccion, terminosAceptados).any { it != null }
}
