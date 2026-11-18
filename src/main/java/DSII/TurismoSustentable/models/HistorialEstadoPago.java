package DSII.TurismoSustentable.models;

import DSII.TurismoSustentable.time.FechaAlquilerLocalConverter;

import javax.persistence.*;
import javax.validation.constraints.*;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Objects;

@Entity
@Table(name = "historial_estado_pago")
public class HistorialEstadoPago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pago_id", nullable = false, updatable = false)
    private Pago pago;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "estado_anterior_id", updatable = false)
    private EstadoPago estadoAnterior;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "estado_nuevo_id",
            nullable = false,
            updatable = false
    )
    private EstadoPago estadoNuevo;

    @NotNull
    @Column(
            name = "fecha_hora_cambio",
            nullable = false,
            updatable = false
    )
    @Convert(converter = FechaAlquilerLocalConverter.class)
    private LocalDateTime fechaHoraCambio;

    @Column(name = "fecha_hora_evento_externo", updatable = false)
    @Convert(converter = FechaAlquilerLocalConverter.class)
    private LocalDateTime fechaHoraEventoExterno;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_usuario_id", updatable = false)
    private Usuario actorUsuario;

    @NotBlank
    @Pattern(regexp = "^(USUARIO|SISTEMA)$")
    @Column(nullable = false, length = 20, updatable = false)
    private String origen;

    @NotBlank
    @Size(max = 50)
    @Column(nullable = false, length = 50, updatable = false)
    private String fuente;

    @Size(max = 100)
    @Column(
            name = "referencia_evento_externo",
            length = 100,
            updatable = false
    )
    private String referenciaEventoExterno;

    @Column(columnDefinition = "TEXT", updatable = false)
    private String observaciones;

    protected HistorialEstadoPago() {
    }

    public HistorialEstadoPago(
            Pago pago,
            EstadoPago estadoAnterior,
            EstadoPago estadoNuevo,
            LocalDateTime fechaHoraCambio,
            LocalDateTime fechaHoraEventoExterno,
            Usuario actorUsuario,
            String origen,
            String fuente,
            String referenciaEventoExterno,
            String observaciones
    ) {
        this.pago = Objects.requireNonNull(pago);
        this.estadoNuevo = Objects.requireNonNull(estadoNuevo);
        this.fechaHoraCambio = Objects.requireNonNull(fechaHoraCambio);

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
                    "USUARIO requiere un actor; SISTEMA no debe tener actor humano."
            );
        }

        if (estadoAnterior == null
                && !"CREADO".equals(estadoNuevo.getCodigo())) {
            throw new IllegalArgumentException(
                    "El registro inicial del pago debe indicar CREADO."
            );
        }

        if (estadoAnterior != null
                && Objects.equals(
                estadoAnterior.getCodigo(),
                estadoNuevo.getCodigo()
        )) {
            throw new IllegalArgumentException(
                    "La transición debe cambiar realmente el estado."
            );
        }

        this.estadoAnterior = estadoAnterior;
        this.fechaHoraEventoExterno = fechaHoraEventoExterno;
        this.actorUsuario = actorUsuario;
        this.fuente = limpiar(fuente);
        this.referenciaEventoExterno = limpiar(referenciaEventoExterno);
        this.observaciones = limpiar(observaciones);
    }

    private static String limpiar(String texto) {
        return texto == null || texto.isBlank()
                ? null
                : texto.trim();
    }

    public Integer getId() {
        return id;
    }

    public Pago getPago() {
        return pago;
    }

    public EstadoPago getEstadoAnterior() {
        return estadoAnterior;
    }

    public EstadoPago getEstadoNuevo() {
        return estadoNuevo;
    }

    public LocalDateTime getFechaHoraCambio() {
        return fechaHoraCambio;
    }

    public LocalDateTime getFechaHoraEventoExterno() {
        return fechaHoraEventoExterno;
    }

    public Usuario getActorUsuario() {
        return actorUsuario;
    }

    public String getOrigen() {
        return origen;
    }

    public String getFuente() {
        return fuente;
    }

    public String getReferenciaEventoExterno() {
        return referenciaEventoExterno;
    }

    public String getObservaciones() {
        return observaciones;
    }
}
