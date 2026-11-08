package DSII.TurismoSustentable.dto;

import DSII.TurismoSustentable.models.Bicicleta;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class BicicletaDTO {

    private int id;
    private String codigoBicicleta;

    private String estado;

    private String ubicacion;

    private String tipo;

    private BigDecimal precio;
    private Integer tipoBicicletaId;
    private Integer estacionId;
    private Integer version;
    private LocalDate fechaAlta;
    private boolean borradoBici;

    private List<AlquilerDTO> alquileres;

    public BicicletaDTO() {
    }

    public BicicletaDTO(Bicicleta bicicleta) {
        this.id = bicicleta.getId();
        this.codigoBicicleta = bicicleta.getCodigoBicicleta();
        this.estado = bicicleta.getEstado().getCodigo();
        this.tipo = bicicleta.getTipo().getCodigo();
        this.precio = bicicleta.getTipo().getPrecioPorHoraUsd();
        this.tipoBicicletaId = bicicleta.getTipo().getId();
        this.estacionId = bicicleta.getUbicacionActual() == null
                ? null : bicicleta.getUbicacionActual().getId();
        this.version = bicicleta.getVersion();
        this.fechaAlta = bicicleta.getFechaAlta();
        this.borradoBici = bicicleta.isBorradoBici();

        if (bicicleta.getUbicacionActual() != null) {
            this.ubicacion = bicicleta.getUbicacionActual().getNombre();
        } else {
            this.ubicacion = "Sin Asignar";
        }

        //ALQUILERES
        /*this.alquileres = bicicleta.getAlquileres().stream().map(alquiler ->
                new AlquilerDTO(alquiler)).collect(Collectors.toList());*/
    }

    public int getId() {
        return id;
    }

    public Integer getTipoBicicletaId() { return tipoBicicletaId; }
    public Integer getEstacionId() { return estacionId; }
    public Integer getVersion() { return version; }
    public LocalDate getFechaAlta() { return fechaAlta; }
    public boolean isBorradoBici() { return borradoBici; }

    public String getCodigoBicicleta() {
        return codigoBicicleta;
    }

    public String getEstado() {
        return estado;
    }

    public String getUbicacion() {
        return ubicacion;
    }
    public String getTipo() {
        return tipo;
    }

    public BigDecimal getPrecio() {
        return precio;
    }

    public List<AlquilerDTO>getAlquileres(){
        return alquileres;
    }
}
