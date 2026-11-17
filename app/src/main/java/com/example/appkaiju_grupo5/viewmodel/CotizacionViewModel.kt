package com.example.appkaiju_grupo5.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.appkaiju_grupo5.data.external.ConsultaCotizacionException
import com.example.appkaiju_grupo5.data.external.ErrorCotizacion
import com.example.appkaiju_grupo5.data.external.TipoCambioRepository
import com.example.appkaiju_grupo5.model.CotizacionUsdClp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal data class CotizacionUiState(
    val cotizacion: CotizacionUsdClp? = null,
    val cargando: Boolean = false,
    val error: ErrorCotizacion? = null
)

internal class CotizacionViewModel(private val repository: TipoCambioRepository) : ViewModel() {
    private val estado = MutableStateFlow(CotizacionUiState())
    val uiState = estado.asStateFlow()

    init {
        actualizar()
    }

    fun actualizar() {
        val anterior = estado.value
        if (anterior.cargando) return
        estado.value = anterior.copy(cargando = true, error = null)
        viewModelScope.launch {
            estado.value = try {
                CotizacionUiState(cotizacion = repository.consultar())
            } catch (error: ConsultaCotizacionException) {
                anterior.copy(error = error.motivo)
            }
        }
    }
}
