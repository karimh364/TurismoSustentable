package DSII.TurismoSustentable.models;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Objects;

@Entity
@Table(name = "historial_incidencia")
public class HistorialIncidencia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotNull(message = "La incidencia es obligatoria.")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "incidencia_id",
            nullable = false,
            updatable = false
    )
    private Incidencia incidencia;

    @NotBlank(message = "La acción es obligatoria.")
    @Size(
            max = 50,
            message = "La acción no puede superar los 50 caracteres."
    )
    @Column(nullable = false, length = 50, updatable = false)
    private String accion;

    @Pattern(
            regexp = "^(PENDIENTE|VALIDADA|DESCARTADA)$",
            message = "El estado anterior no es válido."
    )
    @Column(name = "estado_anterior", length = 20, updatable = false)
    private String estadoAnterior;

    @Pattern(
            regexp = "^(PENDIENTE|VALIDADA|DESCARTADA)$",
            message = "El estado nuevo no es válido."
    )
    @Column(name = "estado_nuevo", length = 20, updatable = false)
    private String estadoNuevo;

    @NotNull(message = "La fecha del evento es obligatoria.")
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

    @Column(
            name = "valores_anteriores",
            columnDefinition = "TEXT",
            updatable = false
    )
    private String valoresAnteriores;

    @Column(
            name = "valores_nuevos",
            columnDefinition = "TEXT",
            updatable = false
    )
    private String valoresNuevos;

    @Column(columnDefinition = "TEXT", updatable = false)
    private String observaciones;

    protected HistorialIncidencia() {
    }

    public HistorialIncidencia(
            Incidencia incidencia,
            String accion,
            String estadoAnterior,
            String estadoNuevo,
            Usuario actorUsuario,
            String origen,
            String observaciones,
            String valoresAnteriores,
            String valoresNuevos
    ) {
        this.incidencia = Objects.requireNonNull(
                incidencia,
                "La incidencia es obligatoria."
        );

        this.accion = normalizarCodigo(accion);
        this.estadoAnterior = normalizarCodigo(estadoAnterior);
        this.estadoNuevo = normalizarCodigo(estadoNuevo);
        this.origen = normalizarCodigo(origen);

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

        if (this.estadoAnterior != null && this.estadoNuevo == null) {
            throw new IllegalArgumentException(
                    "Un estado anterior requiere un estado nuevo."
            );
        }

        if (this.estadoAnterior != null
                && Objects.equals(this.estadoAnterior, this.estadoNuevo)) {
            throw new IllegalArgumentException(
                    "Una transición debe cambiar realmente el estado."
            );
        }

        this.actorUsuario = actorUsuario;
        this.fechaHoraCambio = LocalDateTime.now();
        this.observaciones = observaciones == null
                ? null
                : observaciones.trim();

        this.valoresAnteriores = valoresAnteriores;
        this.valoresNuevos = valoresNuevos;
    }

    private static String normalizarCodigo(String valor) {
        return valor == null
                ? null
                : valor.trim().toUpperCase(Locale.ROOT);
    }

    public Integer getId() {
        return id;
    }

    public Incidencia getIncidencia() {
        return incidencia;
    }

    public String getAccion() {
        return accion;
    }

    public String getEstadoAnterior() {
        return estadoAnterior;
    }

    public String getEstadoNuevo() {
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

    public String getValoresAnteriores() {
        return valoresAnteriores;
    }

    public String getValoresNuevos() {
        return valoresNuevos;
    }

    public String getObservaciones() {
        return observaciones;
    }
}
