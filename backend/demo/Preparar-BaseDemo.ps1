param([string]$Psql = 'C:\Program Files\PostgreSQL\17\bin\psql.exe')

$ErrorActionPreference = 'Stop'
$privado = Join-Path $env:LOCALAPPDATA 'AppKaiju\DemoUsb'
$archivo = Join-Path $privado 'postgres.credential.xml'
$env:PGCLIENTENCODING = 'UTF8'

& $Psql -X -P pager=off -h 127.0.0.1 -p 5432 -U postgres -d postgres -W -f (Join-Path $PSScriptRoot 'preparar-base.sql')
if ($LASTEXITCODE -ne 0) { throw 'No se pudo preparar la base de demostración.' }

if (Test-Path -LiteralPath $archivo) {
    $credencial = Import-Clixml -LiteralPath $archivo
} else {
    $clave = Read-Host 'Repite la contraseña de kaiju_demo para iniciar el backend' -AsSecureString
    $credencial = [pscredential]::new('kaiju_demo', $clave)
}

$pgPasswordAnterior = $env:PGPASSWORD
try {
    $env:PGPASSWORD = $credencial.GetNetworkCredential().Password
    & $Psql -X -P pager=off -h 127.0.0.1 -p 5432 -U kaiju_demo -d kaiju_inventory_demo -w -Atc 'SELECT current_database(), current_user;'
    if ($LASTEXITCODE -ne 0) { throw 'No se pudo conectar como kaiju_demo. Revisa la contraseña.' }
} finally {
    $env:PGPASSWORD = $pgPasswordAnterior
}

if (!(Test-Path -LiteralPath $archivo)) {
    New-Item -Path $privado -ItemType Directory -Force | Out-Null
    $credencial | Export-Clixml -LiteralPath $archivo
}
'Base de demostración lista.'
