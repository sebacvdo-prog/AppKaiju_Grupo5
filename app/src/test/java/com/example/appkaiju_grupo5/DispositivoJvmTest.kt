package com.example.appkaiju_grupo5

import android.os.VibratorManager
import android.provider.Settings
import androidx.exifinterface.media.ExifInterface
import com.example.appkaiju_grupo5.device.FotosProducto
import com.example.appkaiju_grupo5.device.VibracionInventario
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowBitmapFactory
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = KaijuApplication::class)
@GraphicsMode(GraphicsMode.Mode.LEGACY)
@Suppress("DEPRECATION")
class DispositivoJvmTest {
    @Test
    fun archivoTemporalValidaElContenidoYRechazaRutasExternas() = runBlocking {
        val app = RuntimeEnvironment.getApplication() as KaijuApplication
        val fotos = app.fotosProducto
        val nombre = fotos.crearTemporal()
        val archivo = File(app.filesDir, "fotos_productos/$nombre")
        assertTrue(FotosProducto.nombreSeguro(nombre))
        assertTrue(archivo.isFile)
        assertFalse(fotos.esValida(nombre))
        assertTrue(runCatching { fotos.uriPara("../$nombre") }.exceptionOrNull() is IllegalArgumentException)

        assertTrue(ImageIO.write(BufferedImage(1_280, 720, BufferedImage.TYPE_INT_RGB), "jpg", archivo))
        ExifInterface(archivo).apply {
            setAttribute(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_ROTATE_90.toString())
            saveAttributes()
        }
        ShadowBitmapFactory.provideWidthAndHeightHints(archivo.path, 1_280, 720)
        assertTrue(fotos.esValida(nombre))
        assertNotNull(fotos.miniatura(nombre, maximo = 160))
        assertNull(fotos.miniatura("../$nombre"))

        fotos.eliminar(nombre)
        assertFalse(archivo.exists())
        assertFalse(fotos.esValida(nombre))
        fotos.eliminar(nombre)
    }

    @Test
    fun vibracionRespetaPreferenciaDelUsuarioYDisponibilidadDeHardware() {
        val app = RuntimeEnvironment.getApplication()
        val manager = checkNotNull(app.getSystemService(VibratorManager::class.java))
        val shadow = shadowOf(manager.defaultVibrator)
        val vibracion = VibracionInventario(app)

        Settings.System.putInt(app.contentResolver, Settings.System.HAPTIC_FEEDBACK_ENABLED, 0)
        shadow.setHasVibrator(true)
        assertFalse(vibracion.confirmar())
        assertFalse(shadow.isVibrating)

        Settings.System.putInt(app.contentResolver, Settings.System.HAPTIC_FEEDBACK_ENABLED, 1)
        shadow.setHasVibrator(false)
        assertFalse(vibracion.confirmar())
        shadow.setHasVibrator(true)
        assertTrue(vibracion.confirmar())
        assertTrue(shadow.isVibrating)
    }
}
