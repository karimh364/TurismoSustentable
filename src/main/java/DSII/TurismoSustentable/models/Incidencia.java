package DSII.TurismoSustentable.models;

import javax.persistence.*;
import javax.validation.constraints.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "incidencia")
public class Incidencia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotNull(message = "El detalle del alquiler es obligatorio.")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "alquiler_detalle_id",
            nullable = false,
            updatable = false
    )
    private AlquilerDetalle alquilerDetalle;

    @NotNull(message = "El usuario que registra la incidencia es obligatorio.")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "registrado_por_id",
            nullable = false,
            updatable = false
    )
    private Usuario registradoPor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "informante_id")
    private Usuario informante;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resuelto_por_id")
    private Usuario resueltoPor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "orden_mantenimiento_id")
    private OrdenMantenimiento ordenMantenimiento;

    @NotBlank(message = "El tipo de incidencia es obligatorio.")
    @Pattern(
            regexp = "^(AVERIA|ACCIDENTE|PERDIDA|ROBO)$",
            message = "El tipo de incidencia no es válido."
    )
    @Column(nullable = false, length = 20)
    private String tipo;

    @NotBlank(message = "El estado de revisión es obligatorio.")
    @Pattern(
            regexp = "^(PENDIENTE|VALIDADA|DESCARTADA)$",
            message = "El estado de revisión no es válido."
    )
    @Column(name = "estado_revision", nullable = false, length = 20)
    private String estadoRevision = "PENDIENTE";

    @Column(name = "requiere_revision_economica", nullable = false)
    private boolean requiereRevisionEconomica;

    @NotBlank(message = "La descripción de la incidencia es obligatoria.")
    @Column(nullable = false, columnDefinition = "TEXT")
    private String descripcion;

    @Column(name = "lugar_reportado", columnDefinition = "TEXT")
    private String lugarReportado;

    @NotNull(message = "La fecha y hora informada del hecho es obligatoria.")
    @Column(name = "fecha_hora_hecho_informada", nullable = false)
    private LocalDateTime fechaHoraHechoInformada;

    @NotNull(message = "La fecha y hora de registro es obligatoria.")
    @Column(
            name = "fecha_hora_registro",
            nullable = false,
            updatable = false
    )
    private LocalDateTime fechaHoraRegistro = LocalDateTime.now();

    @Column(name = "fecha_hora_decision")
    private LocalDateTime fechaHoraDecision;

    @Column(name = "fecha_hora_corte_validado")
    private LocalDateTime fechaHoraCorteValidado;

    @Column(name = "motivo_decision", columnDefinition = "TEXT")
    private String motivoDecision;

    @Column(name = "fecha_hora_recepcion_fisica")
    private LocalDateTime fechaHoraRecepcionFisica;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "estacion_recepcion_id")
    private Estacion estacionRecepcion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recibido_por_id")
    private Usuario recibidoPor;

    @Column(name = "lugar_recepcion", columnDefinition = "TEXT")
    private String lugarRecepcion;

    @Version
    @Column(nullable = false)
    private Integer version;

    public Incidencia() {
    }

    public Integer getId() {
        return id;
    }

    public AlquilerDetalle getAlquilerDetalle() {
        return alquilerDetalle;
    }

    public Usuario getRegistradoPor() {
        return registradoPor;
    }

    public Usuario getInformante() {
        return informante;
    }

    public Usuario getResueltoPor() {
        return resueltoPor;
    }

    public OrdenMantenimiento getOrdenMantenimiento() {
        return ordenMantenimiento;
    }

    public String getTipo() {
        return tipo;
    }

    public String getEstadoRevision() {
        return estadoRevision;
    }

    public boolean isRequiereRevisionEconomica() {
        return requiereRevisionEconomica;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public String getLugarReportado() {
        return lugarReportado;
    }

    public LocalDateTime getFechaHoraHechoInformada() {
        return fechaHoraHechoInformada;
    }

    public LocalDateTime getFechaHoraRegistro() {
        return fechaHoraRegistro;
    }

    public LocalDateTime getFechaHoraDecision() {
        return fechaHoraDecision;
    }

    public LocalDateTime getFechaHoraCorteValidado() {
        return fechaHoraCorteValidado;
    }

    public String getMotivoDecision() {
        return motivoDecision;
    }

    public LocalDateTime getFechaHoraRecepcionFisica() {
        return fechaHoraRecepcionFisica;
    }

    public Estacion getEstacionRecepcion() {
        return estacionRecepcion;
    }

    public Usuario getRecibidoPor() {
        return recibidoPor;
    }

    public String getLugarRecepcion() {
        return lugarRecepcion;
    }

    public Integer getVersion() {
        return version;
    }

    public void setAlquilerDetalle(AlquilerDetalle alquilerDetalle) {
        this.alquilerDetalle = alquilerDetalle;
    }

    public void setRegistradoPor(Usuario registradoPor) {
        this.registradoPor = registradoPor;
    }

    public void setInformante(Usuario informante) {
        this.informante = informante;
    }

    public void setResueltoPor(Usuario resueltoPor) {
        this.resueltoPor = resueltoPor;
    }

    public void setOrdenMantenimiento(OrdenMantenimiento ordenMantenimiento) {
        this.ordenMantenimiento = ordenMantenimiento;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public void setEstadoRevision(String estadoRevision) {
        this.estadoRevision = estadoRevision;
    }

    public void setRequiereRevisionEconomica(boolean requiereRevisionEconomica) {
        this.requiereRevisionEconomica = requiereRevisionEconomica;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public void setLugarReportado(String lugarReportado) {
        this.lugarReportado = lugarReportado;
    }

    public void setFechaHoraHechoInformada(LocalDateTime fechaHoraHechoInformada) {
        this.fechaHoraHechoInformada = fechaHoraHechoInformada;
    }

    public void setFechaHoraDecision(LocalDateTime fechaHoraDecision) {
        this.fechaHoraDecision = fechaHoraDecision;
    }

    public void setFechaHoraCorteValidado(LocalDateTime fechaHoraCorteValidado) {
        this.fechaHoraCorteValidado = fechaHoraCorteValidado;
    }

    public void setMotivoDecision(String motivoDecision) {
        this.motivoDecision = motivoDecision;
    }

    public void setFechaHoraRecepcionFisica(LocalDateTime fechaHoraRecepcionFisica) {
        this.fechaHoraRecepcionFisica = fechaHoraRecepcionFisica;
    }

    public void setEstacionRecepcion(Estacion estacionRecepcion) {
        this.estacionRecepcion = estacionRecepcion;
    }

    public void setRecibidoPor(Usuario recibidoPor) {
        this.recibidoPor = recibidoPor;
    }

    public void setLugarRecepcion(String lugarRecepcion) {
        this.lugarRecepcion = lugarRecepcion;
    }
}
