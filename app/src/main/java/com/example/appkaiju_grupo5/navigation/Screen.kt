package com.example.appkaiju_grupo5.navigation

sealed class Screen(val route: String, val title: String) {
    data object Home : Screen("inicio", "Inicio")
    data object Products : Screen("productos", "Productos")
    data object Profile : Screen("perfil", "Perfil")

    companion object {
        val destinations: List<Screen> = listOf(Home, Products, Profile)

        fun fromRoute(route: String?): Screen =
            destinations.firstOrNull { it.route == route } ?: Home
    }
}
