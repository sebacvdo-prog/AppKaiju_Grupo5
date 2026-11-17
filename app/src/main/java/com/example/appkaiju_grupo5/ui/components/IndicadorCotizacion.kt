package com.example.appkaiju_grupo5.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.appkaiju_grupo5.R
import com.example.appkaiju_grupo5.data.external.ErrorCotizacion
import com.example.appkaiju_grupo5.viewmodel.CotizacionUiState
import com.example.appkaiju_grupo5.viewmodel.CotizacionViewModel
import java.text.NumberFormat
import java.util.Locale

@Composable
internal fun IndicadorCotizacion(viewModel: CotizacionViewModel) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    IndicadorCotizacion(estado, viewModel::actualizar)
}

@Composable
internal fun IndicadorCotizacion(estado: CotizacionUiState, onActualizar: () -> Unit) {
    Card(
        modifier = Modifier.widthIn(max = 360.dp).fillMaxWidth().testTag("indicador_cotizacion")
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(stringResource(R.string.cotizacion_titulo), style = MaterialTheme.typography.titleMedium)
            estado.cotizacion?.let { cotizacion ->
                val tasa = NumberFormat.getNumberInstance(Locale.forLanguageTag("es-CL")).apply {
                    minimumFractionDigits = 2
                    maximumFractionDigits = 2
                }.format(cotizacion.pesosPorDolar)
                val fecha = cotizacion.fechaReferencia.split('-').asReversed().joinToString("/")
                Text(stringResource(R.string.cotizacion_valor, tasa), style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.testTag("valor_cotizacion"))
                Text(stringResource(R.string.cotizacion_fecha, fecha), style = MaterialTheme.typography.bodyMedium)
            }
            Text(stringResource(R.string.cotizacion_fuente), style = MaterialTheme.typography.labelMedium)
            if (estado.cargando) {
                Text(stringResource(R.string.cotizacion_cargando), style = MaterialTheme.typography.bodySmall)
                LinearProgressIndicator(Modifier.fillMaxWidth())
            }
            estado.error?.let { error ->
                val mensaje = when (error) {
                    ErrorCotizacion.Conexion -> R.string.cotizacion_conexion
                    ErrorCotizacion.ServicioNoDisponible -> R.string.cotizacion_servicio
                    ErrorCotizacion.RespuestaInvalida -> R.string.cotizacion_respuesta
                }
                Text(stringResource(mensaje), color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.testTag("error_cotizacion"))
            }
            TextButton(
                onClick = onActualizar,
                enabled = !estado.cargando,
                modifier = Modifier.align(Alignment.End).testTag("actualizar_cotizacion")
            ) {
                val label = if (estado.error == null) R.string.cotizacion_actualizar else R.string.cotizacion_reintentar
                Text(stringResource(label))
            }
        }
    }
}
