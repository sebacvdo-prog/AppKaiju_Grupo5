package com.example.appkaiju_grupo5.viewmodel

import androidx.lifecycle.ViewModel
import com.example.appkaiju_grupo5.model.UsuarioErrores
import com.example.appkaiju_grupo5.model.UsuarioUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet

class UsuarioViewModel : ViewModel() {
    // Los datos viven solo en memoria durante el flujo; la clave no se persiste.
    private val estado = MutableStateFlow(UsuarioUiState())
    val uiState: StateFlow<UsuarioUiState> = estado.asStateFlow()

    fun actualizarNombre(valor: String) = actualizar(Campo.Nombre) { it.copy(nombre = valor) }

    fun actualizarCorreo(valor: String) = actualizar(Campo.Correo) { it.copy(correo = valor) }

    fun actualizarClave(valor: String) = actualizar(Campo.Clave) { it.copy(clave = valor) }

    fun actualizarDireccion(valor: String) = actualizar(Campo.Direccion) { it.copy(direccion = valor) }

    fun actualizarTerminosAceptados(valor: Boolean) =
        actualizar(Campo.Terminos) { it.copy(terminosAceptados = valor) }

    fun validarFormulario(): Boolean {
        val validado = estado.updateAndGet { actual ->
            val errores = validarCampos(actual)
            actual.copy(
                errores = errores,
                formularioValido = !errores.hayErrores,
                validacionIntentada = true
            )
        }
        return validado.formularioValido
    }

    private fun actualizar(campo: Campo, transformar: (UsuarioUiState) -> UsuarioUiState) {
        estado.update { actual ->
            val nuevo = transformar(actual)
            val errores = validarCampos(nuevo)
            // Antes del primer envío solo se muestran errores de campos editados.
            val visibles = if (actual.validacionIntentada) errores else when (campo) {
                Campo.Nombre -> actual.errores.copy(nombre = errores.nombre)
                Campo.Correo -> actual.errores.copy(correo = errores.correo)
                Campo.Clave -> actual.errores.copy(clave = errores.clave)
                Campo.Direccion -> actual.errores.copy(direccion = errores.direccion)
                Campo.Terminos -> actual.errores.copy(terminosAceptados = errores.terminosAceptados)
            }
            nuevo.copy(errores = visibles, formularioValido = !errores.hayErrores)
        }
    }

    private fun validarCampos(usuario: UsuarioUiState) = UsuarioErrores(
        nombre = if (usuario.nombre.isBlank()) "Ingresa tu nombre." else null,
        correo = if (!CORREO.matches(usuario.correo.trim())) "Ingresa un correo válido." else null,
        clave = if (usuario.clave.isBlank() || usuario.clave.length < 6)
            "La clave debe tener al menos 6 caracteres." else null,
        direccion = if (usuario.direccion.isBlank()) "Ingresa tu dirección." else null,
        terminosAceptados = if (!usuario.terminosAceptados) "Debes aceptar los términos y condiciones." else null
    )

    private enum class Campo { Nombre, Correo, Clave, Direccion, Terminos }

    private companion object {
        val CORREO = Regex("[^\\s@]+@[^\\s@.]+(?:\\.[^\\s@.]+)+")
    }
}
