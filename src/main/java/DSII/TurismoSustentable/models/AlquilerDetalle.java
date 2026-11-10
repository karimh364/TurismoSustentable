package DSII.TurismoSustentable.models;

import javax.persistence.*;
import javax.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "alquiler_detalle",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_alquiler_detalle_bicicleta",
                columnNames = {"alquiler_id", "bicicleta_id"}
        )
)
public class AlquilerDetalle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotNull(message = "El alquiler es obligatorio.")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "alquiler_id",
            nullable = false,
            updatable = false
    )
    private Alquiler alquiler;

    @NotNull(message = "La bicicleta es obligatoria.")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "bicicleta_id",
            nullable = false,
            updatable = false
    )
    private Bicicleta bicicleta;

    @NotNull(message = "El tipo de bicicleta contratado es obligatorio.")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "tipo_bicicleta_contratado_id",
            nullable = false,
            updatable = false
    )
    private TipoBicicleta tipoBicicletaContratado;

    @NotNull(message = "El estado del detalle es obligatorio.")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "estado_alquiler_detalle_id",
            nullable = false
    )
    private EstadoAlquilerDetalle estado;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "estacion_devolucion_id")
    private Estacion estacionDevolucion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "orden_mantenimiento_id")
    private OrdenMantenimiento ordenMantenimiento;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "incidencia_corte_id", unique = true)
    private Incidencia incidenciaCorte;

    @NotNull(message = "La tarifa contratada es obligatoria.")
    @DecimalMin(
            value = "0.01",
            message = "La tarifa debe ser de al menos USD 0.01."
    )
    @Digits(integer = 8, fraction = 2)
    @Column(
            name = "precio_por_hora_aplicado_usd",
            nullable = false,
            updatable = false,
            precision = 10,
            scale = 2
    )
    private BigDecimal precioPorHoraAplicadoUsd;

    @NotNull(message = "La duración estimada es obligatoria.")
    @Positive(message = "La duración estimada debe ser mayor que cero.")
    @Column(
            name = "duracion_estimada_segundos",
            nullable = false,
            updatable = false
    )
    private Integer duracionEstimadaSegundos;

    @Column(name = "fecha_hora_retiro")
    private LocalDateTime fechaHoraRetiro;

    @Column(name = "fecha_hora_devolucion_estimada")
    private LocalDateTime fechaHoraDevolucionEstimada;

    @Column(name = "fecha_hora_devolucion_real")
    private LocalDateTime fechaHoraDevolucionReal;

    @Column(name = "fecha_hora_fin_uso")
    private LocalDateTime fechaHoraFinUso;

    @Column(name = "fecha_hora_cierre_detalle")
    private LocalDateTime fechaHoraCierreDetalle;

    @NotNull(message = "El subtotal estimado en USD es obligatorio.")
    @PositiveOrZero(message = "El subtotal no puede ser negativo.")
    @Digits(integer = 18, fraction = 2)
    @Column(
            name = "subtotal_estimado_usd",
            nullable = false,
            updatable = false,
            precision = 20,
            scale = 2
    )
    private BigDecimal subtotalEstimadoUsd;

    @NotNull(message = "El subtotal estimado en ARS es obligatorio.")
    @PositiveOrZero(message = "El subtotal no puede ser negativo.")
    @Digits(integer = 18, fraction = 2)
    @Column(
            name = "subtotal_estimado_ars",
            nullable = false,
            updatable = false,
            precision = 20,
            scale = 2
    )
    private BigDecimal subtotalEstimadoArs;

    @PositiveOrZero(message = "El subtotal no puede ser negativo.")
    @Digits(integer = 18, fraction = 2)
    @Column(
            name = "subtotal_real_usd",
            precision = 20,
            scale = 2
    )
    private BigDecimal subtotalRealUsd;

    @PositiveOrZero(message = "El subtotal no puede ser negativo.")
    @Digits(integer = 18, fraction = 2)
    @Column(
            name = "subtotal_real_ars",
            precision = 20,
            scale = 2
    )
    private BigDecimal subtotalRealArs;

    @Column(name = "falla_reportada", nullable = false)
    private boolean fallaReportada;

    @Column(name = "descripcion_falla", columnDefinition = "TEXT")
    private String descripcionFalla;

    @Column(name = "fecha_reporte_falla")
    private LocalDateTime fechaReporteFalla;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reportado_por_id")
    private Usuario reportadoPor;

    @Pattern(
            regexp = "^(CLIENTE|ADMIN|SISTEMA)$",
            message = "El origen del cierre no es válido."
    )
    @Column(name = "origen_cierre", length = 10)
    private String origenCierre;

    @Column(name = "motivo_cierre", columnDefinition = "TEXT")
    private String motivoCierre;

    @Version
    @Column(nullable = false)
    private Integer version;

    public AlquilerDetalle() {
    }

    public Integer getId() {
        return id;
    }

    public Alquiler getAlquiler() {
        return alquiler;
    }

    public Bicicleta getBicicleta() {
        return bicicleta;
    }

    public TipoBicicleta getTipoBicicletaContratado() {
        return tipoBicicletaContratado;
    }

    public EstadoAlquilerDetalle getEstado() {
        return estado;
    }

    public Estacion getEstacionDevolucion() {
        return estacionDevolucion;
    }

    public OrdenMantenimiento getOrdenMantenimiento() {
        return ordenMantenimiento;
    }

    public Incidencia getIncidenciaCorte() {
        return incidenciaCorte;
    }

    public BigDecimal getPrecioPorHoraAplicadoUsd() {
        return precioPorHoraAplicadoUsd;
    }

    public Integer getDuracionEstimadaSegundos() {
        return duracionEstimadaSegundos;
    }

    public LocalDateTime getFechaHoraRetiro() {
        return fechaHoraRetiro;
    }

    public LocalDateTime getFechaHoraDevolucionEstimada() {
        return fechaHoraDevolucionEstimada;
    }

    public LocalDateTime getFechaHoraDevolucionReal() {
        return fechaHoraDevolucionReal;
    }

    public LocalDateTime getFechaHoraFinUso() {
        return fechaHoraFinUso;
    }

    public LocalDateTime getFechaHoraCierreDetalle() {
        return fechaHoraCierreDetalle;
    }

    public BigDecimal getSubtotalEstimadoUsd() {
        return subtotalEstimadoUsd;
    }

    public BigDecimal getSubtotalEstimadoArs() {
        return subtotalEstimadoArs;
    }

    public BigDecimal getSubtotalRealUsd() {
        return subtotalRealUsd;
    }

    public BigDecimal getSubtotalRealArs() {
        return subtotalRealArs;
    }

    public boolean isFallaReportada() {
        return fallaReportada;
    }

    public String getDescripcionFalla() {
        return descripcionFalla;
    }

    public LocalDateTime getFechaReporteFalla() {
        return fechaReporteFalla;
    }

    public Usuario getReportadoPor() {
        return reportadoPor;
    }

    public String getOrigenCierre() {
        return origenCierre;
    }

    public String getMotivoCierre() {
        return motivoCierre;
    }

    public Integer getVersion() {
        return version;
    }

    public void setAlquiler(Alquiler alquiler) {
        this.alquiler = alquiler;
    }

    public void setBicicleta(Bicicleta bicicleta) {
        this.bicicleta = bicicleta;
    }

    public void setTipoBicicletaContratado(TipoBicicleta tipoBicicletaContratado) {
        this.tipoBicicletaContratado = tipoBicicletaContratado;
    }

    public void setEstado(EstadoAlquilerDetalle estado) {
        this.estado = estado;
    }

    public void setEstacionDevolucion(Estacion estacionDevolucion) {
        this.estacionDevolucion = estacionDevolucion;
    }

    public void setOrdenMantenimiento(OrdenMantenimiento ordenMantenimiento) {
        this.ordenMantenimiento = ordenMantenimiento;
    }

    public void setIncidenciaCorte(Incidencia incidenciaCorte) {
        this.incidenciaCorte = incidenciaCorte;
    }

    public void setPrecioPorHoraAplicadoUsd(BigDecimal precioPorHoraAplicadoUsd) {
        this.precioPorHoraAplicadoUsd = precioPorHoraAplicadoUsd;
    }

    public void setDuracionEstimadaSegundos(Integer duracionEstimadaSegundos) {
        this.duracionEstimadaSegundos = duracionEstimadaSegundos;
    }

    public void setFechaHoraRetiro(LocalDateTime fechaHoraRetiro) {
        this.fechaHoraRetiro = fechaHoraRetiro;
    }

    public void setFechaHoraDevolucionEstimada(LocalDateTime fechaHoraDevolucionEstimada) {
        this.fechaHoraDevolucionEstimada = fechaHoraDevolucionEstimada;
    }

    public void setFechaHoraDevolucionReal(LocalDateTime fechaHoraDevolucionReal) {
        this.fechaHoraDevolucionReal = fechaHoraDevolucionReal;
    }

    public void setFechaHoraFinUso(LocalDateTime fechaHoraFinUso) {
        this.fechaHoraFinUso = fechaHoraFinUso;
    }

    public void setFechaHoraCierreDetalle(LocalDateTime fechaHoraCierreDetalle) {
        this.fechaHoraCierreDetalle = fechaHoraCierreDetalle;
    }

    public void setSubtotalEstimadoUsd(BigDecimal subtotalEstimadoUsd) {
        this.subtotalEstimadoUsd = subtotalEstimadoUsd;
    }

    public void setSubtotalEstimadoArs(BigDecimal subtotalEstimadoArs) {
        this.subtotalEstimadoArs = subtotalEstimadoArs;
    }

    public void setSubtotalRealUsd(BigDecimal subtotalRealUsd) {
        this.subtotalRealUsd = subtotalRealUsd;
    }

    public void setSubtotalRealArs(BigDecimal subtotalRealArs) {
        this.subtotalRealArs = subtotalRealArs;
    }

    public void setFallaReportada(boolean fallaReportada) {
        this.fallaReportada = fallaReportada;
    }

    public void setDescripcionFalla(String descripcionFalla) {
        this.descripcionFalla = descripcionFalla;
    }

    public void setFechaReporteFalla(LocalDateTime fechaReporteFalla) {
        this.fechaReporteFalla = fechaReporteFalla;
    }

    public void setReportadoPor(Usuario reportadoPor) {
        this.reportadoPor = reportadoPor;
    }

    public void setOrigenCierre(String origenCierre) {
        this.origenCierre = origenCierre;
    }

    public void setMotivoCierre(String motivoCierre) {
        this.motivoCierre = motivoCierre;
    }
}