package com.example.appkaiju_grupo5.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.appkaiju_grupo5.device.FotoProductoStorage

@Composable
fun MiniaturaProducto(archivo: String, descripcion: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current.applicationContext
    val fotos = remember(context) { FotoProductoStorage(context) }
    var cargando by remember(archivo) { mutableStateOf(true) }
    val imagen by produceState<ImageBitmap?>(null, archivo) {
        value = null
        value = fotos.miniatura(archivo)?.asImageBitmap()
        cargando = false
    }
    val bitmap = imagen
    if (bitmap != null) Image(
        bitmap = bitmap, contentDescription = descripcion, contentScale = ContentScale.Crop,
        modifier = modifier.fillMaxWidth().height(140.dp).testTag("foto_producto")
    ) else Text(if (cargando) "Cargando fotografía…" else "Fotografía no disponible.",
        modifier = modifier.testTag("foto_pendiente"))
}
