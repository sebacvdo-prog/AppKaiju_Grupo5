package com.example.appkaiju_grupo5.navigation

sealed class NavigationEvent {
    data class NavigateTo(val screen: Screen) : NavigationEvent()
    data object Back : NavigationEvent()
}
