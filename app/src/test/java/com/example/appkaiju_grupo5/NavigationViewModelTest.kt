package com.example.appkaiju_grupo5

import com.example.appkaiju_grupo5.navigation.NavigationEvent
import com.example.appkaiju_grupo5.navigation.Screen
import com.example.appkaiju_grupo5.viewmodel.NavigationViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class NavigationViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun navigationEventsKeepTheirOrder() = runTest {
        val viewModel = NavigationViewModel()
        viewModel.navigateTo(Screen.Products)
        viewModel.navigateTo(Screen.Profile)

        assertEquals(NavigationEvent.NavigateTo(Screen.Products), viewModel.navigationEvents.first())
        assertEquals(NavigationEvent.NavigateTo(Screen.Profile), viewModel.navigationEvents.first())
    }

    @Test
    fun consumedEventIsNotReplayedToANewCollector() = runTest {
        val viewModel = NavigationViewModel()
        viewModel.navigateTo(Screen.Products)
        viewModel.navigationEvents.first()

        assertNull(withTimeoutOrNull(1) { viewModel.navigationEvents.first() })
    }

    @Test
    fun backIsAnIndependentNavigationEvent() = runTest {
        val viewModel = NavigationViewModel()
        viewModel.goBack()

        assertEquals(NavigationEvent.Back, viewModel.navigationEvents.first())
    }
}
