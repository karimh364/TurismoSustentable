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
@Table(name = "historial_orden_mantenimiento")
public class HistorialOrdenMantenimiento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotNull(message = "La orden de mantenimiento es obligatoria.")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "orden_mantenimiento_id",
            nullable = false,
            updatable = false
    )
    private OrdenMantenimiento ordenMantenimiento;

    @NotBlank(message = "La acción es obligatoria.")
    @Size(
            max = 50,
            message = "La acción no puede superar los 50 caracteres."
    )
    @Column(nullable = false, length = 50, updatable = false)
    private String accion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "estado_anterior_id", updatable = false)
    private EstadoOrdenMantenimiento estadoAnterior;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "estado_nuevo_id", updatable = false)
    private EstadoOrdenMantenimiento estadoNuevo;

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

    @Column(columnDefinition = "TEXT", updatable = false)
    private String observaciones;

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

    protected HistorialOrdenMantenimiento() {
    }

    public HistorialOrdenMantenimiento(
            OrdenMantenimiento ordenMantenimiento,
            String accion,
            EstadoOrdenMantenimiento estadoAnterior,
            EstadoOrdenMantenimiento estadoNuevo,
            Usuario actorUsuario,
            String origen,
            String observaciones,
            String valoresAnteriores,
            String valoresNuevos
    ) {
        this.ordenMantenimiento = Objects.requireNonNull(
                ordenMantenimiento,
                "La orden de mantenimiento es obligatoria."
        );

        this.accion = normalizarCodigo(accion);
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

        if (estadoAnterior != null && estadoNuevo == null) {
            throw new IllegalArgumentException(
                    "Un estado anterior requiere un estado nuevo."
            );
        }

        if (estadoAnterior != null && estadoNuevo != null
                && Objects.equals(
                estadoAnterior.getCodigo(),
                estadoNuevo.getCodigo()
        )) {
            throw new IllegalArgumentException(
                    "Una transición debe cambiar realmente el estado."
            );
        }

        this.estadoAnterior = estadoAnterior;
        this.estadoNuevo = estadoNuevo;
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

    public OrdenMantenimiento getOrdenMantenimiento() {
        return ordenMantenimiento;
    }

    public String getAccion() {
        return accion;
    }

    public EstadoOrdenMantenimiento getEstadoAnterior() {
        return estadoAnterior;
    }

    public EstadoOrdenMantenimiento getEstadoNuevo() {
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

    public String getValoresAnteriores() {
        return valoresAnteriores;
    }

    public String getValoresNuevos() {
        return valoresNuevos;
    }
}