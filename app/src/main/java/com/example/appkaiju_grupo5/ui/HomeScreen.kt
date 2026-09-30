package com.example.appkaiju_grupo5.ui

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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.appkaiju_grupo5.R

@Composable
fun HomeScreen() {

    // Scaffold proporciona la estructura base de la pantalla.
    Scaffold { innerPadding ->

        // Column organiza los elementos verticalmente y los centra en pantalla.
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // Imagen representativa del sistema de inventario Kaiju.
            Image(
                painter = painterResource(id = R.drawable.kaiju_inventory),
                contentDescription = "Ilustración de gestión de inventario",
                modifier = Modifier.size(220.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Título principal de la aplicación.
            Text(
                text = "Kaiju",
                style = MaterialTheme.typography.headlineLarge
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Descripción principal del propósito de la aplicación.
            Text(
                text = "Gestión y trazabilidad de inventario",
                style = MaterialTheme.typography.bodyLarge
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Botón principal de acceso a los productos.
            Button(
                onClick = {
                    // La navegación se implementará posteriormente.
                }
            ) {
                Text("Ver productos")
            }
        }
    }
}