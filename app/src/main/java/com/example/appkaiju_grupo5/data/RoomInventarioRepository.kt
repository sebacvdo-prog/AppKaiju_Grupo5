package com.example.appkaiju_grupo5.data

import com.example.appkaiju_grupo5.data.local.ProductoDao
import com.example.appkaiju_grupo5.data.local.toEntity
import com.example.appkaiju_grupo5.data.local.toProducto
import com.example.appkaiju_grupo5.model.Producto
import com.example.appkaiju_grupo5.device.FotosProducto
import kotlinx.coroutines.flow.map

class RoomInventarioRepository(private val dao: ProductoDao) : InventarioRepository {
    override fun observarProductos() = dao.observarProductos().map { lista -> lista.map { it.toProducto() } }

    override suspend fun guardar(producto: Producto): Long {
        require(producto.id >= 0) { "El identificador no es válido." }
        require(producto.nombre.isNotBlank()) { "Ingresa el nombre del producto." }
        require(producto.categoria.isNotBlank()) { "Ingresa la categoría." }
        require(producto.ubicacion.isNotBlank()) { "Ingresa la ubicación." }
        require(producto.stock >= 0 && producto.stockMinimo >= 0) { "El stock no puede ser negativo." }
        val entity = producto.copy(
            nombre = producto.nombre.trim(), categoria = producto.categoria.trim(),
            ubicacion = producto.ubicacion.trim()
        ).toEntity()
        if (producto.id == 0) return dao.insertar(entity)
        check(dao.actualizar(entity.id, entity.nombre, entity.categoria, entity.stock,
            entity.stockMinimo, entity.ubicacion) == 1) { "El producto ya no está disponible." }
        return producto.id.toLong()
    }

    override suspend fun cambiarStock(id: Int, cantidad: Int) {
        require(cantidad != 0) { "Indica una cantidad distinta de cero." }
        check(dao.cambiarStock(id, cantidad) == 1) {
            "No se pudo modificar el stock: revisa la cantidad y el producto."
        }
    }

    override suspend fun actualizarFoto(id: Int, archivo: String): String? {
        require(FotosProducto.nombreSeguro(archivo)) { "Nombre de fotografía no válido." }
        return dao.reemplazarFoto(id, archivo)
    }
}
