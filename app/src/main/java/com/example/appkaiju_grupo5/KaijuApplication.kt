package com.example.appkaiju_grupo5

import android.app.Application
import com.example.appkaiju_grupo5.data.InventarioRepository
import com.example.appkaiju_grupo5.data.RoomInventarioRepository
import com.example.appkaiju_grupo5.data.external.ClienteFrankfurter
import com.example.appkaiju_grupo5.data.external.TipoCambioRepository
import com.example.appkaiju_grupo5.data.local.KaijuDatabase
import com.example.appkaiju_grupo5.data.remote.ClienteInventario
import com.example.appkaiju_grupo5.data.remote.InventarioRemotoRepository
import com.example.appkaiju_grupo5.device.FotoProductoStorage

open class KaijuApplication : Application() {
    private val database by lazy { KaijuDatabase.crear(this) }
    val inventarioRepository: InventarioRepository by lazy { RoomInventarioRepository(database.productoDao()) }
    internal open val tipoCambioRepository by lazy { TipoCambioRepository(ClienteFrankfurter.crear()) }
    internal val inventarioRemotoRepository: InventarioRemotoRepository? by lazy {
        getString(R.string.kaiju_api_base_url).takeIf { it.isNotBlank() }?.let {
            InventarioRemotoRepository(ClienteInventario.crear(it))
        }
    }
    val fotosProducto by lazy { FotoProductoStorage(this) }
}
