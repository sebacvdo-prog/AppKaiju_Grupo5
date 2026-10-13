package com.example.appkaiju_grupo5.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
fun HomeCompactScreen() {

Column(
modifier = Modifier
.fillMaxSize()
.padding(24.dp),
verticalArrangement = Arrangement.Center,
horizontalAlignment = Alignment.CenterHorizontally
) {

Image(
painter = painterResource(id = R.drawable.kaiju_inventory),
contentDescription = "Ilustración de gestión de inventario",
modifier = Modifier.size(180.dp)
)

Spacer(modifier = Modifier.height(20.dp))

Text(
text = "Kaiju",
style = MaterialTheme.typography.headlineLarge
)

Spacer(modifier = Modifier.height(12.dp))

Text(
text = "Gestión y trazabilidad de inventario",
style = MaterialTheme.typography.bodyLarge
)

Spacer(modifier = Modifier.height(28.dp))

Button(
onClick = {
// La navegación se implementará posteriormente.
}
) {
Text("Ver productos")
}
}
}

@Preview(
showBackground = true,
widthDp = 360,
heightDp = 800
)
@Composable
fun HomeCompactScreenPreview() {
HomeCompactScreen()
}