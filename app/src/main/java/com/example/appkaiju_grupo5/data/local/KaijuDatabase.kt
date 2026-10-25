package com.example.appkaiju_grupo5.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.appkaiju_grupo5.data.ProductosIniciales

@Database(entities = [ProductoEntity::class], version = 2, exportSchema = true)
abstract class KaijuDatabase : RoomDatabase() {
    abstract fun productoDao(): ProductoDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE productos ADD COLUMN fotoRuta TEXT")
            }
        }

        fun crear(context: Context, nombre: String = "kaiju_inventario.db"): KaijuDatabase =
            Room.databaseBuilder(context.applicationContext, KaijuDatabase::class.java, nombre)
                .addMigrations(MIGRATION_1_2)
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
