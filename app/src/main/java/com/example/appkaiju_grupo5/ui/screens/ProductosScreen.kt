package com.example.appkaiju_grupo5.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.appkaiju_grupo5.data.InventarioRepository
import com.example.appkaiju_grupo5.model.Producto
import com.example.appkaiju_grupo5.ui.theme.AppKaiju_Grupo5Theme
import com.example.appkaiju_grupo5.viewmodel.ProductosUiState

@Composable
fun ProductosScreen(
    state: ProductosUiState,
    onToggleStockFilter: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 260.dp),
        state = rememberLazyGridState(),
        contentPadding = PaddingValues(24.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier.fillMaxSize().testTag("screen_products")
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Inventario", style = MaterialTheme.typography.headlineMedium)
                Text("Productos para tu operación diaria", style = MaterialTheme.typography.bodyLarge)
                Text("Datos ficticios de demostración", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    InventarioResumen("Productos", state.totalProductos, Modifier.weight(1f))
                    InventarioResumen("Stock bajo", state.productosConStockBajo, Modifier.weight(1f))
                }
                FilterChip(
                    selected = state.soloStockBajo,
                    onClick = onToggleStockFilter,
                    label = { Text("Solo stock bajo") },
                    modifier = Modifier.testTag("stock_filter")
                )
            }
        }
        items(items = state.productos, key = { it.id }) { producto ->
            ProductoCard(producto)
        }
        if (state.productos.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text("No hay productos que coincidan con el filtro.")
            }
        }
    }
}

@Composable
private fun InventarioResumen(label: String, value: Int, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.secondaryContainer
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(value.toString(), style = MaterialTheme.typography.headlineMedium)
            Text(label, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun ProductoCard(producto: Producto) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth().testTag("product_${producto.id}"),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(producto.categoria, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Text(producto.nombre, style = MaterialTheme.typography.titleLarge)
            Text(producto.ubicacion, style = MaterialTheme.typography.bodyMedium)
            Text("Stock: ${producto.stock} unidades", style = MaterialTheme.typography.titleMedium)
            Surface(
                shape = MaterialTheme.shapes.small,
                color = if (producto.requiereReposicion) MaterialTheme.colorScheme.errorContainer
                else MaterialTheme.colorScheme.tertiaryContainer
            ) {
                Text(
                    text = if (producto.requiereReposicion) "Reponer stock" else "Stock disponible",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}

@Preview(name = "Productos compacto", widthDp = 360, heightDp = 800, showBackground = true)
@Preview(name = "Productos expandido", widthDp = 1000, heightDp = 700, showBackground = true)
@Composable
private fun ProductosScreenPreview() {
    val productos = InventarioRepository.obtenerProductos()
    AppKaiju_Grupo5Theme {
        ProductosScreen(
            state = ProductosUiState(productos, false, productos.size, productos.count { it.requiereReposicion }),
            onToggleStockFilter = {}
        )
    }
}
