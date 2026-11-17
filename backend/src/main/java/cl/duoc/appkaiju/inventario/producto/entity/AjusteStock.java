package cl.duoc.appkaiju.inventario.producto.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "ajustes_stock")
public class AjusteStock {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private UUID productoUuid;
    private UUID operacionId;
    private int delta;
    private int stockResultante;
    private long versionResultante;

    protected AjusteStock() {}

    public AjusteStock(UUID productoUuid, UUID operacionId, int delta, int stockResultante, long versionResultante) {
        this.productoUuid = productoUuid;
        this.operacionId = operacionId;
        this.delta = delta;
        this.stockResultante = stockResultante;
        this.versionResultante = versionResultante;
    }

    public UUID getProductoUuid() { return productoUuid; }
    public UUID getOperacionId() { return operacionId; }
    public int getDelta() { return delta; }
    public int getStockResultante() { return stockResultante; }
    public long getVersionResultante() { return versionResultante; }
}
