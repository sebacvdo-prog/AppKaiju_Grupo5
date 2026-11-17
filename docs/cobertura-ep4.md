# Cobertura EP4 · IE 3.2.1

Medición local del 16/11/2026 en `feature/ep4-backend-integracion`. El indicador es `LINE / COVEREDRATIO` en JaCoCo 0.8.14. En Android se analizan las clases de producción debug y se usan exclusivamente los datos de `testDebugUnitTest`. La cuenta incluye las pruebas locales de componentes con Robolectric. La ejecución instrumentada y el backend se registran por separado.

| Medición | Inicial | Final | Pruebas finales aprobadas |
|---|---:|---:|---:|
| Android JVM | 702/1.119 · 62,73 % | **1.026/1.119 · 91,69 %** | 74 |
| Android instrumentado, API 35 | — | — | 27 |
| Backend Spring Boot | 147/152 · 96,71 % | **147/152 · 96,71 %** | 13 |

Las dos mediciones de Android usaron el mismo alcance y las mismas 1.119 líneas. La línea base se ejecutó con las 66 pruebas JVM que ya existían. Las ocho pruebas adicionales cubren migración y reglas de Room en bases temporales, persistencia de referencias fotográficas, límites de stock, validaciones, archivos y miniaturas de prueba, preferencias de vibración, navegación compacta y navegación expandida. Se conservaron las pruebas de Retrofit, validaciones, errores HTTP, conflictos 409/412, reintentos, mapeo de DTO y cotización. Las 13 pruebas de backend ya cubrían 63 de 65 líneas de `ProductoService` y las 24 ramas medibles de todo el backend.

## Alcance Android

Prefijo de paquetes `com.example.appkaiju_grupo5`. La lista agrupa por archivo las clases, objetos, funciones de nivel superior y clases anidadas que emite el compilador.

| Paquete o grupo | Archivos de producción incluidos | Líneas finales |
|---|---|---:|
| Raíz | `KaijuApplication.kt` | 9/9 |
| `model` | `CotizacionUsdClp.kt`, `Producto.kt`, `ProductoFormulario.kt`, `ProductoRemoto.kt`, `UsuarioErrores.kt`, `UsuarioUiState.kt` | 97/97 |
| `viewmodel` | `CotizacionViewModel.kt`, `FotoProductoViewModel.kt`, `InventarioRemotoViewModel.kt`, `NavigationViewModel.kt`, `OperacionInventarioRemoto.kt`, `ProductosViewModel.kt`, `UsuarioViewModel.kt` | 415/435 |
| `data` | `InventarioRepository.kt`, `ProductosIniciales.kt`, `RoomInventarioRepository.kt` | 28/28 |
| `data.local` | `KaijuDatabase.kt`, `ProductoDao.kt`, `ProductoEntity.kt` | 31/34 |
| `data.remote` | `ClienteInventario.kt`, `InventarioRemotoRepository.kt`, `ProductoApiService.kt`, `ProductoDto.kt`, `ProductoMapper.kt` | 118/125 |
| `data.external` | `ClienteFrankfurter.kt`, `FrankfurterApi.kt`, `TipoCambioDto.kt`, `TipoCambioRepository.kt` | 50/55 |
| `device` | `FotoProductoStorage.kt`, `FotosProducto.kt`, `ProductoFileProvider.kt`, `VibracionInventario.kt` | 52/70 |
| `navigation` | `AppNavigation.kt`, `KaijuNavHost.kt`, `NavigationEvent.kt`, `Screen.kt` | 55/66 |
| `ui` | `KaijuApp.kt` | 124/128 |
| `ui.screens` | `ProductosRoute.kt` | 35/60 |
| `ui.utils` | `WindowSizeUtils.kt` | 12/12 |

La selección conserva las clases de UI que coordinan navegación, cámara y confirmaciones. Algunas clases declarativas, como las interfaces Retrofit, aparecen en el XML sin líneas ejecutables. Las entradas `Emitters.kt` y `SafeCollector.common.kt` corresponden a referencias de línea de Kotlin y no aportan líneas al indicador.

### Exclusiones

- Implementaciones generadas `KaijuDatabase_Impl*` y `ProductoDao_Impl*` y clases sintéticas `ComposableSingletons$*`. El informe mantiene la migración, el contenido inicial, los mapeadores y las funciones del DAO escritas en Kotlin. Los recursos, clases Android generadas fuera de la salida Kotlin y dependencias externas quedan fuera del universo de clases de producción analizado.
- `MainActivity.kt`, que monta Compose y aplica la configuración visual de ventana. `KaijuApplication.kt` se mide porque ensambla las fuentes de datos y sus variantes.
- `ui/theme/{Color,Theme,Type}.kt`, `ui/HomeScreen.kt`, `ui/components/{IndicadorCotizacion,MiniaturaProducto,ProductoEditorDialog}.kt` y `ui/screens/{AdaptiveHomeScreen,EditorRemotoDialog,HomeCompactScreen,HomeExpandedScreen,HomeMediumScreen,InventarioRemotoDialog,PerfilScreen,ProductosScreen,RegistroScreen,ResumenScreen}.kt`. Son tema, presentación, adaptación visual y enlace de controles con estados y funciones que se miden en sus clases de origen.

La apertura exitosa de una URI de `FileProvider` se verifica en Android instrumentado. En Windows, el sandbox JVM prueba el rechazo de rutas externas y el ciclo de crear, validar, generar una miniatura y retirar su propio archivo de prueba. La comparación de datos del AVD se realiza por separado.

## Alcance backend

El informe Maven analiza todas las clases de producción del paquete `cl.duoc.appkaiju.inventario` y sus subpaquetes.

- Raíz, `KaijuInventoryApplication`.
- `producto.controller.ProductoController`, `producto.service.ProductoService`, `producto.entity.Producto` y `producto.entity.AjusteStock`.
- `producto.repository.ProductoRepository` y `producto.repository.AjusteStockRepository`.
- `producto.dto.ProductoDto` y sus registros `Alta`, `Edicion`, `Vista`, `Pagina`, `Ajuste` y `AjusteAplicado`.
- `error.ApiException` y `error.ApiExceptionHandler`.

Los repositorios sin métodos ejecutables y el contenedor de DTOs se enumeran en XML. Las migraciones SQL se verifican en integración y no forman parte del contador de líneas Java. Las pruebas levantan PostgreSQL temporal e independiente de `kaiju_inventory`.

## Reproducir

Android desde la raíz, usando el JDK del daemon Gradle indicado en el proyecto.

~~~powershell
.\gradlew.bat :app:verificarCoberturaLogicaDebug :app:assembleDebug :app:assembleRelease :app:assembleDebugAndroidTest
.\gradlew.bat :app:connectedDebugAndroidTest
~~~

`verificarCoberturaLogicaDebug` genera el HTML en `app/build/reports/jacoco/logicaDebug/html/index.html` y el XML en `app/build/reports/jacoco/logicaDebug/cobertura.xml`, y falla si el porcentaje de líneas baja de 80 %. El reporte de pruebas está en `app/build/reports/tests/testDebugUnitTest/index.html`. Los casos Retrofit usan MockWebServer en loopback o una API simulada. Robolectric usa su SDK de prueba y un sustituto de Frankfurter para las pruebas de navegación. Ejecutar las pruebas instrumentadas en un AVD dedicado porque el runner puede desinstalar la app. La suite verificada se ejecutó sin conectividad en un AVD separado sobre los APK debug recién compilados. Gradle guarda sus resultados en `app/build/outputs/androidTest-results/`.

Backend desde `backend` con JDK 21.

~~~powershell
.\mvnw.cmd -B -ntp test jacoco:report@reporte-cobertura
~~~

El HTML queda en `backend/target/site/jacoco/index.html` y el XML en `backend/target/site/jacoco/jacoco.xml`. Este comando deja el JAR de Spring Boot que pueda estar en uso fuera del ciclo de empaquetado.

En esta corrida permanecieron el aviso de AGP para el `srcDir` existente y el aviso de carga dinámica de agente de Mockito/Byte Buddy en las pruebas Maven. Los dos resultados de cobertura, los APK Android y las tres suites se verificaron por separado. [Documentación de JaCoCo para Gradle](https://docs.gradle.org/current/userguide/jacoco_plugin.html) · [Documentación de JaCoCo para Maven](https://www.jacoco.org/jacoco/trunk/doc/maven.html)
