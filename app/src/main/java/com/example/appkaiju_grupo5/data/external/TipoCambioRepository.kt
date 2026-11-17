package com.example.appkaiju_grupo5.data.external

import com.example.appkaiju_grupo5.model.CotizacionUsdClp
import com.google.gson.JsonParseException
import com.google.gson.stream.MalformedJsonException
import java.io.EOFException
import java.io.IOException

internal enum class ErrorCotizacion {
    Conexion, ServicioNoDisponible, RespuestaInvalida
}

internal class ConsultaCotizacionException(val motivo: ErrorCotizacion) : Exception()

internal class TipoCambioRepository(private val api: FrankfurterApi) {
    suspend fun consultar(): CotizacionUsdClp {
        try {
            val respuesta = api.cotizacionUsdClp()
            if (!respuesta.isSuccessful) {
                respuesta.errorBody()?.close()
                throw ConsultaCotizacionException(ErrorCotizacion.ServicioNoDisponible)
            }
            require(respuesta.code() == 200)
            return requireNotNull(respuesta.body()).aCotizacion()
        } catch (_: MalformedJsonException) {
            throw ConsultaCotizacionException(ErrorCotizacion.RespuestaInvalida)
        } catch (_: EOFException) {
            throw ConsultaCotizacionException(ErrorCotizacion.RespuestaInvalida)
        } catch (_: IOException) {
            throw ConsultaCotizacionException(ErrorCotizacion.Conexion)
        } catch (_: JsonParseException) {
            throw ConsultaCotizacionException(ErrorCotizacion.RespuestaInvalida)
        } catch (_: IllegalArgumentException) {
            throw ConsultaCotizacionException(ErrorCotizacion.RespuestaInvalida)
        }
    }
}
