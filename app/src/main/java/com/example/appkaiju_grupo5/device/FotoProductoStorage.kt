package com.example.appkaiju_grupo5.device

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.IOException
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class FotoProductoStorage(context: Context) : FotosProducto {
    private val appContext = context.applicationContext
    private val directorio = File(appContext.filesDir, "fotos_productos")

    private fun archivo(nombre: String): File {
        require(FotosProducto.nombreSeguro(nombre))
        val file = File(directorio, nombre).canonicalFile
        require(file.parentFile == directorio.canonicalFile)
        return file
    }

    override suspend fun crearTemporal(): String = withContext(Dispatchers.IO) {
        if (!directorio.exists() && !directorio.mkdirs()) throw IOException("No se pudo crear la carpeta de fotos.")
        val nombre = "producto_${UUID.randomUUID()}.jpg"
        check(archivo(nombre).createNewFile())
        nombre
    }

    fun uriPara(nombre: String): Uri = FileProvider.getUriForFile(
        appContext, "${appContext.packageName}.fileprovider", archivo(nombre)
    )

    override suspend fun esValida(archivo: String): Boolean = withContext(Dispatchers.IO) {
        val file = archivo(archivo)
        if (!file.isFile || file.length() == 0L) return@withContext false
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.path, bounds)
        bounds.outWidth > 0 && bounds.outHeight > 0
    }

    override suspend fun eliminar(archivo: String) = withContext(Dispatchers.IO) {
        val file = archivo(archivo)
        if (file.exists() && !file.delete()) throw IOException("No se pudo eliminar la foto temporal.")
    }

    suspend fun miniatura(nombre: String, maximo: Int = 320): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val file = archivo(nombre)
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.path, bounds)
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@withContext null
            var sample = 1
            while (bounds.outWidth / sample > maximo * 2 || bounds.outHeight / sample > maximo * 2) sample *= 2
            val bitmap = BitmapFactory.decodeFile(file.path, BitmapFactory.Options().apply { inSampleSize = sample })
                ?: return@withContext null
            val orientation = try { ExifInterface(file.path).getAttributeInt(ExifInterface.TAG_ORIENTATION, 1) }
                catch (_: IOException) { 1 }
            val matrix = Matrix().apply {
                when (orientation) {
                    2 -> setScale(-1f, 1f)
                    3 -> setRotate(180f)
                    4 -> setScale(1f, -1f)
                    5 -> { setRotate(90f); postScale(-1f, 1f) }
                    6 -> setRotate(90f)
                    7 -> { setRotate(270f); postScale(-1f, 1f) }
                    8 -> setRotate(270f)
                }
            }
            if (matrix.isIdentity) bitmap else Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                .also { if (it !== bitmap) bitmap.recycle() }
        } catch (_: IOException) { null }
          catch (_: IllegalArgumentException) { null }
    }
}
