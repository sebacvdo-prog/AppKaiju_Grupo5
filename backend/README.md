# Kaiju Inventory API

Microservicio de inventario para AppKaiju. Usa Java 21, Spring Boot, Spring Web MVC, Spring Data JPA, Bean Validation, PostgreSQL y Flyway. El proyecto Maven en esta carpeta se compila de forma independiente del módulo Android.

La [guía de evaluación en el emulador](../docs/demo-usb-ep4.md) describe cómo arrancar una instancia HTTPS independiente en el puerto `8443` con la base `kaiju_inventory_demo`. Android la alcanza mediante `adb reverse`.

## Preparar PostgreSQL en Windows

Usa una instancia de PostgreSQL disponible. Para configurar una instancia local, consulta el [instalador de PostgreSQL para Windows](https://www.postgresql.org/download/windows/). La suite automatizada usa PostgreSQL temporal y puede ejecutarse antes de configurar una instancia para el servidor.

En PowerShell, con el JDK 21 configurado mediante `JAVA_HOME`, comprueba la versión y entra al proyecto Maven desde la raíz del repositorio.

~~~powershell
java -version
Set-Location backend
.\mvnw.cmd -v
~~~

Para preparar una base local nueva, abre psql con un usuario administrador de PostgreSQL. Si `psql` no está en PATH, puedes abrir SQL Shell (psql) desde el menú de Windows.

~~~powershell
psql -h localhost -U postgres -d postgres
~~~

~~~sql
CREATE ROLE kaiju LOGIN;
\password kaiju
CREATE DATABASE kaiju_inventory OWNER kaiju;
\q
~~~

Configura la conexión en la misma sesión de PowerShell que iniciará Spring Boot. [.env.example](.env.example) muestra los nombres y valores de referencia. PowerShell necesita que las variables se asignen como se muestra abajo.

~~~powershell
$env:KAIJU_DB_URL = 'jdbc:postgresql://localhost:5432/kaiju_inventory'
$env:KAIJU_DB_USER = 'kaiju'
$clave = Read-Host 'Contraseña del usuario de PostgreSQL' -AsSecureString
$env:KAIJU_DB_PASSWORD = [pscredential]::new($env:KAIJU_DB_USER, $clave).GetNetworkCredential().Password

.\mvnw.cmd spring-boot:run
~~~

Flyway aplica la migración V1 al iniciar y Hibernate valida el esquema. El servidor escucha por defecto en `127.0.0.1:8080`. Para otro puerto local, establece `SERVER_PORT` antes de arrancar. Los endpoints de esta etapa están previstos para pruebas locales.

## Compilar y probar

Desde `backend/`, ejecuta el siguiente comando con JDK 21. El wrapper prepara Maven en la primera ejecución. La suite inicia PostgreSQL embebido en un puerto dinámico, ejecuta Flyway y envía solicitudes HTTP al servidor Spring Boot. En Windows está prevista para x64. La base temporal se cierra al terminar.

~~~powershell
.\mvnw.cmd clean verify
~~~

El JAR ejecutable queda en `target/kaiju-inventory-api-0.1.0-SNAPSHOT.jar` y los resultados de Surefire en `target/surefire-reports`. Para ejecutar el JAR, utiliza las mismas variables de base de datos.

~~~powershell
& "$env:JAVA_HOME\bin\java.exe" -jar .\target\kaiju-inventory-api-0.1.0-SNAPSHOT.jar
~~~

Windows puede bloquear un JAR que todavía está en ejecución. Para empaquetar otra instancia sin detenerla, usar `-Dkaiju.build.directory` con una ruta de salida distinta. La [guía de demostración](../docs/demo-usb-ep4.md#preparación-inicial-en-windows) usa este procedimiento para generar un JAR independiente.

## API

| Método | Ruta | Respuesta |
|---|---|---|
| GET | `/api/v1/productos` | `200`. Página con `productos`, `pagina`, `tamano`, `totalProductos` y `totalPaginas`. `pagina` empieza en 0, `tamano` predeterminado es 20 y admite de 1 a 100. El orden es por UUID. Usa `incluirArchivados=true` para incluir archivados. |
| GET | `/api/v1/productos/{uuid}` | `200` con `ETag`. Devuelve `404` para un UUID inexistente o archivado. |
| POST | `/api/v1/productos` | `201` con `Location` y `ETag`. Recibe `uuid`, `nombre`, `categoria`, `ubicacion`, `stock` y `stockMinimo`. Conserva el mismo UUID al reintentar la misma alta. |
| PUT | `/api/v1/productos/{uuid}` | `200` con `ETag`. Requiere `If-Match` y recibe `nombre`, `categoria`, `ubicacion`, `stock` y `stockMinimo`. |
| DELETE | `/api/v1/productos/{uuid}` | `204`. Requiere `If-Match`. Marca el registro como archivado y conserva sus datos. |
| POST | `/api/v1/productos/{uuid}/ajustes-stock` | `200` con `ETag`. Recibe `operacionId` y `delta`. Devuelve `uuid`, `operacionId`, `delta`, `stock` y `version` del ajuste. |

El stock y el mínimo son enteros entre 0 y 2.147.483.647. Un delta debe ser un entero distinto de cero y su saldo resultante debe quedar dentro del mismo rango. Nombre, categoría y ubicación son obligatorios. Las respuestas de producto contienen `version` y `archivado`.

Usa el `ETag` devuelto por el servidor, incluidas las comillas, en `If-Match`. Un valor ausente devuelve `428`. Una versión antigua devuelve `412`. Los errores de formato o validación devuelven `400`. Un UUID de alta reutilizado con otros datos, un `operacionId` reutilizado en el mismo producto con otro delta o un saldo imposible devuelven `409`.

Si se repite un alta o un ajuste con los mismos datos, la API devuelve el recibo original. Su stock y versión corresponden a esa operación. Para obtener la revisión actual, usa GET.

### Probar desde otra ventana de PowerShell

~~~powershell
$base = 'http://127.0.0.1:8080/api/v1/productos'
$uuid = [guid]::NewGuid().ToString()
$producto = @{
    uuid = $uuid
    nombre = 'Linterna'
    categoria = 'Herramientas'
    ubicacion = 'Estante A-01'
    stock = 5
    stockMinimo = 2
}
$alta = Invoke-WebRequest -UseBasicParsing -Method Post -Uri $base -ContentType 'application/json' -Body ($producto | ConvertTo-Json)
$alta.StatusCode
Invoke-RestMethod -Uri "$base/$uuid"

$ajuste = @{ operacionId = [guid]::NewGuid().ToString(); delta = -1 }
Invoke-RestMethod -Method Post -Uri "$base/$uuid/ajustes-stock" -ContentType 'application/json' -Body ($ajuste | ConvertTo-Json)
~~~

## Estructura

- `producto/controller` expone el contrato HTTP.
- `producto/dto` define entradas, respuestas y validaciones.
- `producto/service` aplica reglas de stock, concurrencia e idempotencia en transacciones.
- `producto/entity` y `producto/repository` modelan el inventario y los recibos con JPA.
- `error` devuelve errores HTTP mediante Problem Details.
- `src/main/resources/db/migration` contiene el esquema versionado de PostgreSQL.
