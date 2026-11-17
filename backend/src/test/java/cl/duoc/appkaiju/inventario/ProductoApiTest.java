package cl.duoc.appkaiju.inventario;

import cl.duoc.appkaiju.inventario.producto.dto.ProductoDto;
import com.fasterxml.jackson.databind.JsonNode;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;

import javax.sql.DataSource;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(ProductoApiTest.PostgresDePruebas.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class ProductoApiTest {
    private static final String RUTA = "/api/v1/productos";

    @Autowired
    private TestRestTemplate http;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void iniciaPostgresYAplicaFlyway() {
        assertThat(jdbc.queryForObject("SHOW server_version", String.class)).startsWith("17.");
        assertThat(jdbc.queryForObject("""
                SELECT success FROM flyway_schema_history WHERE version = '1'
                """, Boolean.class)).isTrue();
    }

    @Test
    void registraYConsultaUnProducto() {
        UUID uuid = UUID.randomUUID();
        var alta = enviar(HttpMethod.POST, RUTA, alta(uuid, 12), null);
        assertThat(alta.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(alta.getHeaders().getLocation()).hasToString(RUTA + "/" + uuid);
        assertThat(alta.getHeaders().getETag()).isEqualTo("\"0\"");
        assertThat(alta.getBody().path("nombre").asText()).isEqualTo("Casco de seguridad");
        assertThat(alta.getBody().path("categoria").asText()).isEqualTo("Protección");
        assertThat(alta.getBody().path("ubicacion").asText()).isEqualTo("Estante A-01");
        assertThat(alta.getBody().path("stockMinimo").asInt()).isEqualTo(2);
        assertThat(alta.getBody().path("archivado").asBoolean()).isFalse();

        var consulta = enviar(HttpMethod.GET, RUTA + "/" + uuid, null, null);
        assertThat(consulta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(consulta.getHeaders().getETag()).isEqualTo(alta.getHeaders().getETag());
        assertThat(consulta.getBody()).isEqualTo(alta.getBody());
    }

    @Test
    void consultaUnListadoPaginado() {
        UUID uuid = crear(4);
        var listado = enviar(HttpMethod.GET, RUTA + "?tamano=100", null, null);
        var primeraPagina = enviar(HttpMethod.GET, RUTA + "?pagina=0&tamano=1", null, null);

        assertThat(listado.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(listado.getBody().path("productos")).anyMatch(p -> p.path("uuid").asText().equals(uuid.toString()));
        assertThat(primeraPagina.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(primeraPagina.getBody().path("productos").size()).isEqualTo(1);
        assertThat(primeraPagina.getBody().path("productos").get(0)).isEqualTo(listado.getBody().path("productos").get(0));
        assertThat(primeraPagina.getBody().path("totalProductos")).isEqualTo(listado.getBody().path("totalProductos"));
    }

    @Test
    void reintentaAltasConcurrentesYConservaElReciboOriginal() throws Exception {
        UUID uuid = UUID.randomUUID();
        Supplier<ResponseEntity<JsonNode>> solicitud = () -> enviar(HttpMethod.POST, RUTA, alta(uuid, 3), null);
        var respuestas = concurrentes(solicitud, solicitud);
        assertThat(respuestas).allMatch(r -> r.getStatusCode() == HttpStatus.CREATED);
        assertThat(respuestas.get(0).getBody()).isEqualTo(respuestas.get(1).getBody());
        assertThat(ajustar(uuid, UUID.randomUUID(), 2).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(solicitud.get().getBody()).isEqualTo(respuestas.getFirst().getBody());
        assertThat(enviar(HttpMethod.POST, RUTA, alta(uuid, 50), null).getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(consultar(uuid).path("stock").asInt()).isEqualTo(5);
    }

    @Test
    void editaProductoYRechazaUnaVersionAnterior() {
        UUID uuid = crear(3);
        var cambios = new ProductoDto.Edicion("Linterna", "Herramientas", "Bodega", 0, 4);
        var modificacion = enviar(HttpMethod.PUT, RUTA + "/" + uuid, cambios, "\"0\"");

        assertThat(modificacion.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(modificacion.getHeaders().getETag()).isEqualTo("\"1\"");
        assertThat(modificacion.getBody().path("nombre").asText()).isEqualTo("Linterna");
        assertThat(modificacion.getBody().path("stock").asInt()).isZero();
        assertThat(modificacion.getBody().path("stockMinimo").asInt()).isEqualTo(4);
        var atrasada = enviar(HttpMethod.PUT, RUTA + "/" + uuid, cambios, "\"0\"");
        assertThat(atrasada.getStatusCode()).isEqualTo(HttpStatus.PRECONDITION_FAILED);
        assertThat(atrasada.getBody().path("codigo").asText()).isEqualTo("version_desactualizada");
        assertThat(consultar(uuid)).isEqualTo(modificacion.getBody());
    }

    @Test
    void aceptaSoloUnaDeDosEdicionesConcurrentesConElMismoETag() throws Exception {
        UUID uuid = crear(3);
        var primero = new ProductoDto.Edicion("Linterna", "Herramientas", "Bodega", 4, 2);
        var segundo = new ProductoDto.Edicion("Taladro", "Herramientas", "Bodega", 5, 2);
        var respuestas = concurrentes(
                () -> enviar(HttpMethod.PUT, RUTA + "/" + uuid, primero, "\"0\""),
                () -> enviar(HttpMethod.PUT, RUTA + "/" + uuid, segundo, "\"0\""));
        assertThat(respuestas).extracting(ResponseEntity::getStatusCode)
                .containsExactlyInAnyOrder(HttpStatus.OK, HttpStatus.PRECONDITION_FAILED);
        assertThat(consultar(uuid).path("version").asLong()).isEqualTo(1);
    }

    @Test
    void archivaSinBorrarElProductoNiSuStock() {
        UUID uuid = crear(8);
        assertThat(enviar(HttpMethod.DELETE, RUTA + "/" + uuid, null, "\"0\"").getStatusCode())
                .isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(enviar(HttpMethod.GET, RUTA + "/" + uuid, null, null).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(ajustar(uuid, UUID.randomUUID(), 1).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        var activos = enviar(HttpMethod.GET, RUTA + "?tamano=100", null, null).getBody().path("productos");
        var todos = enviar(HttpMethod.GET, RUTA + "?tamano=100&incluirArchivados=true", null, null)
                .getBody().path("productos");
        assertThat(activos).noneMatch(p -> p.path("uuid").asText().equals(uuid.toString()));
        assertThat(todos).anyMatch(p -> p.path("uuid").asText().equals(uuid.toString())
                && p.path("archivado").asBoolean() && p.path("stock").asInt() == 8 && p.path("version").asLong() == 1);
    }

    @Test
    void ajustaStockYReenviaElReciboSinAplicarElDeltaOtraVez() {
        UUID uuid = crear(5);
        UUID operacion = UUID.randomUUID();
        var incremento = ajustar(uuid, operacion, 2);
        assertThat(incremento.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(incremento.getHeaders().getETag()).isEqualTo("\"1\"");
        assertThat(incremento.getBody().path("operacionId").asText()).isEqualTo(operacion.toString());
        assertThat(incremento.getBody().path("stock").asInt()).isEqualTo(7);
        assertThat(ajustar(uuid, UUID.randomUUID(), -3).getBody().path("stock").asInt()).isEqualTo(4);
        assertThat(ajustar(uuid, operacion, 2).getBody()).isEqualTo(incremento.getBody());
        assertThat(ajustar(uuid, operacion, 3).getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(consultar(uuid).path("stock").asInt()).isEqualTo(4);
    }

    @Test
    void serializaAjustesConcurrentesYDeduplicaReenvios() throws Exception {
        UUID uuid = crear(0);
        UUID operacion = UUID.randomUUID();
        var distintos = concurrentes(() -> ajustar(uuid, operacion, 2), () -> ajustar(uuid, UUID.randomUUID(), 3));
        assertThat(distintos).allMatch(r -> r.getStatusCode() == HttpStatus.OK);
        var duplicados = concurrentes(() -> ajustar(uuid, operacion, 2), () -> ajustar(uuid, operacion, 2));
        assertThat(duplicados).allMatch(r -> r.getStatusCode() == HttpStatus.OK);
        assertThat(duplicados.get(0).getBody()).isEqualTo(duplicados.get(1).getBody());
        assertThat(consultar(uuid).path("stock").asInt()).isEqualTo(5);
        assertThat(consultar(uuid).path("version").asInt()).isEqualTo(2);
    }

    @Test
    void rechazaDeltasCeroYStocksFueraDeRango() {
        UUID vacio = crear(0);
        UUID lleno = crear(Integer.MAX_VALUE);
        assertThat(ajustar(vacio, UUID.randomUUID(), 0).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(ajustar(vacio, UUID.randomUUID(), -1).getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(ajustar(lleno, UUID.randomUUID(), 1).getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(consultar(vacio).path("stock").asInt()).isZero();
        assertThat(consultar(lleno).path("stock").asInt()).isEqualTo(Integer.MAX_VALUE);
    }

    @Test
    void identificaLosCamposInvalidos() {
        var invalido = new ProductoDto.Alta(null, " ", "", " ", -1, -1);
        var respuesta = enviar(HttpMethod.POST, RUTA, invalido, null);
        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(respuesta.getHeaders().getContentType().isCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)).isTrue();
        assertThat(respuesta.getBody().path("codigo").asText()).isEqualTo("validacion");
        var campos = respuesta.getBody().path("campos");
        for (String campo : List.of("uuid", "nombre", "categoria", "ubicacion", "stock", "stockMinimo")) {
            assertThat(campos.path(campo).asText()).isNotBlank();
        }

        var nombreConNulo = new ProductoDto.Alta(UUID.randomUUID(), "A" + (char) 0 + "B", "Categoría", "A-01", 1, 1);
        var textoInvalido = enviar(HttpMethod.POST, RUTA, nombreConNulo, null);
        assertThat(textoInvalido.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(textoInvalido.getBody().path("campos").has("nombre")).isTrue();
    }

    @Test
    void rechazaJsonMalformadoYValoresQueNoSonEnterosValidos() {
        UUID uuid = crear(0);
        String ruta = RUTA + "/" + uuid + "/ajustes-stock";
        String base = "{\"operacionId\":\"" + UUID.randomUUID() + "\",\"delta\":";
        for (String invalido : List.of("{", base + "1.5}", base + "\"1\"}", base + "2147483648}")) {
            var respuesta = enviar(HttpMethod.POST, ruta, invalido, null);
            assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(respuesta.getBody().path("codigo").asText()).isEqualTo("json_invalido");
        }
        var sinCampos = enviar(HttpMethod.POST, ruta, "{}", null);
        assertThat(sinCampos.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(sinCampos.getBody().path("campos").has("delta")).isTrue();
        assertThat(consultar(uuid).path("stock").asInt()).isZero();
    }

    @Test
    void devuelveErroresHttpParaRecursosParametrosYPrecondiciones() {
        UUID uuid = crear(1);
        var cambios = new ProductoDto.Edicion("Linterna", "Herramientas", "Bodega", 0, 4);
        assertThat(enviar(HttpMethod.GET, RUTA + "/" + UUID.randomUUID(), null, null).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(enviar(HttpMethod.GET, RUTA + "/invalido", null, null).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(enviar(HttpMethod.GET, RUTA + "?tamano=101", null, null).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(enviar(HttpMethod.PUT, RUTA + "/" + uuid, cambios, null).getStatusCode())
                .isEqualTo(HttpStatus.PRECONDITION_REQUIRED);
        assertThat(enviar(HttpMethod.DELETE, RUTA + "/" + uuid, null, "0").getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(enviar(HttpMethod.DELETE, RUTA + "/" + uuid, null, "\"1\"").getStatusCode())
                .isEqualTo(HttpStatus.PRECONDITION_FAILED);
        assertThat(consultar(uuid).path("stock").asInt()).isEqualTo(1);
    }

    private UUID crear(int stock) {
        UUID uuid = UUID.randomUUID();
        assertThat(enviar(HttpMethod.POST, RUTA, alta(uuid, stock), null).getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return uuid;
    }

    private JsonNode consultar(UUID uuid) {
        var respuesta = enviar(HttpMethod.GET, RUTA + "/" + uuid, null, null);
        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        return respuesta.getBody();
    }

    private ResponseEntity<JsonNode> ajustar(UUID uuid, UUID operacionId, int delta) {
        return enviar(HttpMethod.POST, RUTA + "/" + uuid + "/ajustes-stock", new ProductoDto.Ajuste(operacionId, delta), null);
    }

    private ResponseEntity<JsonNode> enviar(HttpMethod metodo, String ruta, Object cuerpo, String ifMatch) {
        var headers = new HttpHeaders();
        if (cuerpo != null) {
            headers.setContentType(MediaType.APPLICATION_JSON);
        }
        if (ifMatch != null) {
            headers.set(HttpHeaders.IF_MATCH, ifMatch);
        }
        return http.exchange(ruta, metodo, new HttpEntity<>(cuerpo, headers), JsonNode.class);
    }

    private static ProductoDto.Alta alta(UUID uuid, int stock) {
        return new ProductoDto.Alta(uuid, " Casco de seguridad ", "Protección", "Estante A-01", stock, 2);
    }

    private static List<ResponseEntity<JsonNode>> concurrentes(
            Supplier<ResponseEntity<JsonNode>> primera, Supplier<ResponseEntity<JsonNode>> segunda) throws Exception {
        try (var executor = Executors.newFixedThreadPool(2)) {
            var inicio = new CountDownLatch(1);
            var a = executor.submit(() -> { inicio.await(); return primera.get(); });
            var b = executor.submit(() -> { inicio.await(); return segunda.get(); });
            inicio.countDown();
            return List.of(a.get(10, TimeUnit.SECONDS), b.get(10, TimeUnit.SECONDS));
        }
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class PostgresDePruebas {
        @Bean(destroyMethod = "close")
        EmbeddedPostgres postgres() throws IOException {
            return EmbeddedPostgres.builder().start();
        }

        @Bean
        DataSource dataSource(EmbeddedPostgres postgres) {
            return postgres.getPostgresDatabase();
        }
    }
}
