package com.example.appkaiju_grupo5

import android.app.Application
import com.example.appkaiju_grupo5.data.InventarioRepository
import com.example.appkaiju_grupo5.data.RoomInventarioRepository
import com.example.appkaiju_grupo5.data.local.KaijuDatabase

class KaijuApplication : Application() {
    private val database by lazy { KaijuDatabase.crear(this) }
    val inventarioRepository: InventarioRepository by lazy { RoomInventarioRepository(database.productoDao()) }
}
