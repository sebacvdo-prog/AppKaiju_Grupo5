package com.example.appkaiju_grupo5

import android.graphics.Bitmap
import androidx.exifinterface.media.ExifInterface
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.appkaiju_grupo5.device.FotoProductoStorage
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FotoStorageTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val fotos = FotoProductoStorage(context)

    @Test fun privateFileProviderImageCanBeReadValidatedAndSampledAfterStorageRecreation() = runBlocking {
        val nombre = fotos.crearTemporal()
        try {
            assertFalse(fotos.esValida(nombre))
            val uri = fotos.uriPara(nombre)
            assertEquals("content", uri.scheme)
            assertEquals("${context.packageName}.fileprovider", uri.authority)
            val bitmap = Bitmap.createBitmap(2400, 1600, Bitmap.Config.ARGB_8888)
            context.contentResolver.openOutputStream(uri)!!.use { assertTrue(bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it)) }
            bitmap.recycle()
            ExifInterface(java.io.File(context.filesDir, "fotos_productos/$nombre")).apply {
                setAttribute(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_ROTATE_90.toString())
                saveAttributes()
            }
            val recreado = FotoProductoStorage(context)
            assertTrue(recreado.esValida(nombre))
            val miniatura = recreado.miniatura(nombre)!!
            assertTrue(miniatura.width <= 640 && miniatura.height <= 640)
            assertTrue(miniatura.width < miniatura.height)
            miniatura.recycle()
            assertTrue(context.contentResolver.openInputStream(uri)!!.use { it.read() >= 0 })
        } finally { fotos.eliminar(nombre) }
        assertNull(fotos.miniatura(nombre))
    }

    @Test fun invalidReferencesCannotAccessFilesOutsidePhotoDirectory() {
        listOf("../kaiju_inventario.db", "producto_../../a.jpg", "/producto_a.jpg").forEach {
            try { fotos.uriPara(it); fail("Debía rechazar la ruta") } catch (_: IllegalArgumentException) { }
        }
    }
}
