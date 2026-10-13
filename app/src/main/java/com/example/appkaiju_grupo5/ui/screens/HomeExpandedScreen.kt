package com.example.appkaiju_grupo5.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.appkaiju_grupo5.R

@Composable
fun HomeExpandedScreen(onProductsClick: () -> Unit = {}) {

Row(
modifier = Modifier
.fillMaxSize()
.verticalScroll(rememberScrollState())
.padding(48.dp),
verticalAlignment = Alignment.CenterVertically,
horizontalArrangement = Arrangement.SpaceEvenly
) {

// Imagen de mayor tamaño para pantallas grandes.
Image(
painter = painterResource(id = R.drawable.kaiju_inventory),
contentDescription = "Ilustración de gestión de inventario",
modifier = Modifier.weight(0.4f).sizeIn(maxWidth = 300.dp, maxHeight = 300.dp).aspectRatio(1f)
)

Spacer(modifier = Modifier.width(48.dp))

// Información principal de la aplicación.
Column(
modifier = Modifier.weight(0.6f),
verticalArrangement = Arrangement.Center,
horizontalAlignment = Alignment.Start
) {

Text(
text = "Kaiju",
style = MaterialTheme.typography.displayMedium
)

Text(
text = "Gestión y trazabilidad de inventario",
style = MaterialTheme.typography.headlineSmall
)

Spacer(modifier = Modifier.size(32.dp))

Text(
text = "Controla productos, stock y movimientos de inventario.",
style = MaterialTheme.typography.bodyLarge
)

Spacer(modifier = Modifier.size(32.dp))

Button(
onClick = onProductsClick
) {
Text("Ver productos")
}
}
}
}

@Preview(
showBackground = true,
widthDp = 1000,
heightDp = 700
)
@Composable
fun HomeExpandedScreenPreview() {
HomeExpandedScreen()
}
