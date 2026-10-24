package com.example.appkaiju_grupo5

import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.appkaiju_grupo5.ui.screens.ResumenScreen
import com.example.appkaiju_grupo5.ui.theme.AppKaiju_Grupo5Theme
import com.example.appkaiju_grupo5.viewmodel.UsuarioViewModel
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ResumenScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun summaryReactsToSharedViewModelUpdatesAndNeverRendersThePassword() {
        val viewModel = UsuarioViewModel()
        viewModel.actualizarNombre("Ana Kaiju")
        viewModel.actualizarCorreo("ana@example.cl")
        viewModel.actualizarClave("clave-privada")
        viewModel.actualizarDireccion("Avenida Ejemplo 123")
        viewModel.actualizarTerminosAceptados(true)
        viewModel.validarFormulario()
        composeRule.setContent {
            AppKaiju_Grupo5Theme { ResumenScreen(viewModel = viewModel, onEdit = {}) }
        }

        composeRule.onNodeWithTag("resumen_nombre").assertTextEquals("Ana Kaiju")
        composeRule.runOnIdle {
            viewModel.actualizarNombre("María Kaiju")
            viewModel.actualizarCorreo("maria@example.cl")
            viewModel.actualizarDireccion("Pasaje Ejemplo 456")
        }

        composeRule.onNodeWithTag("resumen_nombre").assertTextEquals("María Kaiju")
        composeRule.onNodeWithTag("resumen_correo").assertTextEquals("maria@example.cl")
        composeRule.onNodeWithTag("resumen_direccion").assertTextEquals("Pasaje Ejemplo 456")
        composeRule.onNodeWithText("clave-privada").assertDoesNotExist()
        composeRule.onNodeWithText("Contraseña", substring = true).assertDoesNotExist()
    }
}
