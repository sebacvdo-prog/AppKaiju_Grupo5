package com.example.appkaiju_grupo5.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
fun HomeMediumScreen(onProductsClick: () -> Unit = {}) {

Row(
modifier = Modifier
.fillMaxSize()
.verticalScroll(rememberScrollState())
.padding(32.dp),
verticalAlignment = Alignment.CenterVertically,
horizontalArrangement = Arrangement.Center
) {

// Imagen ubicada a la izquierda en pantallas medianas.
Image(
painter = painterResource(id = R.drawable.kaiju_inventory),
contentDescription = "Ilustración de gestión de inventario",
modifier = Modifier.size(220.dp)
)

Spacer(modifier = Modifier.width(32.dp))

// Contenido principal ubicado a la derecha.
Column(
modifier = Modifier.weight(1f),
verticalArrangement = Arrangement.Center,
horizontalAlignment = Alignment.Start
) {

Text(
text = "Kaiju",
style = MaterialTheme.typography.headlineLarge
)

Text(
text = "Gestión y trazabilidad de inventario",
style = MaterialTheme.typography.bodyLarge
)

Spacer(modifier = Modifier.size(24.dp))

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
widthDp = 700,
heightDp = 500
)
@Composable
fun HomeMediumScreenPreview() {
HomeMediumScreen()
}
