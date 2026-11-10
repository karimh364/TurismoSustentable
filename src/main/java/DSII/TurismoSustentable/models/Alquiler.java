package DSII.TurismoSustentable.models;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Digits;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import javax.validation.constraints.PositiveOrZero;

@Entity
public class Alquiler {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    // Relaciones para el análisis de estadísticas (Género del Usuario, Tipo de Bicicleta)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false) // Aseguramos que el usuario no sea nulo
    private Usuario usuario;

    // Legado: se conserva hasta migrar sus consumidores.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bicicleta_id")
    private Bicicleta bicicleta;

    // --- CAMPOS DE UBICACIÓN Y TIEMPO ---

    private LocalDateTime fechaInicio;
    private LocalDateTime fechaFin;
    private BigDecimal costoTotal;
    private BigDecimal costoTotalArs;

    @Column(precision = 10, scale = 2)
    private BigDecimal precioPorHoraAplicado;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "estacion_retiro_id")
    @NotNull(message = "La ubicación de retiro es obligatoria.")
    private Estacion ubicacionRetiro;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "estacion_devolucion_id")
    private Estacion ubicacionDevolucion;

    // ------------------------------------

    @Enumerated(EnumType.STRING)
    private CodigoEstadoAlquiler estado;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "estado_alquiler_id")
    private EstadoAlquiler estadoContratacion;

    private boolean borradoAlquiler = false;

    @Version
    @Column(nullable = false)
    private Integer version;

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
            precision = 20,
            scale = 8,
            updatable = false
    )
    private BigDecimal cotizacionArsPorUsd;

    @Size(
            max = 100,
            message = "La fuente de cotización no puede superar los 100 caracteres."
    )
    @Pattern(
            regexp = "(?s).*\\S.*",
            message = "La fuente de cotización no puede estar vacía."
    )
    @Column(
            name = "fuente_cotizacion",
            length = 100,
            updatable = false
    )
    private String fuenteCotizacion;

    @Column(
            name = "fecha_hora_aplicacion_cotizacion",
            updatable = false
    )
    private LocalDateTime fechaHoraAplicacionCotizacion;

    @Column(
            name = "fecha_hora_actualizacion_fuente",
            updatable = false
    )
    private LocalDateTime fechaHoraActualizacionFuente;

    @Column(
            name = "fecha_hora_creacion",
            updatable = false
    )
    private LocalDateTime fechaHoraCreacion;

    @Column(
            name = "fecha_hora_vencimiento_reserva",
            updatable = false
    )
    private LocalDateTime fechaHoraVencimientoReserva;

    @Column(name = "fecha_hora_cierre")
    private LocalDateTime fechaHoraCierre;

    @Column(name = "fecha_hora_cancelacion")
    private LocalDateTime fechaHoraCancelacion;

    @Column(name = "fecha_hora_liquidacion")
    private LocalDateTime fechaHoraLiquidacion;

    @PositiveOrZero(message = "El costo estimado no puede ser negativo.")
    @Digits(
            integer = 19,
            fraction = 2,
            message = "El importe admite hasta 19 dígitos enteros y 2 decimales."
    )
    @Column(
            name = "costo_estimado_usd",
            precision = 21,
            scale = 2,
            updatable = false
    )
    private BigDecimal costoEstimadoUsd;

    @PositiveOrZero(message = "El costo estimado no puede ser negativo.")
    @Digits(
            integer = 19,
            fraction = 2,
            message = "El importe admite hasta 19 dígitos enteros y 2 decimales."
    )
    @Column(
            name = "costo_estimado_ars",
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

    // Constructor simple para iniciar un alquiler
    public Alquiler(
            Usuario usuario,
            Bicicleta bicicleta,
            Estacion ubicacionRetiro
    ) {
        this(
                usuario,
                bicicleta,
                LocalDateTime.now(),
                null,
                BigDecimal.ZERO,
                CodigoEstadoAlquiler.INICIADO,
                ubicacionRetiro,
                null
        );
    }

    // Constructor completo
    public Alquiler(
            Usuario usuario,
            Bicicleta bicicleta,
            LocalDateTime fechaInicio,
            LocalDateTime fechaFin,
            BigDecimal costoTotal,
            CodigoEstadoAlquiler estado,
            Estacion ubicacionRetiro,
            Estacion ubicacionDevolucion
    ) {
        this.usuario = usuario;
        this.bicicleta = bicicleta;
        this.fechaInicio = fechaInicio;
        this.fechaFin = fechaFin;
        this.costoTotal = costoTotal;
        this.estado = estado;
        this.ubicacionRetiro = ubicacionRetiro;
        this.ubicacionDevolucion = ubicacionDevolucion;
        this.costoTotalArs = null;
        this.borradoAlquiler = false;
    }


    // GETTERS Y SETTERS
    public int getId() {
        return id;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public Bicicleta getBicicleta() {
        return bicicleta;
    }

    public LocalDateTime getFechaInicio() {
        return fechaInicio;
    }

    public LocalDateTime getFechaFin() {
        return fechaFin;
    }

    public BigDecimal getCostoTotal() {
        return costoTotal;
    }

    public BigDecimal getCostoTotalArs() {
        return costoTotalArs;
    }

    public BigDecimal getPrecioPorHoraAplicado() {
        return precioPorHoraAplicado;
    }

    public CodigoEstadoAlquiler getEstado() {
        return estado;
    }

    public Estacion getUbicacionRetiro() {
        return ubicacionRetiro;
    }

    public Estacion getUbicacionDevolucion() {
        return ubicacionDevolucion;
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

    public void setBicicleta(Bicicleta bicicleta) {
        this.bicicleta = bicicleta;
    }

    public void setFechaInicio(LocalDateTime fechaInicio) {
        this.fechaInicio = fechaInicio;
    }

    public void setFechaFin(LocalDateTime fechaFin) {
        this.fechaFin = fechaFin;
    }

    public void setCostoTotal(BigDecimal costoTotal) {
        this.costoTotal = costoTotal;
    }

    public void setCostoTotalArs(BigDecimal costoTotalArs) {
        this.costoTotalArs = costoTotalArs;
    }

    public void setPrecioPorHoraAplicado(
            BigDecimal precioPorHoraAplicado
    ) {
        this.precioPorHoraAplicado =
                precioPorHoraAplicado;
    }

    public void setEstado(CodigoEstadoAlquiler estado) {
        this.estado = estado;
    }

    public void setUbicacionRetiro(Estacion ubicacionRetiro) {
        this.ubicacionRetiro = ubicacionRetiro;
    }

    public void setUbicacionDevolucion(Estacion ubicacionDevolucion) {
        this.ubicacionDevolucion = ubicacionDevolucion;
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
