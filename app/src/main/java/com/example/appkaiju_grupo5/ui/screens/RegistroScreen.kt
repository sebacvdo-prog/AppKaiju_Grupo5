package com.example.appkaiju_grupo5.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.appkaiju_grupo5.viewmodel.UsuarioViewModel

@Composable
fun RegistroScreen(
    viewModel: UsuarioViewModel,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current

    Box(
        modifier = modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState())
            .padding(24.dp).testTag("screen_register"),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier.widthIn(max = 640.dp).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text("Registro de usuario", style = MaterialTheme.typography.headlineMedium)
            Text("Completa tus datos para revisar tu perfil de Kaiju.", style = MaterialTheme.typography.bodyLarge)
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = state.nombre,
                        onValueChange = viewModel::actualizarNombre,
                        label = { Text("Nombre") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
                        isError = state.errores.nombre != null,
                        supportingText = { state.errores.nombre?.let { Text(it) } },
                        modifier = Modifier.fillMaxWidth().testTag("registro_nombre")
                    )
                    OutlinedTextField(
                        value = state.correo,
                        onValueChange = viewModel::actualizarCorreo,
                        label = { Text("Correo electrónico") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                        isError = state.errores.correo != null,
                        supportingText = { state.errores.correo?.let { Text(it) } },
                        modifier = Modifier.fillMaxWidth().testTag("registro_correo")
                    )
                    OutlinedTextField(
                        value = state.clave,
                        onValueChange = viewModel::actualizarClave,
                        label = { Text("Clave") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
                        isError = state.errores.clave != null,
                        supportingText = { Text(state.errores.clave ?: "Al menos 6 caracteres.") },
                        modifier = Modifier.fillMaxWidth().testTag("registro_clave")
                    )
                    OutlinedTextField(
                        value = state.direccion,
                        onValueChange = viewModel::actualizarDireccion,
                        label = { Text("Dirección") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                        isError = state.errores.direccion != null,
                        supportingText = { state.errores.direccion?.let { Text(it) } },
                        modifier = Modifier.fillMaxWidth().testTag("registro_direccion")
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().toggleable(
                            value = state.terminosAceptados,
                            role = Role.Checkbox,
                            onValueChange = viewModel::actualizarTerminosAceptados
                        ).testTag("registro_terminos"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(checked = state.terminosAceptados, onCheckedChange = null)
                        Text("Acepto los términos y condiciones", style = MaterialTheme.typography.bodyMedium)
                    }
                    state.errores.terminosAceptados?.let {
                        Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                    Button(
                        onClick = {
                            focusManager.clearFocus()
                            if (viewModel.validarFormulario()) onContinue()
                        },
                        modifier = Modifier.fillMaxWidth().testTag("registro_continuar")
                    ) {
                        Text("Validar y continuar")
                    }
                }
            }
        }
    }
}
