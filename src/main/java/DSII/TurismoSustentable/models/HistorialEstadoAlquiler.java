package DSII.TurismoSustentable.models;

import DSII.TurismoSustentable.time.FechaAlquilerLocalConverter;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Objects;

/** Evento de cabecera: no reemplaza el historial de cada bicicleta del alquiler. */
@Entity
@Table(name = "historial_estado_alquiler", indexes = {
        @Index(name = "ix_hist_alquiler_orden", columnList = "alquiler_id,id")
})
public class HistorialEstadoAlquiler {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "alquiler_id", nullable = false, updatable = false,
            foreignKey = @ForeignKey(name = "fk_hist_alquiler_cabecera"))
    private Alquiler alquiler;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "estado_anterior_id", updatable = false,
            foreignKey = @ForeignKey(name = "fk_hist_alquiler_anterior"))
    private EstadoAlquiler estadoAnterior;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "estado_nuevo_id", nullable = false, updatable = false,
            foreignKey = @ForeignKey(name = "fk_hist_alquiler_nuevo"))
    private EstadoAlquiler estadoNuevo;

    @NotNull
    @Column(name = "fecha_hora_cambio", nullable = false, updatable = false)
    @Convert(converter = FechaAlquilerLocalConverter.class)
    private LocalDateTime fechaHoraCambio;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_usuario_id", updatable = false,
            foreignKey = @ForeignKey(name = "fk_hist_alquiler_actor"))
    private Usuario actorUsuario;

    @NotBlank
    @Pattern(regexp = "^(USUARIO|SISTEMA)$")
    @Column(nullable = false, length = 20, updatable = false)
    private String origen;

    @Column(columnDefinition = "TEXT", updatable = false)
    private String observaciones;

    protected HistorialEstadoAlquiler() { }

    public HistorialEstadoAlquiler(Alquiler alquiler, EstadoAlquiler estadoAnterior,
            EstadoAlquiler estadoNuevo, Usuario actorUsuario, String origen,
            String observaciones, LocalDateTime fechaHoraCambio) {
        this.alquiler = Objects.requireNonNull(alquiler, "El alquiler es obligatorio.");
        this.estadoNuevo = Objects.requireNonNull(estadoNuevo, "El estado nuevo es obligatorio.");
        this.fechaHoraCambio = Objects.requireNonNull(fechaHoraCambio, "La fecha del cambio es obligatoria.");
        this.origen = origen == null ? null : origen.trim().toUpperCase(Locale.ROOT);
        if (!"USUARIO".equals(this.origen) && !"SISTEMA".equals(this.origen)) {
            throw new IllegalArgumentException("El origen debe ser USUARIO o SISTEMA.");
        }
        if (("USUARIO".equals(this.origen) && actorUsuario == null)
                || ("SISTEMA".equals(this.origen) && actorUsuario != null)) {
            throw new IllegalArgumentException("USUARIO requiere un actor; SISTEMA no debe tener un actor humano.");
        }
        if (estadoAnterior != null && Objects.equals(estadoAnterior.getCodigo(), estadoNuevo.getCodigo())) {
            throw new IllegalArgumentException("Una transición debe cambiar realmente el estado.");
        }
        if (estadoAnterior == null && !"RESERVADO".equals(estadoNuevo.getCodigo())) {
            throw new IllegalArgumentException("El estado inicial del alquiler debe ser RESERVADO.");
        }
        this.estadoAnterior = estadoAnterior;
        this.actorUsuario = actorUsuario;
        this.observaciones = observaciones == null || observaciones.trim().isEmpty()
                ? null : observaciones.trim();
    }

    public Integer getId() { return id; }
    public Alquiler getAlquiler() { return alquiler; }
    public EstadoAlquiler getEstadoAnterior() { return estadoAnterior; }
    public EstadoAlquiler getEstadoNuevo() { return estadoNuevo; }
    public LocalDateTime getFechaHoraCambio() { return fechaHoraCambio; }
    public Usuario getActorUsuario() { return actorUsuario; }
    public String getOrigen() { return origen; }
    public String getObservaciones() { return observaciones; }
}
