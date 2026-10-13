package com.example.appkaiju_grupo5.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.PermanentDrawerSheet
import androidx.compose.material3.PermanentNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.appkaiju_grupo5.R
import com.example.appkaiju_grupo5.navigation.KaijuNavHost
import com.example.appkaiju_grupo5.navigation.NavigationEvent
import com.example.appkaiju_grupo5.navigation.Screen
import com.example.appkaiju_grupo5.navigation.navigateToTopLevel
import com.example.appkaiju_grupo5.ui.screens.AdaptiveHomeScreen
import com.example.appkaiju_grupo5.ui.theme.AppKaiju_Grupo5Theme
import com.example.appkaiju_grupo5.ui.utils.WindowSize
import com.example.appkaiju_grupo5.ui.utils.getWindowSize
import com.example.appkaiju_grupo5.viewmodel.NavigationViewModel
import kotlinx.coroutines.launch

@Composable
fun KaijuApp(navigationViewModel: NavigationViewModel = viewModel()) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentScreen = Screen.fromRoute(backStackEntry?.destination?.route)
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(navController, navigationViewModel, lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            navigationViewModel.navigationEvents.collect { event ->
                when (event) {
                    is NavigationEvent.NavigateTo -> navController.navigateToTopLevel(event.screen)
                    NavigationEvent.Back -> navController.popBackStack()
                }
            }
        }
    }

    KaijuAppLayout(
        currentScreen = currentScreen,
        windowSize = getWindowSize(),
        onNavigate = navigationViewModel::navigateTo,
        onBack = navigationViewModel::goBack
    ) { modifier ->
        KaijuNavHost(
            navController = navController,
            onProductsClick = { navigationViewModel.navigateTo(Screen.Products) },
            modifier = modifier
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun KaijuAppLayout(
    currentScreen: Screen,
    windowSize: WindowSize,
    onNavigate: (Screen) -> Unit,
    onBack: () -> Unit,
    content: @Composable (Modifier) -> Unit
) {
    val expanded = windowSize == WindowSize.Expanded
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    LaunchedEffect(windowSize) { drawerState.close() }

    val scaffoldContent: @Composable () -> Unit = {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(if (currentScreen == Screen.Home) "Kaiju" else currentScreen.title) },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    ),
                    navigationIcon = {
                        if (currentScreen != Screen.Home) {
                            IconButton(onClick = onBack) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver a Inicio")
                            }
                        } else if (!expanded) {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(Icons.Default.Menu, contentDescription = "Abrir menú")
                            }
                        }
                    },
                    actions = {
                        if (!expanded && currentScreen != Screen.Home) {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(Icons.Default.Menu, contentDescription = "Abrir menú")
                            }
                        }
                    }
                )
            },
            bottomBar = {
                if (!expanded) {
                    NavigationBar(modifier = Modifier.testTag("bottom_navigation")) {
                        Screen.destinations.forEach { screen ->
                            NavigationBarItem(
                                selected = screen == currentScreen,
                                onClick = { onNavigate(screen) },
                                icon = { Icon(screen.icon(), contentDescription = null) },
                                label = { Text(screen.title) },
                                modifier = Modifier.testTag("nav_${screen.route}")
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            content(Modifier.fillMaxSize().padding(innerPadding).consumeWindowInsets(innerPadding))
        }
    }

    if (expanded) {
        PermanentNavigationDrawer(
            drawerContent = {
                PermanentDrawerSheet(modifier = Modifier.width(240.dp).testTag("permanent_drawer")) {
                    KaijuDrawerContent(currentScreen, onNavigate)
                }
            },
            content = scaffoldContent
        )
    } else {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet {
                    KaijuDrawerContent(currentScreen) { screen ->
                        scope.launch {
                            drawerState.close()
                            onNavigate(screen)
                        }
                    }
                }
            },
            content = scaffoldContent
        )
    }

    // El NavHost registra sus callbacks en una subcomposición. Al abrir el menú,
    // este callback se registra al final y toma prioridad sobre la pila de rutas.
    if (!expanded && drawerState.isOpen) {
        BackHandler {
            scope.launch { drawerState.close() }
        }
    }
}

@Composable
private fun KaijuDrawerContent(currentScreen: Screen, onNavigate: (Screen) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(24.dp)) {
        Image(
            painter = painterResource(R.drawable.kaiju_inventory),
            contentDescription = null,
            modifier = Modifier.size(72.dp)
        )
        Text("Kaiju", style = MaterialTheme.typography.headlineMedium)
        Text("Tu inventario, bajo control", style = MaterialTheme.typography.bodyMedium)
    }
    HorizontalDivider(modifier = Modifier.padding(bottom = 12.dp))
    Screen.destinations.forEach { screen ->
        NavigationDrawerItem(
            label = { Text(screen.title) },
            icon = { Icon(screen.icon(), contentDescription = null) },
            selected = screen == currentScreen,
            onClick = { onNavigate(screen) },
            modifier = Modifier.padding(horizontal = 12.dp).testTag("drawer_${screen.route}")
        )
    }
}

private fun Screen.icon(): ImageVector = when (this) {
    Screen.Home -> Icons.Default.Home
    Screen.Products -> Icons.Default.ShoppingCart
    Screen.Profile -> Icons.Default.Person
}

@Preview(name = "Compacto", widthDp = 360, heightDp = 800, showBackground = true)
@Preview(name = "Mediano", widthDp = 700, heightDp = 700, showBackground = true)
@Preview(name = "Expandido", widthDp = 1100, heightDp = 800, showBackground = true)
@Composable
private fun KaijuAppPreview() {
    AppKaiju_Grupo5Theme {
        KaijuAppLayout(Screen.Home, getWindowSize(), onNavigate = {}, onBack = {}) { modifier ->
            AdaptiveHomeScreen(modifier = modifier)
        }
    }
}
