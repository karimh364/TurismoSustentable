package DSII.TurismoSustentable.models;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Objects;

@Entity
@Table(name = "historial_estado_detalle")
public class HistorialEstadoDetalle {

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "estado_anterior_id", updatable = false)
    private EstadoAlquilerDetalle estadoAnterior;

    @NotNull(message = "El estado nuevo es obligatorio.")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "estado_nuevo_id",
            nullable = false,
            updatable = false
    )
    private EstadoAlquilerDetalle estadoNuevo;

    @NotNull(message = "La fecha del cambio es obligatoria.")
    @Column(
            name = "fecha_hora_cambio",
            nullable = false,
            updatable = false
    )
    private LocalDateTime fechaHoraCambio;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_usuario_id", updatable = false)
    private Usuario actorUsuario;

    @NotBlank(message = "El origen es obligatorio.")
    @Pattern(
            regexp = "^(USUARIO|SISTEMA)$",
            message = "El origen debe ser USUARIO o SISTEMA."
    )
    @Column(nullable = false, length = 20, updatable = false)
    private String origen;

    @Column(columnDefinition = "TEXT", updatable = false)
    private String observaciones;

    protected HistorialEstadoDetalle() {
    }

    public HistorialEstadoDetalle(
            AlquilerDetalle alquilerDetalle,
            EstadoAlquilerDetalle estadoAnterior,
            EstadoAlquilerDetalle estadoNuevo,
            Usuario actorUsuario,
            String origen,
            String observaciones
    ) {
        this.alquilerDetalle = Objects.requireNonNull(
                alquilerDetalle,
                "El detalle del alquiler es obligatorio."
        );

        this.estadoNuevo = Objects.requireNonNull(
                estadoNuevo,
                "El estado nuevo es obligatorio."
        );

        this.origen = origen == null
                ? null
                : origen.trim().toUpperCase(Locale.ROOT);

        if (!"USUARIO".equals(this.origen)
                && !"SISTEMA".equals(this.origen)) {
            throw new IllegalArgumentException(
                    "El origen debe ser USUARIO o SISTEMA."
            );
        }

        if (("USUARIO".equals(this.origen) && actorUsuario == null)
                || ("SISTEMA".equals(this.origen) && actorUsuario != null)) {
            throw new IllegalArgumentException(
                    "USUARIO requiere un actor; SISTEMA no debe tener un actor humano."
            );
        }

        if (estadoAnterior != null
                && Objects.equals(
                estadoAnterior.getCodigo(),
                estadoNuevo.getCodigo()
        )) {
            throw new IllegalArgumentException(
                    "Una transición debe cambiar realmente el estado."
            );
        }

        this.estadoAnterior = estadoAnterior;
        this.actorUsuario = actorUsuario;
        this.fechaHoraCambio = LocalDateTime.now();
        this.observaciones = observaciones == null
                ? null
                : observaciones.trim();
    }

    public Integer getId() {
        return id;
    }

    public AlquilerDetalle getAlquilerDetalle() {
        return alquilerDetalle;
    }

    public EstadoAlquilerDetalle getEstadoAnterior() {
        return estadoAnterior;
    }

    public EstadoAlquilerDetalle getEstadoNuevo() {
        return estadoNuevo;
    }

    public LocalDateTime getFechaHoraCambio() {
        return fechaHoraCambio;
    }

    public Usuario getActorUsuario() {
        return actorUsuario;
    }

    public String getOrigen() {
        return origen;
    }

    public String getObservaciones() {
        return observaciones;
    }
}