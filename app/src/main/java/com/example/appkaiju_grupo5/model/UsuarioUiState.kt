package com.example.appkaiju_grupo5.model

data class UsuarioUiState(
    val nombre: String = "",
    val correo: String = "",
    val clave: String = "",
    val direccion: String = "",
    val terminosAceptados: Boolean = false,
    val errores: UsuarioErrores = UsuarioErrores(),
    val formularioValido: Boolean = false,
    val validacionIntentada: Boolean = false
)
