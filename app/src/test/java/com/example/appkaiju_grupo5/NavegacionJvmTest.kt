package com.example.appkaiju_grupo5

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.example.appkaiju_grupo5.data.external.FrankfurterApi
import com.example.appkaiju_grupo5.data.external.TipoCambioDto
import com.example.appkaiju_grupo5.data.external.TipoCambioRepository
import com.google.gson.JsonPrimitive
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import retrofit2.Response

class KaijuApplicationJvm : KaijuApplication() {
    internal override val tipoCambioRepository = TipoCambioRepository(FrankfurterApi {
        Response.success(TipoCambioDto("2026-11-16", "USD", "CLP", JsonPrimitive(997.08)))
    })
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = KaijuApplicationJvm::class, qualifiers = "w420dp-h860dp")
class NavegacionJvmTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun volverCierraPrimeroElMenuYConservaLaSeccionActiva() {
        compose.onNodeWithText("Ver productos").performScrollTo().performClick()
        compose.onNodeWithTag("screen_products").assertIsDisplayed()
        compose.onNodeWithTag("stock_filter").performClick()
        compose.onNodeWithContentDescription("Abrir menú").performClick()
        compose.onNodeWithTag("drawer_productos").assertIsDisplayed()

        compose.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        compose.onNodeWithTag("screen_products").assertIsDisplayed()
        compose.onNodeWithTag("nav_productos").assertIsSelected()
        compose.onNodeWithTag("stock_filter").assertIsSelected()

        compose.onNodeWithContentDescription("Volver a Inicio").performClick()
        compose.onNodeWithTag("screen_home").assertIsDisplayed()
    }

    @Test
    fun elMenuAbrePerfilYElRegistroConservaLaSeccionSeleccionada() {
        compose.onNodeWithContentDescription("Abrir menú").performClick()
        compose.onNodeWithTag("drawer_perfil").performClick()
        compose.onNodeWithTag("screen_profile").assertIsDisplayed()
        compose.onNodeWithTag("perfil_registro").performScrollTo().performClick()
        compose.onNodeWithTag("screen_register").assertIsDisplayed()
        compose.onNodeWithTag("nav_perfil").assertIsSelected()
        compose.onNodeWithContentDescription("Volver a Perfil").performClick()
        compose.onNodeWithTag("screen_profile").assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "w1100dp-h850dp")
    fun elPanelExpandidoPermiteCambiarSeccionYVolverAInicio() {
        compose.onNodeWithTag("permanent_drawer").assertIsDisplayed()
        compose.onNodeWithTag("drawer_productos").performClick()
        compose.onNodeWithTag("screen_products").assertIsDisplayed()
        compose.onNodeWithTag("drawer_inicio").performClick()
        compose.onNodeWithTag("screen_home").assertIsDisplayed()
        compose.onNodeWithTag("bottom_navigation").assertDoesNotExist()
    }
}
