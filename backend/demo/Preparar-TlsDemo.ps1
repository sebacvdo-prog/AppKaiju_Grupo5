param([string]$Keytool = (Join-Path $env:JAVA_HOME 'bin\keytool.exe'))

$ErrorActionPreference = 'Stop'
$privado = Join-Path $env:LOCALAPPDATA 'AppKaiju\DemoUsb'
$llaves = Join-Path $privado 'localhost.p12'
$credencial = Join-Path $privado 'tls.credential.xml'
$certificado = Join-Path $privado 'localhost.pem'

if ((Test-Path -LiteralPath $llaves) -ne (Test-Path -LiteralPath $credencial)) {
    throw 'El almacén TLS y su credencial no coinciden. Revisa el directorio privado antes de continuar.'
}
if ((Test-Path -LiteralPath $certificado) -and !(Test-Path -LiteralPath $llaves)) {
    throw 'Ya existe un certificado de demostración sin su almacén TLS. Revisa el directorio privado.'
}

New-Item -ItemType Directory -Path $privado -Force | Out-Null
$nueva = !(Test-Path -LiteralPath $llaves)
if ($nueva) {
    $bytes = [byte[]]::new(32)
    $rng = [Security.Cryptography.RandomNumberGenerator]::Create()
    try { $rng.GetBytes($bytes) } finally { $rng.Dispose() }
    $clave = ConvertTo-SecureString ([Convert]::ToBase64String($bytes)) -AsPlainText -Force
    [Array]::Clear($bytes, 0, $bytes.Length)
    $cred = [pscredential]::new('localhost', $clave)
} else {
    $cred = Import-Clixml -LiteralPath $credencial
}

$anterior = $env:KAIJU_DEMO_TLS_PASSWORD
try {
    $env:KAIJU_DEMO_TLS_PASSWORD = $cred.GetNetworkCredential().Password
    if ($nueva) {
        & $Keytool -genkeypair -storetype PKCS12 -keystore $llaves -alias localhost `
            -keyalg RSA -keysize 3072 -validity 365 -dname 'CN=localhost' `
            -ext 'SAN=DNS:localhost' -ext 'EKU=serverAuth' -storepass:env KAIJU_DEMO_TLS_PASSWORD
        if ($LASTEXITCODE -ne 0) { throw 'No se pudo generar el almacén TLS.' }
        $cred | Export-Clixml -LiteralPath $credencial
    }
    if (!(Test-Path -LiteralPath $certificado)) {
        & $Keytool -exportcert -rfc -storetype PKCS12 -keystore $llaves -alias localhost `
            -file $certificado -storepass:env KAIJU_DEMO_TLS_PASSWORD
        if ($LASTEXITCODE -ne 0) { throw 'No se pudo exportar el certificado público.' }
    }
} finally {
    $env:KAIJU_DEMO_TLS_PASSWORD = $anterior
}

"Certificado público TLS: $certificado"
