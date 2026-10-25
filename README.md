# AppKaiju

**Evaluación Parcial 2 · Programación de Aplicaciones Móviles · Duoc UC**

Aplicación Android desarrollada en Kotlin para gestionar un inventario local, consultar existencias y asociar fotografías a los productos.

## Autoría

- **Estudiante:** Sebastián Acevedo.
- **Modalidad:** Desarrollo individual, informado previamente al docente.
- **Asignatura:** Desarrollo de Aplicaciones Móviles (DSY1105).
- **Institución:** Duoc UC.

## Problema y objetivo

La información dispersa sobre productos, cantidades y ubicaciones dificulta el control de una bodega y la identificación de artículos que necesitan reposición. La trazabilidad del inventario requiere mantener esa información disponible y relacionarla con cada producto.

AppKaiju busca centralizar los datos del inventario en el dispositivo, facilitar su actualización y apoyar la identificación visual mediante fotografías persistentes.

## Funcionalidades implementadas

| Área | Funcionalidad |
| --- | --- |
| Productos | Consulta, creación y edición de nombre, categoría, ubicación, stock y stock mínimo. |
| Stock | Controles de incremento y disminución de una unidad; prevención de cantidades negativas y desbordamientos. |
| Reposición | Indicadores y filtro de stock bajo cuando la cantidad es menor o igual al mínimo configurado. |
| Formularios | Registro demostrativo de usuario y editor de productos con validaciones y mensajes por campo. |
| Persistencia | Almacenamiento local con Room y actualización reactiva de la interfaz. |
| Animaciones | Transiciones de la cantidad, el color del indicador de stock y el tamaño de las tarjetas. |
| Vibración | Confirmación háptica después de guardar productos o cambiar stock, según el hardware y la configuración del dispositivo. |
| Cámara | Captura y reemplazo de fotografías de productos guardados, con miniaturas en el editor y las tarjetas. |

## Navegación y formularios

La navegación utiliza una barra inferior y un menú lateral en pantallas compactas y medianas; en pantallas amplias se muestra un menú lateral permanente. La pantalla de inicio adapta su distribución al ancho disponible.

| Pantalla | Ruta | Propósito |
| --- | --- | --- |
| Inicio | `inicio` | Presentación y acceso al inventario. |
| Productos | `productos` | Listado, filtro, edición, stock y fotografías. |
| Perfil | `perfil` | Perfil demostrativo y acceso al registro. |
| Registro | `registro` | Ingreso y validación de datos de usuario. |
| Resumen | `resumen` | Revisión de los datos ingresados y regreso al formulario. |

- **Registro:** nombre y dirección obligatorios, correo con formato válido, clave de al menos seis caracteres y aceptación de términos. El formulario y el resumen comparten un `UsuarioViewModel`; la clave no se muestra en el resumen. Estos datos se mantienen en memoria durante el flujo demostrativo.
- **Productos:** nombre, categoría y ubicación obligatorios; stock y stock mínimo deben ser enteros válidos no negativos. Las operaciones muestran confirmaciones y estados de carga o error.
- **Regreso y estado:** el botón Atrás cierra primero el menú abierto. Los cambios de sección conservan el estado de navegación; el filtro y el borrador del editor utilizan `SavedStateHandle`.

## Arquitectura MVVM

La aplicación separa la presentación, el estado y el acceso a datos:

| Capa | Responsabilidad |
| --- | --- |
| Vista (`ui`, `navigation`) | Pantallas y componentes Compose, navegación y presentación del estado. |
| ViewModel (`viewmodel`) | Validaciones, estado observable con `StateFlow` y eventos de usuario. Incluye `ProductosViewModel`, `FotoProductoViewModel`, `UsuarioViewModel` y `NavigationViewModel`. |
| Modelo y datos (`model`, `data`) | Modelos, contrato `InventarioRepository`, implementación `RoomInventarioRepository`, entidades y DAO. |
| Dispositivo (`device`) | Almacenamiento de fotografías, FileProvider y vibración. |

El inventario sigue el flujo **Vista → ViewModel → Repositorio → DAO → Room**. Los cambios de la base de datos se observan con `Flow` y actualizan la interfaz.

## Persistencia Room y fotografías

- La base local `kaiju_inventario.db` se administra con Room y tiene **esquema versión 2**. Los productos iniciales se insertan solo al crear la base; las altas y modificaciones posteriores se conservan al reabrir la aplicación.
- La migración **1 → 2** agrega la columna opcional `fotoRuta` sin eliminar productos ni stocks. Los esquemas de ambas versiones se incluyen en `app/schemas/`.
- Las imágenes JPEG se guardan en el directorio privado `filesDir/fotos_productos`; Room conserva su nombre relativo. La cámara externa recibe una URI mediante `FileProvider` y el contrato `TakePicture`.
- Las miniaturas se cargan fuera del hilo principal y consideran la orientación EXIF. Cancelar una captura conserva la fotografía anterior; editar los datos del producto también mantiene su referencia.

## Tecnologías y librerías

Versiones declaradas en la configuración Gradle del proyecto:

| Tecnología | Versión o configuración | Uso |
| --- | --- | --- |
| Kotlin y compilador Compose | Plugin Compose `2.2.10` | Desarrollo de la aplicación e interfaz declarativa. |
| Jetpack Compose | BOM `2026.02.01` | Material 3, componentes visuales y animaciones. |
| Lifecycle | `2.9.4` | ViewModel y observación del estado según el ciclo de vida. |
| Navigation Compose | `2.9.8` | Rutas y navegación entre pantallas. |
| Room | `2.8.5` | Persistencia local, DAO y migración de esquema. |
| KSP | `2.3.12` | Generación de código para Room. |
| ExifInterface | `1.4.2` | Lectura de la orientación de las fotografías. |
| Pruebas | JUnit `4.13.2`, Espresso `3.5.1`, Compose UI Test y Coroutines Test `1.10.2` | Validación unitaria e instrumentada. |

## Requisitos y ejecución en Android Studio

- Android Studio compatible con **Android Gradle Plugin 9.4.1** y **Gradle 9.6.0**, incluido mediante el wrapper.
- **JDK 25** disponible para el daemon de Gradle, según la configuración del repositorio.
- **Android SDK Platform API 37**, utilizado por `compileSdk` y `targetSdk`.
- Emulador o dispositivo con **API 24 o superior** (`minSdk`). La validación instrumentada se realizó en **Medium Phone API 35**.
- Conexión a Internet para descargar las herramientas y dependencias durante la primera sincronización.

1. Clonar o descargar el [repositorio AppKaiju_Grupo5](https://github.com/sebacvdo-prog/AppKaiju_Grupo5).
2. En Android Studio, seleccionar **Open** y abrir la carpeta raíz que contiene `settings.gradle.kts`.
3. Instalar el SDK requerido desde **SDK Manager** y sincronizar el proyecto con Gradle, utilizando el wrapper y la JVM configurada.
4. En **Device Manager**, crear o iniciar un emulador; se puede utilizar **Medium Phone API 35** con cámara virtual o webcam configurada.
5. Seleccionar el módulo **app**, elegir el emulador y ejecutar con **Run**.
6. Para comprobar la cámara, abrir **Productos → Editar → Tomar foto** y confirmar la captura. Un producto nuevo debe guardarse antes de agregar su fotografía. Cerrar y reabrir AppKaiju permite comprobar su persistencia.

## Pruebas realizadas

Resultados confirmados de la versión validada el **24 de octubre de 2026**, integrada mediante los PR #3 y #4:

| Comprobación | Resultado |
| --- | --- |
| `assembleDebug` | Correcto. |
| `assembleDebugAndroidTest` | Correcto. |
| `testDebugUnitTest` | **44 pruebas aprobadas**, sin fallos ni errores. |
| `connectedDebugAndroidTest` | **25 pruebas aprobadas en API 35**, sin fallos ni omisiones; código de salida **0**. |
| `lintDebug` | **0 errores y 22 advertencias**. Las advertencias permanecen pendientes. |

Las pruebas cubren validaciones, ViewModels, navegación, formularios, persistencia, actualizaciones de stock, migración y fotografías. La migración se verificó con **siete productos y sus stocks**. Las pruebas automatizadas de interfaz simulan la cámara externa y utilizan el contrato, la URI y Room reales.

Además, se validó manualmente la captura de una fotografía para **Casco de seguridad** en Medium Phone API 35: apareció en la tarjeta, permaneció después de cerrar y reabrir la aplicación y se conservaron los siete productos.

Para reproducir las comprobaciones desde la raíz del proyecto en PowerShell:

```powershell
.\gradlew.bat :app:assembleDebug :app:assembleDebugAndroidTest :app:testDebugUnitTest :app:lintDebug
.\gradlew.bat :app:connectedDebugAndroidTest
```

Para las pruebas instrumentadas, utilizar un emulador dedicado a pruebas: el runner puede desinstalar la aplicación al finalizar. Los reportes se generan bajo `app/build/reports/` y los resultados instrumentados en `app/build/outputs/androidTest-results/`.

## Evidencias de desarrollo

- [Repositorio AppKaiju_Grupo5](https://github.com/sebacvdo-prog/AppKaiju_Grupo5).
- [PR #3: persistencia Room, animaciones y vibración](https://github.com/sebacvdo-prog/AppKaiju_Grupo5/pull/3).
- [PR #4: cámara y fotografías persistentes](https://github.com/sebacvdo-prog/AppKaiju_Grupo5/pull/4).
