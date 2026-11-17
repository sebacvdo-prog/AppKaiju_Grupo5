# Demostración HTTPS de AppKaiju en el emulador

La variante `demoRelease` usa `https://localhost:8443/` y el identificador `com.example.appkaiju_grupo5.demo`. Se puede instalar junto a AppKaiju debug. Cada paquete conserva su propio inventario Room y sus fotos. La variante regular de release sigue usando su propia configuración HTTPS. En Windows, la instancia de demostración de Spring Boot usa un segundo puerto y una base nueva en PostgreSQL.

## Guion breve de evaluación

1. Iniciar Spring Boot con `Iniciar-Demo.ps1` y confirmar que `GET /api/v1/productos` devuelve HTTP 200 en `https://localhost:8443`.
2. Iniciar un AVD Android 15 (API 35), activar el reenvío de ADB e instalar `app-demoRelease.apk` si aún no está instalado. Abrir **AppKaiju Demo**.
3. En **Inicio**, actualizar la cotización USD/CLP. Abrir **Perfil** y regresar a **Productos** para mostrar la navegación. La consulta a Frankfurter requiere Internet en el emulador.
4. En **Productos → Consultar servidor**, registrar un producto ficticio con stock `5` y mínimo `2`. Editar la ubicación, sumar y restar una unidad y confirmar el archivado. El registro desaparece del listado activo. La consulta con `incluirArchivados=true` permite verificar que PostgreSQL lo conserva.
5. Volver al inventario local. Guardar un producto ficticio, abrir **Editar → Tomar foto**, confirmar la captura con la cámara virtual y guardar. Cerrar y reabrir AppKaiju para mostrar la foto persistente.
6. Para comprobar persistencia remota, reiniciar solo Spring Boot de demostración. Repetir la consulta de archivados por HTTPS y confirmar ubicación, stock y estado. Las evidencias de la ejecución verificada están en el [checklist de entrega](entrega-ep4.md#evidencias-disponibles).

## Preparación inicial en Windows

Ejecutar desde la raíz del repositorio, en PowerShell con `JAVA_HOME` apuntando a un JDK válido. El comando de ejemplo usa el JDK de Android Studio en este equipo.

```powershell
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
Get-Service -Name postgresql-x64-17
```

Ejecutar la preparación de la base en una terminal local. `psql` pedirá la contraseña del administrador `postgres` y, si el rol es nuevo, la contraseña independiente de `kaiju_demo`. Introducirlas en la terminal. El script valida la conexión y protege la credencial del rol de demostración con DPAPI bajo el usuario actual de Windows.

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\backend\demo\Preparar-BaseDemo.ps1
```

El servicio de PostgreSQL debe estar activo y escuchar en loopback en el puerto `5432`. El nombre del servicio puede variar según la instalación. El script provisiona el rol `kaiju_demo` y la base `kaiju_inventory_demo`. Flyway aplica la migración V1 al esquema vacío en el primer arranque.

Generar el certificado público y su clave TLS con `keytool`. Se guardan en `%LOCALAPPDATA%\AppKaiju\DemoUsb`. El archivo `localhost.pem` contiene exclusivamente el certificado público. El archivo `localhost.p12` contiene la clave privada del servidor y se mantiene en Windows junto con su credencial protegida por DPAPI. El script conserva un almacén TLS existente.

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\backend\demo\Preparar-TlsDemo.ps1
```

Compilar el backend de demostración en un directorio separado. Windows puede bloquear el JAR que usa el backend de debug mientras está en ejecución.

```powershell
$build = Join-Path $env:LOCALAPPDATA 'AppKaiju\DemoUsb\backend-build'
.\backend\mvnw.cmd -f backend/pom.xml "-Dkaiju.build.directory=$build" verify
```

## Iniciar Spring Boot y verificar el certificado

Mantener abierta la terminal en la que se inicia el backend de demostración. El lanzador fuerza `127.0.0.1:8443` y la base de demostración. La instancia de debug puede seguir en `127.0.0.1:8080`.

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\backend\demo\Iniciar-Demo.ps1
```

Desde otra terminal, consultar el inventario con el certificado de la demo como ancla de confianza. Revisar la fecha de expiración y confirmar que el SAN incluye `localhost`.

```powershell
$cert = Join-Path $env:LOCALAPPDATA 'AppKaiju\DemoUsb\localhost.pem'
curl.exe --noproxy '*' --cacert $cert --fail --silent --show-error 'https://localhost:8443/api/v1/productos'
& "$env:JAVA_HOME\bin\keytool.exe" -printcert -file $cert
```

Dentro de Android, la variante de evaluación empaqueta solo la parte pública del certificado. Aplica esa confianza exclusivamente a `localhost` y exige HTTPS para ese host. Para el resto de los hosts, como Frankfurter, usa la confianza habitual de Android. Si se cambia el certificado del servidor, hay que recompilar el APK de demostración con el nuevo certificado público y volver a verificarlo.

Para comprobar el registro de demostración archivado antes y después de reiniciar Spring Boot, consultar con el mismo certificado.

```powershell
curl.exe --noproxy '*' --cacert $cert --fail --silent --show-error 'https://localhost:8443/api/v1/productos?incluirArchivados=true'
```

## Generar el APK firmado

Ejecutar localmente con una instancia de demostración activa. Si ya existe un JKS en otra ubicación, indicar `-Keystore` con su ruta absoluta y `-AliasFirma` si usa otro alias. El script valida HTTPS antes de firmar. Si falta el JKS, pide confirmación local para generarlo. `keytool` y PowerShell ocultan las contraseñas. Gradle las recibe en el entorno de esa ejecución y el script las retira al terminar.

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\backend\demo\Firmar-ApkDemo.ps1
```

El APK firmado se genera en `app/build/outputs/apk/demoRelease/app-demoRelease.apk`. Si el APK ya existe y corresponde al certificado TLS del servidor, se puede verificar e instalar directamente. [Procedimiento de verificación y entrega del JKS](firma-release-ep4.md)

## Iniciar el emulador e instalar

Crear un AVD dedicado en **Android Studio → Device Manager**, con una imagen API 35 instalada y cámara trasera **VirtualScene**. Abrirlo desde Device Manager o, en otra terminal de PowerShell, listar los AVD e iniciar el elegido.

```powershell
$sdk = Join-Path $env:LOCALAPPDATA 'Android\Sdk'
& "$sdk\emulator\emulator.exe" -list-avds
& "$sdk\emulator\emulator.exe" -avd 'NOMBRE_DEL_AVD'
```

En el equipo de la evaluación, el AVD verificado se llama `AppKaiju_DemoRelease`. Se creó con un directorio de AVD dedicado. Si aún no está activo, se puede iniciarlo con estos comandos.

```powershell
$sdk = Join-Path $env:LOCALAPPDATA 'Android\Sdk'
$env:ANDROID_AVD_HOME = Join-Path $env:LOCALAPPDATA 'AppKaiju\DemoUsb\avd-release'
& "$sdk\emulator\emulator.exe" -avd 'AppKaiju_DemoRelease' -port 5580
```

Desde la raíz del repositorio, usar el serial del emulador que aparezca con estado `device`. En la validación fue `emulator-5580`. Elegir otro AVD limpio si ya existe una instalación del mismo paquete firmada con una clave distinta.

```powershell
$adb = Join-Path $env:LOCALAPPDATA 'Android\Sdk\platform-tools\adb.exe'
& $adb devices -l
$serial = Read-Host 'Serial del emulador'
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\backend\demo\Conectar-UsbDemo.ps1 -Serial $serial
& $adb -s $serial reverse --list
& $adb -s $serial install -r .\app\build\outputs\apk\demoRelease\app-demoRelease.apk
& $adb -s $serial shell am start -W -n com.example.appkaiju_grupo5.demo/com.example.appkaiju_grupo5.MainActivity
```

El script usa el protocolo local de ADB para que `adbd` escuche en el loopback de Android. La CLI 37.0.1 rechaza la sintaxis con host explícito para el puerto de origen. `adb reverse --list` muestra el mapeo como `tcp:8443 tcp:8443`. Si se reinicia el AVD o se pierde el transporte ADB, revisar el listado y volver a ejecutar el script cuando el mapeo ya no aparezca.

Al terminar, retirar el reenvío del emulador y detener la instancia demo desde su terminal. Los registros de PostgreSQL y los datos locales del AVD quedan conservados.

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\backend\demo\Conectar-UsbDemo.ps1 -Serial $serial -Retirar
```
