# AppKaiju

**Evaluación Parcial 4 · Desarrollo de Aplicaciones Móviles (DSY1105) · Duoc UC**

Aplicación Android desarrollada en Kotlin para gestionar productos, existencias y fotografías. La EP4 incorpora un inventario remoto con **Kaiju Inventory API** y una cotización referencial USD/CLP de **Frankfurter**.

## Autoría

- **Estudiante:** Sebastián Acevedo.
- **Modalidad:** Desarrollo individual autorizado por el docente.
- **Asignatura:** Desarrollo de Aplicaciones Móviles (DSY1105).
- **Institución:** Duoc UC.

## Problema y objetivo

La información dispersa sobre productos, cantidades y ubicaciones dificulta el control de una bodega y la identificación de artículos que necesitan reposición. La trazabilidad del inventario requiere mantener esa información disponible y relacionarla con cada producto.

AppKaiju permite mantener un inventario en el dispositivo, consultar y actualizar un inventario separado en el servidor y asociar fotografías a los productos locales.

## Funcionalidades implementadas

| Área | Funcionalidad |
| --- | --- |
| Productos | Consulta, creación y edición de nombre, categoría, ubicación, stock y stock mínimo. |
| Stock | Controles de incremento y disminución de una unidad que previenen cantidades negativas y desbordamientos. |
| Reposición | Indicadores y filtro de stock bajo cuando la cantidad es menor o igual al mínimo configurado. |
| Formularios | Registro demostrativo de usuario y editor de productos con validaciones y mensajes por campo. |
| Persistencia | Almacenamiento local con Room y actualización reactiva de la interfaz. |
| Animaciones | Transiciones de la cantidad, el color del indicador de stock y el tamaño de las tarjetas. |
| Vibración | Confirmación háptica después de guardar productos o cambiar stock, según el hardware y la configuración del dispositivo. |
| Cámara | Captura y reemplazo de fotografías de productos guardados, con miniaturas en el editor y las tarjetas. |
| Inventario del servidor | Desde **Productos → Consultar servidor**, consulta paginada, alta, edición, ajuste de existencias y archivado. |
| Cotización USD/CLP | Consulta en Inicio a Frankfurter, con fecha de referencia, actualización manual y reintento. |

## Navegación y formularios

La navegación utiliza una barra inferior y un menú lateral en pantallas compactas y medianas. En pantallas amplias se muestra un menú lateral permanente. La pantalla de inicio adapta su distribución al ancho disponible.

| Pantalla | Ruta | Propósito |
| --- | --- | --- |
| Inicio | `inicio` | Presentación, acceso al inventario y cotización USD/CLP. |
| Productos | `productos` | Inventario local, filtro, edición, stock, fotografías y acceso al inventario del servidor. |
| Perfil | `perfil` | Perfil demostrativo y acceso al registro. |
| Registro | `registro` | Ingreso y validación de datos de usuario. |
| Resumen | `resumen` | Revisión de los datos ingresados y regreso al formulario. |

- **Registro.** El formulario exige nombre, dirección, un correo válido, una clave de al menos seis caracteres y aceptación de términos. Un `UsuarioViewModel` mantiene en memoria los datos del flujo demostrativo y presenta un resumen sin mostrar la clave.
- **Productos.** Nombre, categoría y ubicación son obligatorios. Stock y stock mínimo deben ser enteros válidos no negativos. Las operaciones muestran confirmaciones y estados de carga o error.
- **Regreso y estado.** El botón Atrás cierra primero el menú abierto. Los cambios de sección conservan el estado de navegación. El filtro y el borrador del editor utilizan `SavedStateHandle`.

## Arquitectura MVVM

La aplicación separa la presentación, el estado y el acceso a datos.

| Capa | Responsabilidad |
| --- | --- |
| Vista (`ui`, `navigation`) | Pantallas y componentes Compose, navegación y presentación del estado. |
| ViewModel (`viewmodel`) | Validaciones, estado observable con `StateFlow` y eventos de usuario. `ProductosViewModel` y `FotoProductoViewModel` coordinan el inventario local. `InventarioRemotoViewModel` y `CotizacionViewModel` gestionan las solicitudes de red. |
| Modelo y datos locales (`model`, `data`, `data.local`) | Modelos, `InventarioRepository`, `RoomInventarioRepository`, entidades, DAO y base Room. |
| Datos remotos (`data.remote`, `data.external`) | Clientes y repositorios Retrofit independientes para Kaiju Inventory API y Frankfurter. Los mapeadores convierten los DTO y los repositorios traducen errores para la interfaz. |
| Dispositivo (`device`) | Almacenamiento de fotografías, FileProvider y vibración. |

- El inventario local sigue el flujo Compose → `ProductosViewModel` → `RoomInventarioRepository` → DAO → Room. La interfaz observa los cambios mediante `Flow`.
- El inventario del servidor sigue el flujo Compose → `InventarioRemotoViewModel` → `InventarioRemotoRepository` → Retrofit → Spring Boot → PostgreSQL. El inventario local y el remoto se administran por separado, sin sincronización automática de registros, fotos ni stocks.
- La cotización sigue el flujo Compose → `CotizacionViewModel` → `TipoCambioRepository` → Retrofit → Frankfurter. La solicitud es independiente de los datos de inventario.

## Persistencia Room y fotografías

- La base local `kaiju_inventario.db` se administra con Room y tiene **esquema versión 2**. Los productos iniciales se insertan solo al crear la base. Las altas y modificaciones posteriores se conservan al reabrir la aplicación.
- La migración **1 → 2** agrega la columna opcional `fotoRuta` sin eliminar productos ni stocks. Los esquemas de ambas versiones se incluyen en `app/schemas/`.
- Las imágenes JPEG se guardan en el directorio privado `filesDir/fotos_productos` y Room conserva su nombre relativo. La cámara externa recibe una URI mediante `FileProvider` y el contrato `TakePicture`.
- Las miniaturas se cargan fuera del hilo principal y consideran la orientación EXIF. Tanto cancelar una captura como editar los datos del producto conservan la referencia a la fotografía anterior.

## Tecnologías y librerías

Versiones y herramientas de Android y del backend.

| Tecnología | Versión o configuración | Uso |
| --- | --- | --- |
| Kotlin y compilador Compose | Plugin Compose `2.2.10` | Desarrollo de la aplicación e interfaz declarativa. |
| Jetpack Compose | BOM `2026.02.01` | Material 3, componentes visuales y animaciones. |
| Lifecycle | `2.9.4` | ViewModel y observación del estado según el ciclo de vida. |
| Navigation Compose | `2.9.8` | Rutas y navegación entre pantallas. |
| Room | `2.8.5` | Persistencia local, DAO y migración de esquema. |
| KSP | `2.3.12` | Generación de código para Room. |
| ExifInterface | `1.4.2` | Lectura de la orientación de las fotografías. |
| Retrofit y OkHttp | `3.0.0` y `4.12.0` | Clientes HTTP y serialización JSON con Gson. |
| Backend | Java 21, Spring Boot `3.5.16`, Spring MVC, Spring Data JPA, Bean Validation y Maven Wrapper | API REST, validación, lógica de stock y acceso a datos. |
| Base de datos del servidor | PostgreSQL `17.12` en el entorno verificado y Flyway | Persistencia y migración versionada del esquema. |
| Pruebas | JUnit `4.13.2`, Espresso `3.5.1`, Compose UI Test y Coroutines Test `1.10.2` | Validación unitaria e instrumentada. |
| Cobertura | JaCoCo `0.8.14` | Informes HTML y XML para Android JVM y backend. |

## Kaiju Inventory API

El microservicio se encuentra en `backend/`. El controlador recibe y valida DTO, el servicio aplica las reglas de inventario y los repositorios JPA persisten productos y recibos de ajustes en PostgreSQL. [Contrato y configuración del backend](backend/README.md)

| Método | Endpoint | Resultado |
| --- | --- | --- |
| `GET` | `/api/v1/productos` | Listado paginado de productos activos. `incluirArchivados=true` incluye archivados. |
| `GET` | `/api/v1/productos/{uuid}` | Detalle de un producto activo con `ETag`. |
| `POST` | `/api/v1/productos` | Alta con UUID suministrado por el cliente. |
| `PUT` | `/api/v1/productos/{uuid}` | Actualización condicionada por `If-Match`. |
| `DELETE` | `/api/v1/productos/{uuid}` | Archivado lógico condicionado por `If-Match`. |
| `POST` | `/api/v1/productos/{uuid}/ajustes-stock` | Ajuste con `operacionId` y `delta`. |

La edición y el archivado emplean control de versión con `ETag` e `If-Match`. Un reintento con el mismo UUID de alta o el mismo identificador de ajuste conserva el resultado de la operación original. Android muestra confirmaciones, errores y opciones de reintento según la respuesta del servidor.

## API externa Frankfurter

La pantalla Inicio consulta `GET https://api.frankfurter.dev/v2/rate/USD/CLP` mediante un cliente Retrofit independiente. Muestra la cotización referencial, su fecha y un control para actualizar o reintentar. Requiere Internet y funciona sin clave de API. [Documentación de Frankfurter](https://frankfurter.dev/)

## Requisitos y ejecución en Android Studio

- Android Studio compatible con **Android Gradle Plugin 9.4.1** y **Gradle 9.6.0**, incluido mediante el wrapper.
- **JDK 25** disponible para el daemon de Gradle, según la configuración del repositorio.
- **Android SDK Platform API 37**, utilizado por `compileSdk` y `targetSdk`.
- Emulador o dispositivo con **API 24 o superior** (`minSdk`). La validación instrumentada y la demostración firmada se realizaron en **API 35 (Android 15)**.
- Conexión a Internet para descargar las herramientas y dependencias durante la primera sincronización.

1. Abrir una copia del repositorio que contenga los cambios de EP4. La rama de trabajo es `feature/ep4-backend-integracion`. [Repositorio AppKaiju_Grupo5](https://github.com/sebacvdo-prog/AppKaiju_Grupo5)
2. En Android Studio, seleccionar **Open** y abrir la carpeta raíz que contiene `settings.gradle.kts`.
3. Instalar el SDK requerido desde **SDK Manager** y sincronizar el proyecto con Gradle, utilizando el wrapper y la JVM configurada.
4. En **Device Manager**, crear o iniciar un emulador. Se puede utilizar **Medium Phone API 35** con cámara virtual o webcam configurada.
5. Seleccionar el módulo **app**, elegir el emulador y ejecutar con **Run**.
6. Para comprobar la cámara, abrir **Productos → Editar → Tomar foto** y confirmar la captura. Un producto nuevo debe guardarse antes de agregar su fotografía. Cerrar y reabrir AppKaiju permite comprobar su persistencia.

También se puede iniciar un AVD y desplegar Android debug desde PowerShell. Seleccionar un AVD dedicado a pruebas.

```powershell
$sdk = Join-Path $env:LOCALAPPDATA 'Android\Sdk'
& "$sdk\emulator\emulator.exe" -list-avds
& "$sdk\emulator\emulator.exe" -avd 'NOMBRE_DEL_AVD'
```

Una vez iniciado, ejecutar desde otra terminal en la raíz del repositorio.

```powershell
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat :app:installDebug
```

Debug consulta el backend de Windows desde el emulador en `http://10.0.2.2:8080/`. La configuración de red habilita HTTP solo para esa dirección en debug.

## Ejecutar Spring Boot y PostgreSQL

1. Usar PostgreSQL en Windows y comprobar que el servicio escuche en loopback en el puerto `5432`. La instalación verificada usó PostgreSQL `17.12`. [Instalador de PostgreSQL](https://www.postgresql.org/download/windows/)
2. Crear la base `kaiju_inventory` y el rol `kaiju` si se prepara un entorno de desarrollo nuevo. Los comandos y el procedimiento de contraseña están en la [guía del backend](backend/README.md). [Variables de ejemplo](backend/.env.example)
3. En una sesión de PowerShell con Java 21 o superior, establecer las variables de conexión y levantar el servicio desde la raíz del repositorio.

```powershell
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
$env:KAIJU_DB_URL = 'jdbc:postgresql://127.0.0.1:5432/kaiju_inventory'
$env:KAIJU_DB_USER = 'kaiju'
$clave = Read-Host 'Contraseña local de PostgreSQL' -AsSecureString
try {
    $env:KAIJU_DB_PASSWORD = [pscredential]::new($env:KAIJU_DB_USER, $clave).GetNetworkCredential().Password
    .\backend\mvnw.cmd -f backend/pom.xml spring-boot:run
} finally {
    Remove-Item Env:KAIJU_DB_PASSWORD -ErrorAction SilentlyContinue
}
```

Con el servidor activo en `127.0.0.1:8080`, ejecutar `Invoke-RestMethod 'http://127.0.0.1:8080/api/v1/productos'` en otra terminal. Flyway aplica la migración V1 al esquema vacío y Hibernate valida el esquema al iniciar.

## Demostración HTTPS y APK firmado

La evaluación usa la variante `demoRelease` con identificador `com.example.appkaiju_grupo5.demo` y versión `1.0` (`versionCode` `1`). En Android se conecta a `https://localhost:8443/` mediante un reenvío `adb reverse` limitado a loopback. La instancia de Spring Boot de demostración usa `kaiju_inventory_demo` y el rol `kaiju_demo`, separados del entorno de debug.

En Windows, preparar la base y TLS, compilar el backend de demostración en su directorio separado e iniciarlo con `Iniciar-Demo.ps1`. `Firmar-ApkDemo.ps1` comprueba HTTPS, usa el JKS externo y solicita las contraseñas en la terminal. El archivo resultante es `app/build/outputs/apk/demoRelease/app-demoRelease.apk`. Para instalarlo, iniciar el AVD Android 15, aplicar el reenvío con `Conectar-UsbDemo.ps1`, ejecutar `adb install` sobre ese AVD y abrir **AppKaiju Demo**. [Guía paso a paso para el emulador y la demo](docs/demo-usb-ep4.md) · [Firma y huellas SHA-256](docs/firma-release-ep4.md)

## Pruebas realizadas

Medición de EP4 del **16 de noviembre de 2026**.

| Comprobación | Resultado |
| --- | --- |
| Android JVM (`testDebugUnitTest`) | **74 pruebas aprobadas**. Cobertura de líneas de la lógica **91,69 %** (1.026/1.119), frente al 62,73 % inicial. |
| Android instrumentado (API 35) | **27 pruebas aprobadas**. Suite independiente del porcentaje de cobertura JVM. |
| Backend Spring Boot | **13 pruebas de integración aprobadas**. Cobertura de líneas **96,71 %** (147/152). |
| Compilación y APK | `debug`, `release` y `demoRelease` compilaron. El APK `demoRelease` firmado pasó la verificación v2, y el SHA-256 del APK instalado coincidió con el original. |

En total se registraron **114 pruebas automatizadas aprobadas**. La prueba del APK firmado en un AVD separado confirmó navegación, CRUD por HTTPS, persistencia del stock y archivado tras reiniciar Spring Boot, recuperación de Frankfurter al reintentar y una fotografía de la cámara virtual que persistió al reabrir AppKaiju. [Alcance y reproducción de JaCoCo](docs/cobertura-ep4.md) · [Evidencias de la EP4](docs/entrega-ep4.md#evidencias-disponibles)

Para reproducir las comprobaciones desde la raíz del proyecto en PowerShell:

```powershell
.\gradlew.bat :app:verificarCoberturaLogicaDebug :app:assembleDebug :app:assembleRelease :app:assembleDebugAndroidTest
.\gradlew.bat :app:connectedDebugAndroidTest
.\backend\mvnw.cmd -f backend/pom.xml -B -ntp test jacoco:report@reporte-cobertura
```

Para las pruebas instrumentadas, utilizar un emulador dedicado a pruebas: el runner puede desinstalar la aplicación al finalizar. Los reportes se generan bajo `app/build/reports/` y los resultados instrumentados en `app/build/outputs/androidTest-results/`.

## Documentación y entrega

- [Checklist EP4, evidencias de GitHub y Trello y archivos de entrega](docs/entrega-ep4.md).
- [Configuración, endpoints y ejecución de Kaiju Inventory API](backend/README.md).
- [Guía de demostración HTTPS en Android 15](docs/demo-usb-ep4.md).
- [Generación, verificación y entrega privada del JKS](docs/firma-release-ep4.md).
- [Cobertura y alcance de las pruebas](docs/cobertura-ep4.md).
- Antecedentes de EP2. [PR #3 de persistencia Room, animaciones y vibración](https://github.com/sebacvdo-prog/AppKaiju_Grupo5/pull/3) · [PR #4 de cámara y fotografías persistentes](https://github.com/sebacvdo-prog/AppKaiju_Grupo5/pull/4)
