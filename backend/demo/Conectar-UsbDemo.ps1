param(
    [Parameter(Mandatory)] [ValidatePattern('^[A-Za-z0-9_.:-]+$')] [string]$Serial,
    [switch]$Retirar,
    [string]$Adb = (Join-Path $env:LOCALAPPDATA 'Android\Sdk\platform-tools\adb.exe')
)

$ErrorActionPreference = 'Stop'
$puertoAdb = 5037

if ($Retirar) {
    & $Adb -P $puertoAdb -s $Serial reverse --remove tcp:8443
    if ($LASTEXITCODE -ne 0) { throw 'ADB no pudo retirar el reenvío de demostración.' }
    return
}

& $Adb -P $puertoAdb start-server
if ($LASTEXITCODE -ne 0) { throw 'ADB no está disponible.' }
$reenvios = & $Adb -P $puertoAdb -s $Serial reverse --list
if ($LASTEXITCODE -ne 0) { throw 'ADB no pudo consultar los reenvíos del dispositivo.' }
if ($reenvios -match '\s+tcp:(localhost:)?8443\s+') {
    throw 'El dispositivo ya tiene un reenvío para el puerto 8443. Retíralo si corresponde a la demostración.'
}

function Leer-Adb([IO.Stream]$Flujo, [int]$Longitud) {
    $buffer = [byte[]]::new($Longitud)
    $inicio = 0
    while ($inicio -lt $Longitud) {
        $cantidad = $Flujo.Read($buffer, $inicio, $Longitud - $inicio)
        if ($cantidad -eq 0) { throw 'ADB cerró la conexión antes de confirmar el reenvío.' }
        $inicio += $cantidad
    }
    [Text.Encoding]::UTF8.GetString($buffer)
}

function Confirmar-Adb([IO.Stream]$Flujo) {
    $estado = Leer-Adb $Flujo 4
    if ($estado -eq 'OKAY') { return }
    if ($estado -ne 'FAIL') { throw 'ADB devolvió una respuesta inesperada.' }
    $longitud = [Convert]::ToInt32((Leer-Adb $Flujo 4), 16)
    throw "ADB rechazó la solicitud. $(Leer-Adb $Flujo $longitud)"
}

# La CLI valida el origen de reverse TCP como un número de puerto. El servicio
# reverse de adbd admite localhost explícito, necesario para limitar la escucha.
$cliente = [Net.Sockets.TcpClient]::new()
try {
    $cliente.Connect([Net.IPAddress]::Loopback, $puertoAdb)
    $flujo = $cliente.GetStream()
    $flujo.ReadTimeout = 5000
    foreach ($servicio in "host:transport:$Serial", 'reverse:forward:tcp:localhost:8443;tcp:8443') {
        $mensaje = [Text.Encoding]::ASCII.GetBytes($servicio)
        $cabecera = [Text.Encoding]::ASCII.GetBytes($mensaje.Length.ToString('X4'))
        $flujo.Write($cabecera, 0, $cabecera.Length)
        $flujo.Write($mensaje, 0, $mensaje.Length)
        Confirmar-Adb $flujo
    }
    Confirmar-Adb $flujo
} finally {
    $cliente.Dispose()
}

"Reenvío privado activo en $Serial para https://localhost:8443/."
