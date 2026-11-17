# Checklist y evidencias · AppKaiju EP4

**Sebastián Acevedo · DSY1105 · Duoc UC.** Desarrollo individual autorizado por el docente. Los resultados corresponden a verificaciones locales de `feature/ep4-backend-integracion`. La demostración del APK firmado se realizó en el AVD `AppKaiju_DemoRelease`, Android 15 (API 35).

## Checklist de requisitos EP4

- [x] Aplicación Android con Kotlin, Compose, arquitectura MVVM, validaciones, inventario Room y cámara. La migración 1 → 2 conserva el inventario local.
- [x] Microservicio Spring Boot con PostgreSQL, Flyway V1, endpoints REST de productos, ajustes de stock e inventario remoto mediante Retrofit. [API, configuración y ejecución](../backend/README.md)
- [x] Escenario de evaluación HTTPS por `adb reverse` y base `kaiju_inventory_demo` independiente. Alta, consulta, edición, ajuste y archivado verificados desde el APK firmado. [Guion de la demo](demo-usb-ep4.md#guion-breve-de-evaluación)
- [x] IE 3.1.4. Consumo de Frankfurter desde Android para USD/CLP, con fecha de referencia y reintento. [Evidencias disponibles](#evidencias-disponibles)
- [x] IE 3.2.1. Cobertura Android **91,69 %** (1.026/1.119 líneas), sobre el mínimo de 80 %. Backend **96,71 %** (147/152 líneas). Pasaron 74 pruebas Android JVM, 27 instrumentadas en API 35 y 13 del backend. [Medición y alcance](cobertura-ep4.md)
- [x] IE 3.3.1. APK `demoRelease` firmado, verificado por `apksigner` con esquema v2, instalado y probado en el emulador con el mismo SHA-256. [Firma, ubicación y huellas](firma-release-ep4.md)
- [x] Documentación técnica, instrucciones de firma e instalación y guion de evaluación revisados. [README principal](../README.md)
- [ ] Incorporar la evidencia real de planificación en Trello y el enlace de la versión EP4 en GitHub cuando se autorice. Vincular commits o PR de EP4 que existan efectivamente.
- [ ] Confirmar el canal académico, adjuntar el APK y acordar con el docente la entrega privada del JKS.

## Evidencias disponibles

Las capturas corresponden a la ejecución del `app-demoRelease.apk` firmado. Los productos y la foto se crearon para la prueba. Se conservaron las capturas originales y se copiaron aquí las seleccionadas para documentación.

| Prueba | Evidencia | Resultado observado |
| --- | --- | --- |
| Frankfurter | [Cotización en Inicio](evidencias/ep4/01-inicio-frankfurter.png) | Un reintento devolvió 997,08 CLP por USD, con fecha de referencia 16/11/2026. |
| Inventario y HTTPS | [Conexión tras reiniciar Spring Boot](evidencias/ep4/10-conexion-tras-reinicio.png) | Inventario local visible y consulta remota exitosa. El listado activo quedó vacío tras archivar el producto de prueba. |
| Alta y edición | [Alta](evidencias/ep4/03-alta-servidor.png) · [Edición](evidencias/ep4/04-edicion-servidor.png) | El producto se creó con stock 5 y mínimo 2. La ubicación cambió de Estante A a Estante B. |
| Ajustes y archivado | [Stock actualizado](evidencias/ep4/05-stock-servidor.png) · [Archivado](evidencias/ep4/06-archivado-servidor.png) | El stock pasó de 5 a 6 y regresó a 5. El registro se archivó desde la app. |
| Persistencia PostgreSQL | [Registro antes](evidencias/ep4/06-producto-archivado-antes.json) · [Registro después](evidencias/ep4/08-producto-archivado-despues.json) | El mismo UUID conserva Estante B, stock 5 y `archivado: true` tras reiniciar Spring Boot. |
| Cámara y Room | [Captura confirmada](evidencias/ep4/07-captura-camara.png) · [Foto tras reabrir la app](evidencias/ep4/09-foto-persistente.png) | El producto local conserva la foto tomada con la cámara virtual. |
| Firma del APK | [Salida verificada y SHA-256](evidencias/ep4/firma-apk.txt) | `apksigner` verificó la firma v2. El SHA-256 instalado coincide con el archivo de entrega. |
| Automatización y cobertura | [Resumen extraído de los informes](evidencias/ep4/pruebas-y-cobertura.txt) · [Comandos y rutas de informes HTML/XML](cobertura-ep4.md) | Tres suites con 114 pruebas aprobadas. Los informes se generan con Gradle y Maven. |

## Evidencias para GitHub y Trello

| Destino | Preparar para la entrega | Estado |
| --- | --- | --- |
| GitHub | README, código, guías, migración Flyway, pruebas y evidencias seleccionadas de este documento. | Archivos locales revisados. La publicación de la rama y sus enlaces requieren autorización. |
| GitHub | Historial de cambios de EP4 y enlace al commit o PR de entrega, si corresponde. | Pendiente de una referencia real. Los PR #3 y #4 enlazados en el README corresponden a EP2. |
| Trello | Enlace o captura del tablero real. Mostrar las tarjetas existentes para backend, integración Android, API externa, pruebas, firma y documentación, según figure en el tablero. | Tablero y tarjetas pendientes de adjuntar o confirmar. |
| Trello | Enlazar desde las tarjetas reales la captura o el documento correspondiente. Por ejemplo, Frankfurter en integración externa, JaCoCo en pruebas y `apksigner` en firma. | Capturas y documentación disponibles para asociar una vez autorizado. |

## Archivos y canal de entrega

| Archivo o conjunto | Ubicación | Canal previsto |
| --- | --- | --- |
| Proyecto EP4 | `README.md`, `app/`, `gradle/`, wrappers, `backend/` y `docs/`. Incluir fuentes, pruebas, esquemas Room, migración Flyway y `.env.example`. | Rama de GitHub o archivo académico del código, según indique el docente. |
| APK firmado | `app/build/outputs/apk/demoRelease/app-demoRelease.apk` | Entrega académica. Adjuntar su SHA-256 y la verificación de firma. |
| Reportes JaCoCo | `app/build/reports/jacoco/logicaDebug/` y `backend/target/site/jacoco/` | Adjuntar HTML y XML si se solicitan los informes. Conservar las carpetas auxiliares del HTML al comprimirlo. |
| Evidencia instrumentada | `app/build/outputs/androidTest-results/` o el respaldo local de la corrida instrumentada. | Adjuntar el resultado de la suite si el docente solicita el detalle. |
| JKS de firma | `%LOCALAPPDATA%\AppKaiju\Firma\appkaiju-release.jks`, fuera del proyecto. | Entrega privada con acceso exclusivo al docente. Acordar un canal separado para la contraseña. [Procedimiento](firma-release-ep4.md#entrega-privada-del-jks) |

Conservar fuera de GitHub, Trello y del archivo del código el JKS, el almacén TLS `localhost.p12`, los archivos de credenciales de Windows, contraseñas, tokens, respaldos de bases y datos del AVD. Mantener también fuera `local.properties`, cachés y directorios generados de compilación. La huella del certificado y el resumen de `apksigner` son evidencia pública para la firma.

En el equipo de evaluación, las copias de los HTML/XML y el registro instrumentado están en `%LOCALAPPDATA%\AppKaiju\Entrega-EP4\reportes`. Esa carpeta también permite adjuntar los informes sin incluir los directorios completos de compilación.

Para revisar el inventario completo de cambios locales, incluidas las fuentes nuevas, ejecutar desde la raíz:

```powershell
git status --short --branch --untracked-files=all
git diff --check
```

## Condiciones de la evaluación

- El APK firmado de esta prueba requiere el certificado TLS de la instancia local con la que se compiló. Para reproducirlo en otro Windows con un certificado nuevo, regenerar la variante siguiendo la [guía de firma](firma-release-ep4.md).
- Kaiju Inventory API se usa en loopback. El CRUD de demostración carece de autenticación de aplicación. El reenvío está limitado al emulador autorizado.
- Frankfurter necesita Internet en el AVD. Si aparece el mensaje de conexión, usar **Reintentar cotización**.
- La evidencia de cámara proviene de **VirtualScene** en Android 15. El informe de cobertura distingue las pruebas JVM, las instrumentadas y el ensayo manual del APK firmado.
