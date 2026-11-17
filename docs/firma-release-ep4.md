# Generación y firma del APK de AppKaiju · IE 3.3.1

El APK de la EP4 corresponde a la variante `demoRelease`. Se validó en el AVD `AppKaiju_DemoRelease` con Android 15 (API 35).

| Dato | Valor verificado |
| --- | --- |
| Archivo | `app/build/outputs/apk/demoRelease/app-demoRelease.apk` |
| Identificador | `com.example.appkaiju_grupo5.demo` |
| Versión | `versionName` `1.0` · `versionCode` `1` |
| Firma | APK Signature Scheme v2, un firmante RSA de 3.072 bits |
| Alias de firma | `appkaiju-release` |
| SHA-256 del APK | `EFC0113368EB38DBDA87712C92D9A630C07BEA4609F8572ABF30816A6590D85D` |
| SHA-256 del certificado público de firma | `90BCFC07FD8BD56D9247F7F09F15C44246008EDA8F947D82A80AA6E230CEED5A` |

El SHA-256 del APK instalado coincidió con el del archivo anterior. Las huellas identifican el artefacto que se verificó. Repetir el cálculo si se genera un APK nuevo.

## Generar en Windows

1. Preparar PostgreSQL y TLS, y dejar Spring Boot de demostración activo en `https://localhost:8443`. [Procedimiento HTTPS en el emulador](demo-usb-ep4.md)
2. Ejecutar el script desde la raíz del proyecto con un JDK válido. Con un JKS existente en una ubicación distinta a la predeterminada, indicar `-Keystore` con su ruta absoluta y `-AliasFirma` con el alias correspondiente.

```powershell
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\backend\demo\Firmar-ApkDemo.ps1
```

El script consulta primero la API por HTTPS y usa el JKS existente. La ubicación predeterminada es `%LOCALAPPDATA%\AppKaiju\Firma\appkaiju-release.jks`. Si falta y se necesita generar uno, el script pide escribir `CREAR` antes de invocar `keytool`. Introducir las contraseñas en la terminal con entrada oculta. Gradle las recibe en el entorno de la compilación y el script limpia esas variables al finalizar. El script verifica la firma y calcula el SHA-256 con .NET.

`demoRelease` empaqueta el certificado público TLS de `localhost`, distinto del certificado de firma Android. El JKS de firma y el almacén TLS permanecen fuera del repositorio. En otro Windows, un TLS recién generado requiere regenerar el APK con ese nuevo certificado público para conectarse a Spring Boot. El APK ya verificado funciona con la configuración TLS usada para compilarlo.

## Verificar e instalar

Verificar un APK existente desde la raíz del proyecto. Esta comprobación usa el certificado público incluido en el APK. Ajustar la ruta si el SDK o Build Tools están en otra ubicación.

```powershell
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
$apk = 'app/build/outputs/apk/demoRelease/app-demoRelease.apk'
$buildTools = Join-Path $env:LOCALAPPDATA 'Android\Sdk\build-tools\36.0.0'
& "$buildTools\apksigner.bat" verify --verbose --print-certs $apk
if ($LASTEXITCODE -ne 0) { throw 'La firma del APK no es válida.' }
& "$buildTools\aapt2.exe" dump badging $apk | Select-String '^package:'
certutil.exe -hashfile $apk SHA256
```

Instalar con `adb -s SERIAL install -r RUTA_APK` sobre el AVD de evaluación, con el reenvío activo. Elegir un AVD separado si ya existe una instalación con el mismo identificador y un certificado de firma distinto. [Comandos de inicio, reenvío e instalación](demo-usb-ep4.md#iniciar-el-emulador-e-instalar)

La variante general `release` usa el identificador `com.example.appkaiju_grupo5` y admite un backend HTTPS mediante `KAIJU_RELEASE_API_BASE_URL`. Para documentar y evaluar el flujo de esta EP4 se usa el APK `demoRelease` con su puerto local y su certificado específico.

## Entrega privada del JKS

Para cumplir la exigencia académica del JKS, confirmar con el docente el canal de acceso restringido, por ejemplo la entrega privada del aula virtual. Enviar el archivo únicamente por ese canal y comunicar la contraseña por un medio separado acordado con el docente. Si el docente acepta evidencia alternativa, compartir el certificado público o su huella y la salida de `apksigner`.

En GitHub o Trello se puede publicar la huella del APK, la huella del certificado público y la salida relevante de la verificación. El JKS contiene la clave privada de firma y se conserva fuera de ambos servicios, junto a un respaldo privado. [Checklist de archivos y evidencias](entrega-ep4.md)
