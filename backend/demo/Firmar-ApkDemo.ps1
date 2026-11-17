param(
    [string]$Keystore = (Join-Path $env:LOCALAPPDATA 'AppKaiju\Firma\appkaiju-release.jks'),
    [string]$AliasFirma = 'appkaiju-release',
    [string]$BuildTools = (Join-Path $env:LOCALAPPDATA 'Android\Sdk\build-tools\36.0.0')
)

$ErrorActionPreference = 'Stop'
$repo = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
$certificado = Join-Path $env:LOCALAPPDATA 'AppKaiju\DemoUsb\localhost.pem'
$generado = Join-Path $repo 'app\build\outputs\apk\demoRelease\app-demoRelease.apk'
$keytool = Join-Path $env:JAVA_HOME 'bin\keytool.exe'
$apksigner = Join-Path $BuildTools 'apksigner.bat'

foreach ($archivo in $certificado, $keytool, $apksigner) {
    if (!(Test-Path -LiteralPath $archivo -PathType Leaf)) { throw "Falta $archivo" }
}
$estado = & curl.exe --noproxy '*' --cacert $certificado --fail --silent --show-error --max-time 10 `
    --output NUL --write-out '%{http_code}' 'https://localhost:8443/api/v1/productos'
if ($LASTEXITCODE -ne 0 -or $estado -ne '200') {
    throw 'Inicia el backend HTTPS de demostración y comprueba su certificado antes de firmar.'
}

if (![IO.Path]::IsPathRooted($Keystore)) { throw 'Indica una ruta absoluta al JKS.' }
$destino = [IO.Path]::GetFullPath($Keystore)
if ($destino.StartsWith("$repo\", [StringComparison]::OrdinalIgnoreCase)) { throw 'Guarda el JKS fuera del repositorio.' }
if (!(Test-Path -LiteralPath $destino)) {
    if ((Read-Host "No existe JKS en $destino. Escribe CREAR para generar uno") -cne 'CREAR') {
        throw 'Indica la ruta al JKS existente con -Keystore si el equipo ya dispone de uno.'
    }
    New-Item -Path (Split-Path $destino -Parent) -ItemType Directory -Force | Out-Null
    & $keytool -genkeypair -storetype JKS -keystore $destino -alias $AliasFirma -keyalg RSA -keysize 3072 -validity 10000
    if ($LASTEXITCODE -ne 0) { throw 'No se pudo generar el JKS de firma.' }
}

$variables = @('KAIJU_RELEASE_API_BASE_URL', 'KAIJU_DEMO_TLS_CERT', 'KAIJU_SIGNING_STORE_FILE', 'KAIJU_SIGNING_KEY_ALIAS',
    'KAIJU_SIGNING_STORE_PASSWORD', 'KAIJU_SIGNING_KEY_PASSWORD')
$anteriores = @{}
foreach ($variable in $variables) { $anteriores[$variable] = [Environment]::GetEnvironmentVariable($variable) }
try {
    Remove-Item Env:KAIJU_RELEASE_API_BASE_URL -ErrorAction SilentlyContinue
    $env:KAIJU_DEMO_TLS_CERT = $certificado
    $env:KAIJU_SIGNING_STORE_FILE = $destino
    $env:KAIJU_SIGNING_KEY_ALIAS = $AliasFirma
    $env:KAIJU_SIGNING_STORE_PASSWORD = ([pscredential]::new('JKS', (Read-Host 'Contraseña del JKS' -AsSecureString))).GetNetworkCredential().Password
    $env:KAIJU_SIGNING_KEY_PASSWORD = ([pscredential]::new('Clave', (Read-Host 'Contraseña de la clave de firma' -AsSecureString))).GetNetworkCredential().Password
    & (Join-Path $repo 'gradlew.bat') -p $repo :app:assembleDemoRelease --no-daemon --no-configuration-cache --console=plain
    if ($LASTEXITCODE -ne 0) { throw 'Falló la compilación del APK firmado.' }
} finally {
    foreach ($variable in $variables) { [Environment]::SetEnvironmentVariable($variable, $anteriores[$variable]) }
}

& $apksigner verify --verbose --print-certs $generado
if ($LASTEXITCODE -ne 0) { throw 'La firma del APK no es válida.' }
$flujo = [IO.File]::OpenRead($generado)
try {
    $sha256 = [Security.Cryptography.SHA256]::Create()
    try {
        $huella = [BitConverter]::ToString($sha256.ComputeHash($flujo)).Replace('-', '')
    } finally {
        $sha256.Dispose()
    }
} finally {
    $flujo.Dispose()
}
"SHA-256 del APK: $huella"
"APK firmado: $generado"
"JKS de firma: $destino"
