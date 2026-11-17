package com.example.appkaiju_grupo5.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.UUID

internal interface ProductoApiService {
    @GET("api/v1/productos")
    suspend fun listar(@Query("pagina") pagina: Int): Response<PaginaProductosDto>

    @GET("api/v1/productos/{uuid}")
    suspend fun consultar(@Path("uuid") uuid: UUID): Response<ProductoDto>

    @POST("api/v1/productos")
    suspend fun crear(@Body producto: ProductoEscrituraDto): Response<ProductoDto>

    @PUT("api/v1/productos/{uuid}")
    suspend fun editar(
        @Path("uuid") uuid: UUID, @Header("If-Match") etag: String,
        @Body producto: ProductoEscrituraDto
    ): Response<ProductoDto>

    @DELETE("api/v1/productos/{uuid}")
    suspend fun archivar(@Path("uuid") uuid: UUID, @Header("If-Match") etag: String): Response<Unit>

    @POST("api/v1/productos/{uuid}/ajustes-stock")
    suspend fun ajustar(@Path("uuid") uuid: UUID, @Body ajuste: AjusteStockDto): Response<AjusteAplicadoDto>
}
