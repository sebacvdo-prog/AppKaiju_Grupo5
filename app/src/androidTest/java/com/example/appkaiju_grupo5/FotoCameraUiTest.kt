package com.example.appkaiju_grupo5

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.core.app.ActivityOptionsCompat
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.appkaiju_grupo5.data.RoomInventarioRepository
import com.example.appkaiju_grupo5.data.local.KaijuDatabase
import com.example.appkaiju_grupo5.device.FotoProductoStorage
import com.example.appkaiju_grupo5.ui.screens.ProductosRoute
import com.example.appkaiju_grupo5.ui.theme.AppKaiju_Grupo5Theme
import com.example.appkaiju_grupo5.viewmodel.FotoProductoViewModel
import com.example.appkaiju_grupo5.viewmodel.ProductosViewModel
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Simula únicamente la cámara externa; contrato TakePicture, URI, pantalla y Room son reales. */
@RunWith(AndroidJUnit4::class)
class FotoCameraUiTest {
    @get:Rule val compose = createComposeRule()
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val nombre = "ep2-camera-ui-${UUID.randomUUID()}.db"
    private val fotos = FotoProductoStorage(context)
    private lateinit var db: KaijuDatabase
    private lateinit var repository: RoomInventarioRepository
    private val creadas = mutableListOf<String>()
    private val registry = object : ActivityResultRegistry() {
        var codigo = 0
        var salida: Uri? = null
        override fun <I, O> onLaunch(requestCode: Int, contract: ActivityResultContract<I, O>, input: I, options: ActivityOptionsCompat?) {
            val intent = contract.createIntent(context, input)
            assertEquals(MediaStore.ACTION_IMAGE_CAPTURE, intent.action)
            @Suppress("DEPRECATION")
            val uri = intent.getParcelableExtra<Uri>(MediaStore.EXTRA_OUTPUT)!!
            assertEquals("content", uri.scheme)
            codigo = requestCode
            salida = uri
            creadas.add(uri.lastPathSegment!!)
        }
    }

    @Before fun abrir() {
        db = KaijuDatabase.crear(context, nombre)
        repository = RoomInventarioRepository(db.productoDao())
        runBlocking { repository.observarProductos().first() }
        val owner = object : ActivityResultRegistryOwner { override val activityResultRegistry = registry }
        compose.setContent {
            CompositionLocalProvider(LocalActivityResultRegistryOwner provides owner) {
                AppKaiju_Grupo5Theme {
                    val vm: ProductosViewModel = viewModel(factory = viewModelFactory {
                        initializer { ProductosViewModel(SavedStateHandle(), repository) }
                    })
                    val fotoVm: FotoProductoViewModel = viewModel(factory = viewModelFactory {
                        initializer { FotoProductoViewModel(SavedStateHandle(), repository, fotos) }
                    })
                    ProductosRoute(vm, fotoVm, fotos)
                }
            }
        }
    }

    @After fun cerrar() {
        compose.waitForIdle()
        db.close()
        context.deleteDatabase(nombre)
        runBlocking { creadas.forEach { fotos.eliminar(it) } }
    }

    @Test fun confirmedCaptureRendersThumbnailAndPersistsOnlySelectedProductPhoto() {
        val anteriores = runBlocking { repository.observarProductos().first() }
        tomarFoto()
        val bitmap = Bitmap.createBitmap(80, 60, Bitmap.Config.ARGB_8888)
        context.contentResolver.openOutputStream(registry.salida!!)!!.use { bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it) }
        bitmap.recycle()
        compose.runOnIdle { registry.dispatchResult(registry.codigo, Activity.RESULT_OK, Intent()) }
        compose.waitUntil(5_000) { runBlocking { repository.observarProductos().first().first().fotoRuta != null } }
        compose.waitUntil(5_000) { compose.onAllNodesWithTag("foto_producto").fetchSemanticsNodes().isNotEmpty() }
        compose.onNode(hasTestTag("foto_producto") and hasAnyAncestor(hasTestTag("producto_editor")))
            .performScrollTo().assertIsDisplayed()
        val actual = runBlocking { repository.observarProductos().first() }
        assertEquals(anteriores.map { if (it.id == 1) it.copy(fotoRuta = creadas.single()) else it }, actual)
        val reopened = KaijuDatabase.crear(context, nombre)
        try { assertEquals(actual, runBlocking { RoomInventarioRepository(reopened.productoDao()).observarProductos().first() }) }
        finally { reopened.close() }
    }

    @Test fun cancelledCaptureKeepsPreviousReferenceAndDeletesEmptyFile() {
        val old = runBlocking { fotos.crearTemporal() }
        creadas.add(old)
        runBlocking { repository.actualizarFoto(1, old) }
        tomarFoto()
        val nueva = registry.salida!!
        compose.runOnIdle { registry.dispatchResult(registry.codigo, Activity.RESULT_CANCELED, null) }
        compose.waitUntil(5_000) { !java.io.File(context.filesDir, "fotos_productos/${nueva.lastPathSegment}").exists() }
        assertEquals(old, runBlocking { repository.observarProductos().first().first().fotoRuta })
    }

    private fun tomarFoto() {
        compose.onNodeWithTag("screen_products").performScrollToNode(hasTestTag("producto_editar_1"))
        compose.onNodeWithTag("producto_editar_1").performClick()
        compose.onNodeWithTag("producto_tomar_foto").performScrollTo().performClick()
        compose.waitUntil(5_000) { registry.salida != null }
    }
}
