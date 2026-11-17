package cl.duoc.appkaiju.inventario.producto.repository;

import cl.duoc.appkaiju.inventario.producto.entity.Producto;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface ProductoRepository extends JpaRepository<Producto, UUID> {
    Page<Producto> findByArchivadoFalse(Pageable pageable);

    Optional<Producto> findByUuidAndArchivadoFalse(UUID uuid);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Producto p where p.uuid = :uuid")
    Optional<Producto> bloquearPorUuid(UUID uuid);

    @Modifying
    @Query(value = """
            INSERT INTO productos (uuid, nombre, categoria, ubicacion, stock, stock_minimo, solicitud_alta)
            VALUES (:uuid, :nombre, :categoria, :ubicacion, :stock, :stockMinimo, :solicitud)
            ON CONFLICT (uuid) DO NOTHING
            """, nativeQuery = true)
    int insertarSiNoExiste(UUID uuid, String nombre, String categoria, String ubicacion,
                          int stock, int stockMinimo, String solicitud);
}
