package com.example.appkaiju_grupo5

import android.graphics.Bitmap
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.text.AnnotatedString
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.test.espresso.Espresso.pressBack
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RegistroNavigationTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun emptyFormStaysOnRegistrationAndShowsAllErrors() {
        openRegistration()

        continueToSummary()

        composeRule.onNodeWithTag("screen_register").assertIsDisplayed()
        composeRule.onNodeWithTag("screen_summary").assertDoesNotExist()
        composeRule.onNodeWithText("Ingresa tu nombre.").assertExists()
        composeRule.onNodeWithText("Ingresa un correo válido.").assertExists()
        composeRule.onNodeWithText("La clave debe tener al menos 6 caracteres.").assertExists()
        composeRule.onNodeWithText("Ingresa tu dirección.").assertExists()
        composeRule.onNodeWithText("Debes aceptar los términos y condiciones.").assertExists()
    }

    @Test
    fun errorsReactToEditsBeforeSubmitting() {
        openRegistration()
        composeRule.onNodeWithTag("registro_correo").performScrollTo().performTextInput("incorrecto")
        composeRule.onNodeWithText("Ingresa un correo válido.").assertExists()

        composeRule.onNodeWithTag("registro_correo").performTextReplacement("ana@example.cl")

        composeRule.onNodeWithText("Ingresa un correo válido.").assertDoesNotExist()
        composeRule.onNodeWithTag("registro_clave").performScrollTo().performTextInput("cinco")
        composeRule.onNodeWithText("La clave debe tener al menos 6 caracteres.").assertExists()
        composeRule.onNodeWithTag("registro_clave").performTextReplacement("Kaiju6")
        composeRule.onNodeWithText("La clave debe tener al menos 6 caracteres.").assertDoesNotExist()
    }

    @Test
    fun summaryUsesEnteredDataAndBackPreservesFormAndAllowsEditing() {
        openRegistration()
        fillValidForm()
        continueToSummary()

        assertSummary()
        composeRule.onNodeWithTag("nav_perfil").assertIsSelected()
        composeRule.onNodeWithText("Kaiju6").assertDoesNotExist()
        composeRule.onNodeWithText("Clave").assertDoesNotExist()
        composeRule.onNodeWithTag("registro_clave").assertDoesNotExist()
        captureSummaryEvidence()

        pressBack()

        composeRule.onNodeWithTag("registro_nombre").performScrollTo().assertTextContains("Ana Kaiju")
        composeRule.onNodeWithTag("registro_correo").performScrollTo().assertTextContains("ana@example.cl")
        composeRule.onNodeWithTag("registro_direccion").performScrollTo().assertTextContains("Avenida Ejemplo 123")
        composeRule.onNodeWithTag("registro_nombre").performScrollTo().performTextReplacement("Ana Actualizada")
        continueToSummary()
        assertSummary(expectedName = "Ana Actualizada")
        composeRule.onNodeWithTag("resumen_editar").performScrollTo().performClick()
        composeRule.onNodeWithTag("registro_nombre").performScrollTo().assertTextContains("Ana Actualizada")
    }

    @Test
    fun activityRecreationKeepsSharedStateAndAppBarBackReturnsThroughTheFlow() {
        openRegistration()
        fillValidForm()
        continueToSummary()

        composeRule.activityRule.scenario.recreate()

        assertSummary()
        composeRule.onNodeWithContentDescription("Volver al formulario").performClick()
        composeRule.onNodeWithTag("registro_nombre").performScrollTo().assertTextContains("Ana Kaiju")
        composeRule.onNodeWithContentDescription("Volver a Perfil").performClick()
        awaitScreen("screen_profile")
        composeRule.onNodeWithTag("perfil_registro").performScrollTo().performClick()
        composeRule.onNodeWithTag("registro_nombre").assert(
            SemanticsMatcher.expectValue(SemanticsProperties.EditableText, AnnotatedString(""))
        )
        composeRule.onNodeWithText("Ana Kaiju").assertDoesNotExist()
    }

    @Test
    fun drawerBackClosesMenuBeforeLeavingSummaryAndTopLevelNavigationStillWorks() {
        openRegistration()
        fillValidForm()
        continueToSummary()
        composeRule.onNodeWithContentDescription("Abrir menú").performClick()

        pressBack()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            !composeRule.onNodeWithTag("drawer_productos").isDisplayed()
        }
        assertSummary()
        composeRule.onNodeWithTag("nav_productos").performClick()
        awaitScreen("screen_products")
        composeRule.onNodeWithTag("nav_inicio").performClick()
        awaitScreen("screen_home")
    }

    private fun openRegistration() {
        composeRule.onNodeWithTag("nav_perfil").performClick()
        composeRule.onNodeWithTag("perfil_registro").performScrollTo().performClick()
        awaitScreen("screen_register")
    }

    private fun fillValidForm() {
        composeRule.onNodeWithTag("registro_nombre").performScrollTo().performTextInput("Ana Kaiju")
        composeRule.onNodeWithTag("registro_correo").performScrollTo().performTextInput("ana@example.cl")
        composeRule.onNodeWithTag("registro_clave").performScrollTo().performTextInput("Kaiju6")
        composeRule.onNodeWithTag("registro_direccion").performScrollTo().performTextInput("Avenida Ejemplo 123")
        composeRule.onNodeWithTag("registro_direccion").performImeAction()
        composeRule.onNodeWithTag("registro_terminos").performScrollTo().performClick()
    }

    private fun continueToSummary() {
        // El IME puede desplazar el formulario al editar: espera antes de ubicar el botón.
        closeSoftKeyboard()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            ViewCompat.getRootWindowInsets(composeRule.activity.window.decorView)
                ?.isVisible(WindowInsetsCompat.Type.ime()) == false
        }
        composeRule.onNodeWithTag("registro_continuar").performScrollTo().performClick()
    }

    private fun assertSummary(expectedName: String = "Ana Kaiju") {
        awaitScreen("screen_summary")
        composeRule.onNodeWithTag("resumen_nombre").assertTextEquals(expectedName)
        composeRule.onNodeWithTag("resumen_correo").assertTextEquals("ana@example.cl")
        composeRule.onNodeWithTag("resumen_direccion").assertTextEquals("Avenida Ejemplo 123")
    }

    private fun awaitScreen(tag: String) {
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onNodeWithTag(tag).isDisplayed()
        }
        composeRule.onNodeWithTag(tag).assertIsDisplayed()
    }

    private fun captureSummaryEvidence() {
        composeRule.runOnIdle {
            val instrumentation = InstrumentationRegistry.getInstrumentation()
            val file = File(instrumentation.targetContext.getExternalFilesDir(null), "guia11-resumen.png")
            file.outputStream().use { output ->
                check(instrumentation.uiAutomation.takeScreenshot().compress(Bitmap.CompressFormat.PNG, 100, output))
            }
        }
    }
}
