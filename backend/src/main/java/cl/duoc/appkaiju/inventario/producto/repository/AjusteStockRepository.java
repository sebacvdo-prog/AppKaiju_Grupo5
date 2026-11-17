package cl.duoc.appkaiju.inventario.producto.repository;

import cl.duoc.appkaiju.inventario.producto.entity.AjusteStock;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AjusteStockRepository extends JpaRepository<AjusteStock, Long> {
    Optional<AjusteStock> findByProductoUuidAndOperacionId(UUID productoUuid, UUID operacionId);
}
