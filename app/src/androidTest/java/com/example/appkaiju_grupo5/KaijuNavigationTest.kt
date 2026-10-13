package com.example.appkaiju_grupo5

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.espresso.Espresso.pressBack
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class KaijuNavigationTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun homeButtonOpensInventoryAndStockFilterWorks() {
        composeRule.onNodeWithText("Ver productos").performClick()
        composeRule.onNodeWithTag("screen_products").assertIsDisplayed()
        composeRule.onNodeWithTag("stock_filter").performClick().assertIsSelected()
        composeRule.onNodeWithText("Guantes de trabajo").assertIsDisplayed()
        composeRule.onNodeWithText("Casco de seguridad").assertDoesNotExist()
    }

    @Test
    fun repeatedTabChangesReturnToHomeWithoutBuildingACircularStack() {
        repeat(3) {
            composeRule.onNodeWithTag("nav_productos").performClick()
            composeRule.onNodeWithTag("nav_perfil").performClick()
            composeRule.onNodeWithTag("nav_inicio").performClick()
        }
        composeRule.onNodeWithTag("nav_perfil").performClick()
        composeRule.onNodeWithTag("nav_productos").performClick()
        pressBack()

        composeRule.onNodeWithTag("screen_home").assertIsDisplayed()
        composeRule.onAllNodesWithTag("bottom_navigation").assertCountEquals(1)
    }

    @Test
    fun drawerOpensProfileAndAppBarReturnsHome() {
        composeRule.onNodeWithContentDescription("Abrir menú").performClick()
        composeRule.onNodeWithTag("drawer_perfil").performClick()
        composeRule.onNodeWithTag("screen_profile").assertIsDisplayed()
        composeRule.onNodeWithText("Usuario de demostración").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Volver a Inicio").performClick()
        composeRule.onNodeWithTag("screen_home").assertIsDisplayed()
    }

    @Test
    fun backClosesDrawerBeforeLeavingTheCurrentDestination() {
        composeRule.onNodeWithTag("nav_productos").performClick()
        composeRule.onNodeWithContentDescription("Abrir menú").performClick()
        composeRule.onNodeWithTag("drawer_productos").assertIsDisplayed()

        pressBack()

        composeRule.onNodeWithTag("screen_products").assertIsDisplayed()
        composeRule.onNodeWithTag("nav_productos").assertIsSelected()
        pressBack()
        composeRule.onNodeWithTag("screen_home").assertIsDisplayed()
    }

    @Test
    fun destinationAndInventoryFilterSurviveTabChangesAndActivityRecreation() {
        composeRule.onNodeWithTag("nav_productos").performClick()
        composeRule.onNodeWithTag("stock_filter").performClick()
        composeRule.onNodeWithTag("nav_perfil").performClick()
        composeRule.onNodeWithTag("nav_productos").performClick()
        composeRule.onNodeWithTag("stock_filter").assertIsSelected()

        composeRule.activityRule.scenario.recreate()
        composeRule.onNodeWithTag("screen_products").assertIsDisplayed()
        composeRule.onNodeWithTag("stock_filter").assertIsSelected()
        pressBack()
        composeRule.onNodeWithTag("screen_home").assertIsDisplayed()
    }
}
