package cl.duoc.appkaiju.inventario.producto.controller;

import cl.duoc.appkaiju.inventario.error.ApiException;
import cl.duoc.appkaiju.inventario.producto.dto.ProductoDto;
import cl.duoc.appkaiju.inventario.producto.service.ProductoService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;
import java.util.regex.Pattern;

@RestController
@RequestMapping(ProductoController.RUTA)
public class ProductoController {
    static final String RUTA = "/api/v1/productos";
    private static final Pattern ETAG_VERSION = Pattern.compile("\"(0|[1-9][0-9]{0,18})\"");
    private final ProductoService service;

    public ProductoController(ProductoService service) {
        this.service = service;
    }

    @GetMapping
    public ProductoDto.Pagina listar(
            @RequestParam(defaultValue = "0") @Min(0) @Max(1_000_000) int pagina,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int tamano,
            @RequestParam(defaultValue = "false") boolean incluirArchivados) {
        return service.listar(pagina, tamano, incluirArchivados);
    }

    @GetMapping("/{uuid}")
    public ResponseEntity<ProductoDto.Vista> consultar(@PathVariable UUID uuid) {
        var producto = service.consultar(uuid);
        return ResponseEntity.ok().eTag(etag(producto.version())).body(producto);
    }

    @PostMapping
    public ResponseEntity<ProductoDto.Vista> crear(@Valid @RequestBody ProductoDto.Alta solicitud) {
        var producto = service.crear(solicitud);
        return ResponseEntity.created(URI.create(RUTA + "/" + producto.uuid()))
                .eTag(etag(producto.version())).body(producto);
    }

    @PutMapping("/{uuid}")
    public ResponseEntity<ProductoDto.Vista> editar(@PathVariable UUID uuid,
            @RequestHeader(value = HttpHeaders.IF_MATCH, required = false) String ifMatch,
            @Valid @RequestBody ProductoDto.Edicion solicitud) {
        var producto = service.editar(uuid, versionDe(ifMatch), solicitud);
        return ResponseEntity.ok().eTag(etag(producto.version())).body(producto);
    }

    @DeleteMapping("/{uuid}")
    public ResponseEntity<Void> archivar(@PathVariable UUID uuid,
            @RequestHeader(value = HttpHeaders.IF_MATCH, required = false) String ifMatch) {
        service.archivar(uuid, versionDe(ifMatch));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{uuid}/ajustes-stock")
    public ResponseEntity<ProductoDto.AjusteAplicado> ajustarStock(
            @PathVariable UUID uuid, @Valid @RequestBody ProductoDto.Ajuste solicitud) {
        var ajuste = service.ajustarStock(uuid, solicitud);
        return ResponseEntity.ok().eTag(etag(ajuste.version())).body(ajuste);
    }

    private static String etag(long version) {
        return "\"" + version + "\"";
    }

    private static long versionDe(String ifMatch) {
        if (ifMatch == null) {
            throw new ApiException(HttpStatus.PRECONDITION_REQUIRED, "if_match_requerido",
                    "Envía el ETag del producto en la cabecera If-Match.");
        }
        var matcher = ETAG_VERSION.matcher(ifMatch.strip());
        if (matcher.matches()) {
            try {
                return Long.parseLong(matcher.group(1));
            } catch (NumberFormatException ignored) {
                // Un ETag con más capacidad que la columna version también se rechaza.
            }
        }
        throw new ApiException(HttpStatus.BAD_REQUEST, "if_match_invalido",
                "Envía un único ETag de versión entre comillas.");
    }
}
