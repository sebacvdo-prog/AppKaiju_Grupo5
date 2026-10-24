package com.example.appkaiju_grupo5

import com.example.appkaiju_grupo5.model.UsuarioErrores
import com.example.appkaiju_grupo5.viewmodel.UsuarioViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UsuarioViewModelTest {
    @Test
    fun initialStateHasEmptyFieldsAndNoVisibleErrors() {
        val state = UsuarioViewModel().uiState.value

        assertEquals("", state.nombre)
        assertEquals("", state.correo)
        assertEquals("", state.clave)
        assertEquals("", state.direccion)
        assertFalse(state.terminosAceptados)
        assertFalse(state.formularioValido)
        assertFalse(state.validacionIntentada)
        assertFalse(state.errores.hayErrores)
    }

    @Test
    fun emptySubmissionReportsEveryRequiredField() {
        val viewModel = UsuarioViewModel()

        assertFalse(viewModel.validarFormulario())

        val state = viewModel.uiState.value
        assertNotNull(state.errores.nombre)
        assertNotNull(state.errores.correo)
        assertNotNull(state.errores.clave)
        assertNotNull(state.errores.direccion)
        assertNotNull(state.errores.terminosAceptados)
        assertTrue(state.validacionIntentada)
        assertFalse(state.formularioValido)
    }

    @Test
    fun eachUpdatePreservesOtherFieldsAndPreviousStateIsImmutable() {
        val viewModel = validForm()
        val previous = viewModel.uiState.value

        viewModel.actualizarNombre("María Kaiju")
        assertEquals(previous.copy(nombre = "María Kaiju"), viewModel.uiState.value)
        viewModel.actualizarCorreo("maria@example.cl")
        viewModel.actualizarClave("nueva6")
        viewModel.actualizarDireccion("Pasaje Ejemplo 456")
        viewModel.actualizarTerminosAceptados(false)

        val state = viewModel.uiState.value
        assertEquals("María Kaiju", state.nombre)
        assertEquals("maria@example.cl", state.correo)
        assertEquals("nueva6", state.clave)
        assertEquals("Pasaje Ejemplo 456", state.direccion)
        assertFalse(state.terminosAceptados)
        assertEquals("Ana Kaiju", previous.nombre)
        assertTrue(previous.terminosAceptados)
    }

    @Test
    fun blankNameAndAddressAreRejectedIncludingWhitespace() {
        val viewModel = validForm()

        viewModel.actualizarNombre(" \t ")
        viewModel.actualizarDireccion("  \n ")

        assertFalse(viewModel.validarFormulario())
        assertNotNull(viewModel.uiState.value.errores.nombre)
        assertNotNull(viewModel.uiState.value.errores.direccion)
    }

    @Test
    fun nameAndAddressDoNotHaveUnrequestedLengthRestrictions() {
        val viewModel = validForm()
        viewModel.actualizarNombre("Á")
        viewModel.actualizarDireccion("1")

        assertTrue(viewModel.validarFormulario())
    }

    @Test
    fun invalidEmailsAreRejected() {
        val viewModel = validForm()
        listOf("", "correo", "ana@", "@example.cl", "ana@example", "ana@@example.cl", "ana kaiju@example.cl", "ana@example..cl").forEach { correo ->
            viewModel.actualizarCorreo(correo)

            assertFalse("Se aceptó un correo inválido", viewModel.validarFormulario())
            assertNotNull(viewModel.uiState.value.errores.correo)
        }
    }

    @Test
    fun commonEmailsAndSurroundingWhitespaceAreAccepted() {
        val viewModel = validForm()
        listOf("ana@example.cl", "ana.kaiju+registro@bodega.example.com", " ANA@example.cl ").forEach { correo ->
            viewModel.actualizarCorreo(correo)

            assertTrue(viewModel.validarFormulario())
            assertNull(viewModel.uiState.value.errores.correo)
        }
    }

    @Test
    fun passwordMustHaveSixCharactersAndCannotBeBlank() {
        val viewModel = validForm()
        listOf("", "12345", "      ").forEach { clave ->
            viewModel.actualizarClave(clave)
            assertFalse(viewModel.validarFormulario())
            assertNotNull(viewModel.uiState.value.errores.clave)
        }

        viewModel.actualizarClave("123456")

        assertTrue(viewModel.validarFormulario())
        assertNull(viewModel.uiState.value.errores.clave)
    }

    @Test
    fun termsAreMandatoryAndRevokingThemInvalidatesTheFormImmediately() {
        val viewModel = validForm()
        assertTrue(viewModel.validarFormulario())

        viewModel.actualizarTerminosAceptados(false)

        assertFalse(viewModel.uiState.value.formularioValido)
        assertNotNull(viewModel.uiState.value.errores.terminosAceptados)
        assertFalse(viewModel.validarFormulario())
        viewModel.actualizarTerminosAceptados(true)
        assertTrue(viewModel.uiState.value.formularioValido)
        assertNull(viewModel.uiState.value.errores.terminosAceptados)
    }

    @Test
    fun editedFieldsAreValidatedBeforeSubmittingWithoutShowingUntouchedErrors() {
        val viewModel = UsuarioViewModel()

        viewModel.actualizarCorreo("incorrecto")

        assertNotNull(viewModel.uiState.value.errores.correo)
        assertNull(viewModel.uiState.value.errores.nombre)
        assertNull(viewModel.uiState.value.errores.clave)
        assertNull(viewModel.uiState.value.errores.direccion)
        assertNull(viewModel.uiState.value.errores.terminosAceptados)
        viewModel.actualizarCorreo("ana@example.cl")
        assertNull(viewModel.uiState.value.errores.correo)
        assertFalse(viewModel.uiState.value.formularioValido)
    }

    @Test
    fun correctingAllErrorsAfterSubmissionUpdatesValidityWithoutSubmittingAgain() {
        val viewModel = UsuarioViewModel()
        assertFalse(viewModel.validarFormulario())

        fillForm(viewModel)

        assertTrue(viewModel.uiState.value.formularioValido)
        assertFalse(viewModel.uiState.value.errores.hayErrores)
        assertTrue(viewModel.validarFormulario())
        viewModel.actualizarClave("corta")
        assertFalse(viewModel.uiState.value.formularioValido)
        assertNotNull(viewModel.uiState.value.errores.clave)
    }

    @Test
    fun validSubmissionPreservesTheEnteredDataForTheSummary() {
        val viewModel = validForm()
        val before = viewModel.uiState.value

        assertTrue(viewModel.validarFormulario())

        assertEquals(before.copy(validacionIntentada = true), viewModel.uiState.value)
        assertEquals("Ana Kaiju", viewModel.uiState.value.nombre)
        assertEquals("ana@example.cl", viewModel.uiState.value.correo)
        assertEquals("Avenida Ejemplo 123", viewModel.uiState.value.direccion)
    }

    @Test
    fun termsErrorAloneCountsAsAnError() {
        assertTrue(UsuarioErrores(terminosAceptados = "Acepta los términos").hayErrores)
        assertFalse(UsuarioErrores().hayErrores)
    }

    private fun validForm() = UsuarioViewModel().also(::fillForm)

    private fun fillForm(viewModel: UsuarioViewModel) {
        viewModel.actualizarNombre("Ana Kaiju")
        viewModel.actualizarCorreo("ana@example.cl")
        viewModel.actualizarClave("Kaiju6")
        viewModel.actualizarDireccion("Avenida Ejemplo 123")
        viewModel.actualizarTerminosAceptados(true)
    }
}
