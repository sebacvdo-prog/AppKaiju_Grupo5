package com.example.appkaiju_grupo5.data.external

import retrofit2.Response
import retrofit2.http.GET

internal fun interface FrankfurterApi {
    @GET("v2/rate/USD/CLP")
    suspend fun cotizacionUsdClp(): Response<TipoCambioDto>
}
