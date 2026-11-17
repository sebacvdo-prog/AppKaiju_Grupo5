package cl.duoc.appkaiju.inventario.producto.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.util.UUID;

@Entity
@Table(name = "productos")
public class Producto {
    @Id
    private UUID uuid;
    private String nombre;
    private String categoria;
    private String ubicacion;
    private int stock;
    private int stockMinimo;
    private boolean archivado;
    @Version
    private long version;
    @Column(updatable = false)
    private String solicitudAlta;

    protected Producto() {}

    public UUID getUuid() { return uuid; }
    public String getNombre() { return nombre; }
    public String getCategoria() { return categoria; }
    public String getUbicacion() { return ubicacion; }
    public int getStock() { return stock; }
    public int getStockMinimo() { return stockMinimo; }
    public boolean isArchivado() { return archivado; }
    public long getVersion() { return version; }
    public String getSolicitudAlta() { return solicitudAlta; }

    public void editar(String nombre, String categoria, String ubicacion, int stock, int stockMinimo) {
        this.nombre = nombre;
        this.categoria = categoria;
        this.ubicacion = ubicacion;
        this.stock = stock;
        this.stockMinimo = stockMinimo;
    }

    public void archivar() {
        archivado = true;
    }

    public void actualizarStock(int stock) {
        this.stock = stock;
    }
}
