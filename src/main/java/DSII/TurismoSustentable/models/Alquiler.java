package DSII.TurismoSustentable.models;

import DSII.TurismoSustentable.time.FechaAlquilerLocalConverter;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Digits;
import javax.validation.constraints.Size;
import javax.validation.constraints.PositiveOrZero;

@Entity
public class Alquiler {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    // Cabecera del alquiler. Las bicicletas se vinculan mediante sus detalles.
    @NotNull(message = "El usuario del alquiler es obligatorio.")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @NotNull(message = "La ubicación de retiro es obligatoria.")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "estacion_retiro_id", nullable = false)
    private Estacion ubicacionRetiro;

    @NotNull(message = "El estado del alquiler es obligatorio.")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "estado_alquiler_id", nullable = false)
    private EstadoAlquiler estadoContratacion;

    private boolean borradoAlquiler = false;

    @Version
    @Column(nullable = false)
    private Integer version;

    @NotNull(message = "La cotización aplicada es obligatoria.")
    @DecimalMin(
            value = "0",
            inclusive = false,
            message = "La cotización debe ser mayor que cero."
    )
    @Digits(
            integer = 12,
            fraction = 8,
            message = "La cotización admite hasta 12 dígitos enteros y 8 decimales."
    )
    @Column(
            name = "cotizacion_ars_por_usd",
            nullable = false,
            precision = 20,
            scale = 8,
            updatable = false
    )
    private BigDecimal cotizacionArsPorUsd;

    @Size(
            max = 100,
            message = "La fuente de cotización no puede superar los 100 caracteres."
    )
    @NotBlank(message = "La fuente de cotización es obligatoria.")
    @Column(
            name = "fuente_cotizacion",
            nullable = false,
            length = 100,
            updatable = false
    )
    private String fuenteCotizacion;

    @NotNull(message = "La fecha de aplicación de la cotización es obligatoria.")
    @Column(
            name = "fecha_hora_aplicacion_cotizacion",
            nullable = false,
            updatable = false
    )
    @Convert(converter = FechaAlquilerLocalConverter.class)
    private LocalDateTime fechaHoraAplicacionCotizacion;

    @Column(
            name = "fecha_hora_actualizacion_fuente",
            updatable = false
    )
    @Convert(converter = FechaAlquilerLocalConverter.class)
    private LocalDateTime fechaHoraActualizacionFuente;

    @NotNull(message = "La fecha de creación del alquiler es obligatoria.")
    @Column(
            name = "fecha_hora_creacion",
            nullable = false,
            updatable = false
    )
    @Convert(converter = FechaAlquilerLocalConverter.class)
    private LocalDateTime fechaHoraCreacion;

    @NotNull(message = "El vencimiento de la reserva es obligatorio.")
    @Column(
            name = "fecha_hora_vencimiento_reserva",
            nullable = false,
            updatable = false
    )
    @Convert(converter = FechaAlquilerLocalConverter.class)
    private LocalDateTime fechaHoraVencimientoReserva;

    @Column(name = "fecha_hora_cierre")
    @Convert(converter = FechaAlquilerLocalConverter.class)
    private LocalDateTime fechaHoraCierre;

    @Column(name = "fecha_hora_cancelacion")
    @Convert(converter = FechaAlquilerLocalConverter.class)
    private LocalDateTime fechaHoraCancelacion;

    @Column(name = "fecha_hora_liquidacion")
    @Convert(converter = FechaAlquilerLocalConverter.class)
    private LocalDateTime fechaHoraLiquidacion;

    @NotNull(message = "El costo estimado en USD es obligatorio.")
    @PositiveOrZero(message = "El costo estimado no puede ser negativo.")
    @Digits(
            integer = 19,
            fraction = 2,
            message = "El importe admite hasta 19 dígitos enteros y 2 decimales."
    )
    @Column(
            name = "costo_estimado_usd",
            nullable = false,
            precision = 21,
            scale = 2,
            updatable = false
    )
    private BigDecimal costoEstimadoUsd;

    @NotNull(message = "El costo estimado en ARS es obligatorio.")
    @PositiveOrZero(message = "El costo estimado no puede ser negativo.")
    @Digits(
            integer = 19,
            fraction = 2,
            message = "El importe admite hasta 19 dígitos enteros y 2 decimales."
    )
    @Column(
            name = "costo_estimado_ars",
            nullable = false,
            precision = 21,
            scale = 2,
            updatable = false
    )
    private BigDecimal costoEstimadoArs;

    @PositiveOrZero(message = "El costo final no puede ser negativo.")
    @Digits(
            integer = 19,
            fraction = 2,
            message = "El importe admite hasta 19 dígitos enteros y 2 decimales."
    )
    @Column(
            name = "costo_final_usd",
            precision = 21,
            scale = 2
    )
    private BigDecimal costoFinalUsd;

    @PositiveOrZero(message = "El costo final no puede ser negativo.")
    @Digits(
            integer = 19,
            fraction = 2,
            message = "El importe admite hasta 19 dígitos enteros y 2 decimales."
    )
    @Column(
            name = "costo_final_ars",
            precision = 21,
            scale = 2
    )
    private BigDecimal costoFinalArs;

    // CONSTRUCTORES

    public Alquiler() {
    }


    // GETTERS Y SETTERS
    public int getId() {
        return id;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public Estacion getUbicacionRetiro() {
        return ubicacionRetiro;
    }

    public boolean isBorradoAlquiler() {
        return borradoAlquiler;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public void setUbicacionRetiro(Estacion ubicacionRetiro) {
        this.ubicacionRetiro = ubicacionRetiro;
    }

    public void setBorradoAlquiler(boolean borradoAlquiler) {
        this.borradoAlquiler = borradoAlquiler;
    }

    public Integer getVersion() {
        return version;
    }

    public BigDecimal getCotizacionArsPorUsd() {
        return cotizacionArsPorUsd;
    }

    public void setCotizacionArsPorUsd(BigDecimal cotizacionArsPorUsd) {
        this.cotizacionArsPorUsd = cotizacionArsPorUsd;
    }

    public String getFuenteCotizacion() {
        return fuenteCotizacion;
    }

    public void setFuenteCotizacion(String fuenteCotizacion) {
        this.fuenteCotizacion = fuenteCotizacion;
    }

    public LocalDateTime getFechaHoraAplicacionCotizacion() {
        return fechaHoraAplicacionCotizacion;
    }

    public void setFechaHoraAplicacionCotizacion(LocalDateTime fechaHoraAplicacionCotizacion) {
        this.fechaHoraAplicacionCotizacion = fechaHoraAplicacionCotizacion;
    }

    public LocalDateTime getFechaHoraActualizacionFuente() {
        return fechaHoraActualizacionFuente;
    }

    public void setFechaHoraActualizacionFuente(LocalDateTime fechaHoraActualizacionFuente) {
        this.fechaHoraActualizacionFuente = fechaHoraActualizacionFuente;
    }

    public LocalDateTime getFechaHoraCreacion() {
        return fechaHoraCreacion;
    }

    public void setFechaHoraCreacion(LocalDateTime fechaHoraCreacion) {
        this.fechaHoraCreacion = fechaHoraCreacion;
    }

    public LocalDateTime getFechaHoraVencimientoReserva() {
        return fechaHoraVencimientoReserva;
    }

    public void setFechaHoraVencimientoReserva(LocalDateTime fechaHoraVencimientoReserva) {
        this.fechaHoraVencimientoReserva = fechaHoraVencimientoReserva;
    }

    public LocalDateTime getFechaHoraCierre() {
        return fechaHoraCierre;
    }

    public void setFechaHoraCierre(LocalDateTime fechaHoraCierre) {
        this.fechaHoraCierre = fechaHoraCierre;
    }

    public LocalDateTime getFechaHoraCancelacion() {
        return fechaHoraCancelacion;
    }

    public void setFechaHoraCancelacion(LocalDateTime fechaHoraCancelacion) {
        this.fechaHoraCancelacion = fechaHoraCancelacion;
    }

    public LocalDateTime getFechaHoraLiquidacion() {
        return fechaHoraLiquidacion;
    }

    public void setFechaHoraLiquidacion(LocalDateTime fechaHoraLiquidacion) {
        this.fechaHoraLiquidacion = fechaHoraLiquidacion;
    }

    public BigDecimal getCostoEstimadoUsd() {
        return costoEstimadoUsd;
    }

    public void setCostoEstimadoUsd(BigDecimal costoEstimadoUsd) {
        this.costoEstimadoUsd = costoEstimadoUsd;
    }

    public BigDecimal getCostoEstimadoArs() {
        return costoEstimadoArs;
    }

    public void setCostoEstimadoArs(BigDecimal costoEstimadoArs) {
        this.costoEstimadoArs = costoEstimadoArs;
    }

    public BigDecimal getCostoFinalUsd() {
        return costoFinalUsd;
    }

    public void setCostoFinalUsd(BigDecimal costoFinalUsd) {
        this.costoFinalUsd = costoFinalUsd;
    }

    public BigDecimal getCostoFinalArs() {
        return costoFinalArs;
    }

    public void setCostoFinalArs(BigDecimal costoFinalArs) {
        this.costoFinalArs = costoFinalArs;
    }

    public EstadoAlquiler getEstadoContratacion() {
        return estadoContratacion;
    }

    public void setEstadoContratacion(EstadoAlquiler estadoContratacion) {
        this.estadoContratacion = estadoContratacion;
    }

}
