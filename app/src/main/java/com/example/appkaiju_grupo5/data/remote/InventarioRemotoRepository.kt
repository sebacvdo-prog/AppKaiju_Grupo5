package com.example.appkaiju_grupo5.data.remote

import com.example.appkaiju_grupo5.model.PaginaProductosRemotos
import com.example.appkaiju_grupo5.model.DatosProductoRemoto
import com.example.appkaiju_grupo5.model.ProductoConEtag
import com.google.gson.JsonParseException
import com.google.gson.JsonParser
import com.google.gson.stream.MalformedJsonException
import retrofit2.Response
import java.io.EOFException
import java.io.IOException
import java.net.ConnectException
import java.util.UUID

internal enum class ErrorInventarioRemoto {
    Conexion, ServidorNoDisponible, RespuestaInvalida, Validacion, NoEncontrado, Conflicto, VersionDesactualizada
}

internal class ConsultaRemotaException(
    val motivo: ErrorInventarioRemoto,
    val codigo: String? = null
) : Exception()

internal class InventarioRemotoRepository(private val api: ProductoApiService) {
    suspend fun consultar(pagina: Int): PaginaProductosRemotos = solicitar {
        api.listar(pagina).verificar(200).cuerpo().aPaginaRemota(pagina)
    }

    suspend fun consultarProducto(uuid: UUID): ProductoConEtag = solicitar {
        api.consultar(uuid).verificar(200).recurso(uuid)
    }

    suspend fun crear(uuid: UUID, datos: DatosProductoRemoto): ProductoConEtag = solicitar {
        api.crear(datos.aDto(uuid)).verificar(201).recurso(uuid)
    }

    suspend fun editar(uuid: UUID, etag: String, datos: DatosProductoRemoto): ProductoConEtag = solicitar {
        api.editar(uuid, etag, datos.aDto()).verificar(200).recurso(uuid)
    }

    suspend fun archivar(uuid: UUID, etag: String) = solicitar {
        api.archivar(uuid, etag).verificar(204)
        Unit
    }

    suspend fun ajustar(uuid: UUID, operacionId: UUID, delta: Int) = solicitar {
        val respuesta = api.ajustar(uuid, AjusteStockDto(operacionId, delta)).verificar(200)
        val recibo = respuesta.cuerpo()
        require(recibo.uuid == uuid.toString() && recibo.operacionId == operacionId.toString() && recibo.delta == delta)
        require(requireNotNull(recibo.stock) >= 0)
        respuesta.etag(requireNotNull(recibo.version))
        Unit
    }

    private suspend fun <T> solicitar(operacion: suspend () -> T): T {
        try {
            return operacion()
        } catch (_: ConnectException) {
            throw ConsultaRemotaException(ErrorInventarioRemoto.ServidorNoDisponible)
        } catch (_: MalformedJsonException) {
            throw ConsultaRemotaException(ErrorInventarioRemoto.RespuestaInvalida)
        } catch (_: EOFException) {
            throw ConsultaRemotaException(ErrorInventarioRemoto.RespuestaInvalida)
        } catch (_: IOException) {
            throw ConsultaRemotaException(ErrorInventarioRemoto.Conexion)
        } catch (_: JsonParseException) {
            throw ConsultaRemotaException(ErrorInventarioRemoto.RespuestaInvalida)
        } catch (_: IllegalArgumentException) {
            throw ConsultaRemotaException(ErrorInventarioRemoto.RespuestaInvalida)
        }
    }

    private fun <T> Response<T>.verificar(esperado: Int): Response<T> {
        if (code() == esperado) return this
        val motivo = when (code()) {
            400, 422, 428 -> ErrorInventarioRemoto.Validacion
            404 -> ErrorInventarioRemoto.NoEncontrado
            409 -> ErrorInventarioRemoto.Conflicto
            412 -> ErrorInventarioRemoto.VersionDesactualizada
            in 500..599 -> ErrorInventarioRemoto.ServidorNoDisponible
            else -> ErrorInventarioRemoto.RespuestaInvalida
        }
        val codigo = errorBody()?.use { body ->
            runCatching { JsonParser.parseReader(body.charStream()).asJsonObject.get("codigo")?.asString }.getOrNull()
        }
        throw ConsultaRemotaException(motivo, codigo)
    }

    private fun <T> Response<T>.cuerpo(): T = requireNotNull(body())

    private fun Response<ProductoDto>.recurso(uuid: UUID): ProductoConEtag {
        val producto = cuerpo().aProductoRemoto()
        require(producto.uuid == uuid && !producto.archivado)
        return ProductoConEtag(producto, etag(producto.version))
    }

    private fun Response<*>.etag(version: Long): String {
        val cabecera = requireNotNull(headers()["ETag"])
        require(version >= 0 && cabecera == "\"$version\"")
        return cabecera
    }
}
