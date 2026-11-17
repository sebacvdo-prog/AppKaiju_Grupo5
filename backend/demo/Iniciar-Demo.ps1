param([string]$Java = (Join-Path $env:JAVA_HOME 'bin\java.exe'))

$ErrorActionPreference = 'Stop'
$privado = Join-Path $env:LOCALAPPDATA 'AppKaiju\DemoUsb'
$jar = Join-Path $privado 'backend-build\kaiju-inventory-api-0.1.0-SNAPSHOT.jar'
$db = Import-Clixml -LiteralPath (Join-Path $privado 'postgres.credential.xml')
$tls = Import-Clixml -LiteralPath (Join-Path $privado 'tls.credential.xml')
$llaves = Get-Item -LiteralPath (Join-Path $privado 'localhost.p12')
if ($db.UserName -ne 'kaiju_demo') { throw 'Selecciona las credenciales de la base de demostración.' }
if (!(Test-Path -LiteralPath $jar -PathType Leaf)) { throw 'Compila primero el JAR del backend.' }

$dbAnterior = $env:SPRING_DATASOURCE_PASSWORD
$tlsAnterior = $env:SERVER_SSL_KEY_STORE_PASSWORD
try {
    $env:SPRING_DATASOURCE_PASSWORD = $db.GetNetworkCredential().Password
    $env:SERVER_SSL_KEY_STORE_PASSWORD = $tls.GetNetworkCredential().Password
    & $Java -jar $jar '--server.address=127.0.0.1' '--server.port=8443' '--server.ssl.enabled=true' `
        "--server.ssl.key-store=$([uri]::new($llaves.FullName).AbsoluteUri)" `
        '--server.ssl.key-store-type=PKCS12' '--server.ssl.key-alias=localhost' `
        '--spring.datasource.url=jdbc:postgresql://127.0.0.1:5432/kaiju_inventory_demo' '--spring.datasource.username=kaiju_demo'
    if ($LASTEXITCODE -ne 0) { throw 'El backend de demostración terminó con un error.' }
} finally {
    $env:SPRING_DATASOURCE_PASSWORD = $dbAnterior
    $env:SERVER_SSL_KEY_STORE_PASSWORD = $tlsAnterior
}
