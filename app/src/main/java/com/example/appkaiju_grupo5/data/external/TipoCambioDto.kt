package com.example.appkaiju_grupo5.data.external

import com.example.appkaiju_grupo5.model.CotizacionUsdClp
import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName
import java.text.ParsePosition
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

internal data class TipoCambioDto(
    @SerializedName("date") val fecha: String?,
    @SerializedName("base") val base: String?,
    @SerializedName("quote") val moneda: String?,
    @SerializedName("rate") val valor: JsonElement?
)

private val fechaIso = Regex("[0-9]{4}-[0-9]{2}-[0-9]{2}")

internal fun TipoCambioDto.aCotizacion(): CotizacionUsdClp {
    require(base == "USD" && moneda == "CLP")
    val fecha = requireNotNull(fecha)
    require(fecha.matches(fechaIso))
    val formato = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).apply {
        isLenient = false
        timeZone = TimeZone.getTimeZone("UTC")
    }
    val posicion = ParsePosition(0)
    require(formato.parse(fecha, posicion) != null && posicion.index == fecha.length)

    val valor = requireNotNull(valor)
    require(valor.isJsonPrimitive && valor.asJsonPrimitive.isNumber)
    val tasa = valor.asBigDecimal
    val aproximacion = tasa.toDouble()
    require(aproximacion > 0 && aproximacion.isFinite())
    return CotizacionUsdClp(tasa, fecha)
}
