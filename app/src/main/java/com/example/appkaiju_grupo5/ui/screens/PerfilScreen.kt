package com.example.appkaiju_grupo5.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.appkaiju_grupo5.ui.theme.AppKaiju_Grupo5Theme

@Composable
fun PerfilScreen(modifier: Modifier = Modifier, onRegisterClick: () -> Unit = {}) {
    Box(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp).testTag("screen_profile"),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier.widthIn(max = 640.dp).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.padding(20.dp).size(64.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Text("Usuario de demostración", style = MaterialTheme.typography.headlineSmall)
            Text("Equipo Kaiju", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    PerfilDato("Rol", "Encargado de inventario")
                    HorizontalDivider()
                    PerfilDato("Centro de trabajo", "Bodega de ejemplo")
                    HorizontalDivider()
                    PerfilDato("Acceso", "Consulta de productos y stock")
                }
            }
            Text(
                "Este perfil es ficticio y se utiliza para demostrar la navegación de Kaiju.",
                style = MaterialTheme.typography.bodyMedium
            )
            Button(onClick = onRegisterClick, modifier = Modifier.fillMaxWidth().testTag("perfil_registro")) {
                Text("Registrar usuario")
            }
        }
    }
}

@Composable
private fun PerfilDato(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}

@Preview(name = "Perfil compacto", widthDp = 360, heightDp = 800, showBackground = true)
@Preview(name = "Perfil mediano", widthDp = 700, heightDp = 700, showBackground = true)
@Composable
private fun PerfilScreenPreview() {
    AppKaiju_Grupo5Theme { PerfilScreen() }
}
