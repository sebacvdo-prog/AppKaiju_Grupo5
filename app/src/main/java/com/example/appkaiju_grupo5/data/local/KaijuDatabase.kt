package com.example.appkaiju_grupo5.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.appkaiju_grupo5.data.ProductosIniciales

@Database(entities = [ProductoEntity::class], version = 1, exportSchema = true)
abstract class KaijuDatabase : RoomDatabase() {
    abstract fun productoDao(): ProductoDao

    companion object {
        fun crear(context: Context, nombre: String = "kaiju_inventario.db"): KaijuDatabase =
            Room.databaseBuilder(context.applicationContext, KaijuDatabase::class.java, nombre)
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        // onCreate se ejecuta en la transacción de creación, nunca al reabrir.
                        ProductosIniciales.productos.forEach { producto ->
                            db.execSQL(
                                "INSERT INTO productos (id, nombre, categoria, stock, stockMinimo, ubicacion) VALUES (?, ?, ?, ?, ?, ?)",
                                arrayOf<Any>(producto.id, producto.nombre, producto.categoria,
                                    producto.stock, producto.stockMinimo, producto.ubicacion)
                            )
                        }
                    }
                })
                .build()
    }
}
