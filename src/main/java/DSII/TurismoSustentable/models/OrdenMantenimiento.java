package DSII.TurismoSustentable.models;

import javax.persistence.*;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Digits;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Locale;

@Entity
@Table(name = "orden_mantenimiento")
public class OrdenMantenimiento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotNull(message = "La bicicleta es obligatoria.")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "bicicleta_id", nullable = false)
    private Bicicleta bicicleta;

    @NotNull(message = "El tipo de mantenimiento es obligatorio.")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tipo_mantenimiento_id", nullable = false)
    private TipoMantenimiento tipoMantenimiento;

    @NotNull(message = "El estado de la orden es obligatorio.")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "estado_orden_mantenimiento_id", nullable = false)
    private EstadoOrdenMantenimiento estado;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tecnico_id")
    private Usuario tecnico;

    @NotNull(message = "La fecha de creación es obligatoria.")
    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion = LocalDateTime.now();

    @Column(name = "fecha_inicio")
    private LocalDateTime fechaInicio;

    @Column(name = "fecha_fin")
    private LocalDateTime fechaFin;

    @Pattern(
            regexp = "^(APTA|NO_APTA)$",
            message = "El resultado técnico debe ser APTA o NO_APTA."
    )
    @Column(name = "resultado_tecnico", length = 10)
    private String resultadoTecnico;

    @DecimalMin(
            value = "0.00",
            message = "El costo de repuestos no puede ser negativo."
    )
    @Digits(
            integer = 10,
            fraction = 2,
            message = "El costo admite hasta 10 dígitos enteros y 2 decimales."
    )
    @Column(name = "costo_repuestos", precision = 12, scale = 2)
    private BigDecimal costoRepuestos;

    @Pattern(
            regexp = "^ARS$",
            message = "La moneda de los repuestos debe ser ARS."
    )
    @Column(name = "moneda_repuestos", length = 3)
    private String monedaRepuestos;

    @Column(columnDefinition = "TEXT")
    private String diagnostico;

    @Column(name = "descripcion_trabajo", columnDefinition = "TEXT")
    private String descripcionTrabajo;

    @Column(columnDefinition = "TEXT")
    private String observaciones;

    @Column(name = "motivo_pausa_cancelacion", columnDefinition = "TEXT")
    private String motivoPausaCancelacion;

    @Version
    @Column(nullable = false)
    private Integer version;

    public OrdenMantenimiento() {
    }

    public OrdenMantenimiento(
            Bicicleta bicicleta,
            TipoMantenimiento tipoMantenimiento,
            EstadoOrdenMantenimiento estado
    ) {
        this.bicicleta = bicicleta;
        this.tipoMantenimiento = tipoMantenimiento;
        this.estado = estado;
    }

    public Integer getId() {
        return id;
    }

    public Bicicleta getBicicleta() {
        return bicicleta;
    }

    public void setBicicleta(Bicicleta bicicleta) {
        this.bicicleta = bicicleta;
    }

    public TipoMantenimiento getTipoMantenimiento() {
        return tipoMantenimiento;
    }

    public void setTipoMantenimiento(TipoMantenimiento tipoMantenimiento) {
        this.tipoMantenimiento = tipoMantenimiento;
    }

    public EstadoOrdenMantenimiento getEstado() {
        return estado;
    }

    public void setEstado(EstadoOrdenMantenimiento estado) {
        this.estado = estado;
    }

    public Usuario getTecnico() {
        return tecnico;
    }

    public void setTecnico(Usuario tecnico) {
        this.tecnico = tecnico;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public LocalDateTime getFechaInicio() {
        return fechaInicio;
    }

    public void setFechaInicio(LocalDateTime fechaInicio) {
        this.fechaInicio = fechaInicio;
    }

    public LocalDateTime getFechaFin() {
        return fechaFin;
    }

    public void setFechaFin(LocalDateTime fechaFin) {
        this.fechaFin = fechaFin;
    }

    public String getResultadoTecnico() {
        return resultadoTecnico;
    }

    public void setResultadoTecnico(String resultadoTecnico) {
        this.resultadoTecnico = resultadoTecnico == null
                ? null
                : resultadoTecnico.trim().toUpperCase(Locale.ROOT);
    }

    public BigDecimal getCostoRepuestos() {
        return costoRepuestos;
    }

    public void setCostoRepuestos(BigDecimal costoRepuestos) {
        this.costoRepuestos = costoRepuestos;
    }

    public String getMonedaRepuestos() {
        return monedaRepuestos;
    }

    public void setMonedaRepuestos(String monedaRepuestos) {
        this.monedaRepuestos = monedaRepuestos == null
                ? null
                : monedaRepuestos.trim().toUpperCase(Locale.ROOT);
    }

    public String getDiagnostico() {
        return diagnostico;
    }

    public void setDiagnostico(String diagnostico) {
        this.diagnostico = diagnostico == null ? null : diagnostico.trim();
    }

    public String getDescripcionTrabajo() {
        return descripcionTrabajo;
    }

    public void setDescripcionTrabajo(String descripcionTrabajo) {
        this.descripcionTrabajo = descripcionTrabajo == null
                ? null
                : descripcionTrabajo.trim();
    }

    public String getObservaciones() {
        return observaciones;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones == null
                ? null
                : observaciones.trim();
    }

    public String getMotivoPausaCancelacion() {
        return motivoPausaCancelacion;
    }

    public void setMotivoPausaCancelacion(String motivoPausaCancelacion) {
        this.motivoPausaCancelacion = motivoPausaCancelacion == null
                ? null
                : motivoPausaCancelacion.trim();
    }

    public Integer getVersion() {
        return version;
    }
}