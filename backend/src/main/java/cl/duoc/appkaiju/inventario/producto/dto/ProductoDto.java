package cl.duoc.appkaiju.inventario.producto.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;

import java.util.List;
import java.util.UUID;

public final class ProductoDto {
    private static final String TEXTO_POSTGRESQL = "[^\\x00]*";

    private ProductoDto() {}

    public record Alta(
            @NotNull UUID uuid,
            @NotBlank @Pattern(regexp = TEXTO_POSTGRESQL) String nombre,
            @NotBlank @Pattern(regexp = TEXTO_POSTGRESQL) String categoria,
            @NotBlank @Pattern(regexp = TEXTO_POSTGRESQL) String ubicacion,
            @NotNull @PositiveOrZero Integer stock,
            @NotNull @PositiveOrZero Integer stockMinimo
    ) {
        public Edicion datos() {
            return new Edicion(nombre, categoria, ubicacion, stock, stockMinimo);
        }
    }

    public record Edicion(
            @NotBlank @Pattern(regexp = TEXTO_POSTGRESQL) String nombre,
            @NotBlank @Pattern(regexp = TEXTO_POSTGRESQL) String categoria,
            @NotBlank @Pattern(regexp = TEXTO_POSTGRESQL) String ubicacion,
            @NotNull @PositiveOrZero Integer stock,
            @NotNull @PositiveOrZero Integer stockMinimo
    ) {
        public Edicion normalizada() {
            return new Edicion(nombre.strip(), categoria.strip(), ubicacion.strip(), stock, stockMinimo);
        }
    }

    public record Vista(
            UUID uuid, String nombre, String categoria, String ubicacion,
            int stock, int stockMinimo, boolean archivado, long version
    ) {}

    public record Pagina(List<Vista> productos, int pagina, int tamano, long totalProductos, int totalPaginas) {}

    public record Ajuste(@NotNull UUID operacionId, @NotNull Integer delta) {}

    public record AjusteAplicado(UUID uuid, UUID operacionId, int delta, int stock, long version) {}
}
