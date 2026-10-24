package com.example.appkaiju_grupo5.data

import com.example.appkaiju_grupo5.model.Producto

/** Solo se usan al crear la base por primera vez y en las vistas previas. */
object ProductosIniciales {
    val productos = listOf(
        Producto(1, "Casco de seguridad", "Protección", 24, 8, "Estante A-01"),
        Producto(2, "Guantes de trabajo", "Protección", 6, 10, "Estante A-02"),
        Producto(3, "Taladro inalámbrico", "Herramientas", 12, 4, "Estante B-01"),
        Producto(4, "Caja organizadora", "Almacenamiento", 32, 10, "Estante C-01"),
        Producto(5, "Linterna recargable", "Herramientas", 3, 5, "Estante B-02"),
        Producto(6, "Cinta de embalaje", "Insumos", 48, 12, "Estante C-02")
    )
}
