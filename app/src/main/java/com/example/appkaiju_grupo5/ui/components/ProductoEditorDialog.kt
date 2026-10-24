package com.example.appkaiju_grupo5.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.appkaiju_grupo5.model.ProductoFormulario
import com.example.appkaiju_grupo5.viewmodel.EditorProductoState

@Composable
fun ProductoEditorDialog(
    editor: EditorProductoState,
    guardando: Boolean,
    error: String?,
    onChange: (ProductoFormulario) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    val form = editor.formulario
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (form.id == 0) "Nuevo producto" else "Editar producto") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CampoProducto("Nombre", form.nombre, editor.errores.nombre, "producto_nombre", guardando) { onChange(form.copy(nombre = it)) }
                CampoProducto("Categoría", form.categoria, editor.errores.categoria, "producto_categoria", guardando) { onChange(form.copy(categoria = it)) }
                CampoProducto("Stock", form.stock, editor.errores.stock, "producto_stock", guardando, true) { onChange(form.copy(stock = it)) }
                CampoProducto("Stock mínimo", form.stockMinimo, editor.errores.stockMinimo, "producto_minimo", guardando, true) { onChange(form.copy(stockMinimo = it)) }
                CampoProducto("Ubicación", form.ubicacion, editor.errores.ubicacion, "producto_ubicacion", guardando) { onChange(form.copy(ubicacion = it)) }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(onClick = onSave, enabled = !guardando, modifier = Modifier.testTag("producto_guardar")) {
                Text(if (guardando) "Guardando…" else "Guardar")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !guardando) { Text("Cancelar") } },
        modifier = Modifier.testTag("producto_editor")
    )
}

@Composable
private fun CampoProducto(
    label: String, value: String, error: String?, tag: String,
    guardando: Boolean, numerico: Boolean = false, onChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = true,
        enabled = !guardando,
        isError = error != null,
        supportingText = error?.let { { Text(it) } },
        keyboardOptions = KeyboardOptions(keyboardType = if (numerico) KeyboardType.Number else KeyboardType.Text),
        modifier = Modifier.fillMaxWidth().testTag(tag)
    )
}
