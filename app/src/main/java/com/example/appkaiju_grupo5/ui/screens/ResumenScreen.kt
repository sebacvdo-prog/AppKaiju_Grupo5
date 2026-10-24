package com.example.appkaiju_grupo5.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.appkaiju_grupo5.viewmodel.UsuarioViewModel

@Composable
fun ResumenScreen(
    viewModel: UsuarioViewModel,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Box(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(24.dp).testTag("screen_summary"),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier.widthIn(max = 640.dp).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text("Resumen del registro", style = MaterialTheme.typography.headlineMedium)
            Text("Revisa los datos de tu perfil de Kaiju.", style = MaterialTheme.typography.bodyLarge)
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    ResumenDato("Nombre", state.nombre, "resumen_nombre")
                    HorizontalDivider()
                    ResumenDato("Correo electrónico", state.correo, "resumen_correo")
                    HorizontalDivider()
                    ResumenDato("Dirección", state.direccion, "resumen_direccion")
                    HorizontalDivider()
                    Text("Términos y condiciones aceptados", style = MaterialTheme.typography.bodyMedium)
                }
            }
            OutlinedButton(onClick = onEdit, modifier = Modifier.fillMaxWidth().testTag("resumen_editar")) {
                Text("Volver al formulario")
            }
        }
    }
}

@Composable
private fun ResumenDato(label: String, value: String, tag: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        Text(value, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.testTag(tag))
    }
}
