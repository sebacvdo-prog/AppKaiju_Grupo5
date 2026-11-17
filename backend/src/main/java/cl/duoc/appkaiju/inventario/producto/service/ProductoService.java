package cl.duoc.appkaiju.inventario.producto.service;

import cl.duoc.appkaiju.inventario.error.ApiException;
import cl.duoc.appkaiju.inventario.producto.dto.ProductoDto;
import cl.duoc.appkaiju.inventario.producto.entity.AjusteStock;
import cl.duoc.appkaiju.inventario.producto.entity.Producto;
import cl.duoc.appkaiju.inventario.producto.repository.AjusteStockRepository;
import cl.duoc.appkaiju.inventario.producto.repository.ProductoRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class ProductoService {
    private final ProductoRepository productos;
    private final AjusteStockRepository ajustes;
    private final ObjectMapper json;

    public ProductoService(ProductoRepository productos, AjusteStockRepository ajustes, ObjectMapper json) {
        this.productos = productos;
        this.ajustes = ajustes;
        this.json = json;
    }

    @Transactional(readOnly = true)
    public ProductoDto.Pagina listar(int pagina, int tamano, boolean incluirArchivados) {
        var paginacion = PageRequest.of(pagina, tamano, Sort.by("uuid"));
        var resultado = incluirArchivados ? productos.findAll(paginacion) : productos.findByArchivadoFalse(paginacion);
        return new ProductoDto.Pagina(resultado.map(ProductoService::vista).getContent(), pagina, tamano,
                resultado.getTotalElements(), resultado.getTotalPages());
    }

    @Transactional(readOnly = true)
    public ProductoDto.Vista consultar(UUID uuid) {
        return productos.findByUuidAndArchivadoFalse(uuid).map(ProductoService::vista)
                .orElseThrow(ProductoService::productoNoDisponible);
    }

    @Transactional
    public ProductoDto.Vista crear(ProductoDto.Alta alta) {
        var datos = alta.datos().normalizada();
        try {
            String solicitud = json.writeValueAsString(datos);
            int insertados = productos.insertarSiNoExiste(alta.uuid(), datos.nombre(), datos.categoria(), datos.ubicacion(),
                    datos.stock(), datos.stockMinimo(), solicitud);
            if (insertados == 0) {
                var anterior = productos.findById(alta.uuid()).orElseThrow();
                var datosAnteriores = json.readValue(anterior.getSolicitudAlta(), ProductoDto.Edicion.class);
                if (!datosAnteriores.equals(datos)) {
                    throw new ApiException(HttpStatus.CONFLICT, "uuid_en_uso", "El UUID corresponde a otra solicitud de alta.");
                }
            }
            return new ProductoDto.Vista(alta.uuid(), datos.nombre(), datos.categoria(), datos.ubicacion(),
                    datos.stock(), datos.stockMinimo(), false, 0);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("No se pudo procesar la solicitud de alta.", exception);
        }
    }

    @Transactional
    public ProductoDto.Vista editar(UUID uuid, long versionEsperada, ProductoDto.Edicion solicitud) {
        var producto = bloquear(uuid);
        exigirActivo(producto);
        exigirVersion(producto, versionEsperada);
        var datos = solicitud.normalizada();
        producto.editar(datos.nombre(), datos.categoria(), datos.ubicacion(), datos.stock(), datos.stockMinimo());
        productos.flush();
        return vista(producto);
    }

    @Transactional
    public void archivar(UUID uuid, long versionEsperada) {
        var producto = bloquear(uuid);
        exigirVersion(producto, versionEsperada);
        producto.archivar();
    }

    @Transactional
    public ProductoDto.AjusteAplicado ajustarStock(UUID uuid, ProductoDto.Ajuste solicitud) {
        if (solicitud.delta() == 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "delta_invalido", "El ajuste debe ser distinto de cero.");
        }

        // El bloqueo cubre la revisión del recibo, el cambio de stock y el registro de la operación.
        var producto = bloquear(uuid);
        var anterior = ajustes.findByProductoUuidAndOperacionId(uuid, solicitud.operacionId());
        if (anterior.isPresent()) {
            var ajuste = anterior.get();
            if (ajuste.getDelta() != solicitud.delta()) {
                throw new ApiException(HttpStatus.CONFLICT, "operacion_en_uso", "La operación ya tiene otro delta.");
            }
            return recibo(ajuste);
        }

        exigirActivo(producto);
        long stockResultante = (long) producto.getStock() + solicitud.delta();
        if (stockResultante < 0 || stockResultante > Integer.MAX_VALUE) {
            throw new ApiException(HttpStatus.CONFLICT, "stock_fuera_de_rango", "El ajuste deja el stock fuera de rango.");
        }
        producto.actualizarStock((int) stockResultante);
        productos.flush();
        return recibo(ajustes.save(new AjusteStock(uuid, solicitud.operacionId(), solicitud.delta(),
                producto.getStock(), producto.getVersion())));
    }

    private Producto bloquear(UUID uuid) {
        return productos.bloquearPorUuid(uuid).orElseThrow(ProductoService::productoNoDisponible);
    }

    private static void exigirActivo(Producto producto) {
        if (producto.isArchivado()) {
            throw productoNoDisponible();
        }
    }

    private static void exigirVersion(Producto producto, long versionEsperada) {
        if (producto.getVersion() != versionEsperada) {
            throw new ApiException(HttpStatus.PRECONDITION_FAILED, "version_desactualizada",
                    "El producto cambió. Consulta la versión actual antes de reintentar.");
        }
    }

    private static ApiException productoNoDisponible() {
        return new ApiException(HttpStatus.NOT_FOUND, "producto_no_disponible", "El producto no está disponible.");
    }

    private static ProductoDto.Vista vista(Producto producto) {
        return new ProductoDto.Vista(producto.getUuid(), producto.getNombre(), producto.getCategoria(),
                producto.getUbicacion(), producto.getStock(), producto.getStockMinimo(),
                producto.isArchivado(), producto.getVersion());
    }

    private static ProductoDto.AjusteAplicado recibo(AjusteStock ajuste) {
        return new ProductoDto.AjusteAplicado(ajuste.getProductoUuid(), ajuste.getOperacionId(),
                ajuste.getDelta(), ajuste.getStockResultante(), ajuste.getVersionResultante());
    }
}
