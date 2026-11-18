package DSII.TurismoSustentable.models;

import DSII.TurismoSustentable.time.FechaAlquilerLocalConverter;

import javax.persistence.*;
import javax.validation.constraints.*;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Objects;

@Entity
@Table(name = "historial_estado_reembolso")
public class HistorialEstadoReembolso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "reembolso_id",
            nullable = false,
            updatable = false
    )
    private Reembolso reembolso;

    @Pattern(regexp = "^(PENDIENTE|EN_PROCESO|CONFIRMADO|FALLIDO)$")
    @Column(name = "estado_anterior", length = 20, updatable = false)
    private String estadoAnterior;

    @NotBlank
    @Pattern(regexp = "^(PENDIENTE|EN_PROCESO|CONFIRMADO|FALLIDO)$")
    @Column(
            name = "estado_nuevo",
            nullable = false,
            length = 20,
            updatable = false
    )
    private String estadoNuevo;

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

    protected HistorialEstadoReembolso() {
    }

    public HistorialEstadoReembolso(
            Reembolso reembolso,
            String estadoAnterior,
            String estadoNuevo,
            LocalDateTime fechaHoraCambio,
            LocalDateTime fechaHoraEventoExterno,
            Usuario actorUsuario,
            String origen,
            String fuente,
            String referenciaEventoExterno,
            String observaciones
    ) {
        this.reembolso = Objects.requireNonNull(reembolso);
        this.estadoAnterior = normalizarCodigo(estadoAnterior);
        this.estadoNuevo = normalizarCodigo(estadoNuevo);
        this.fechaHoraCambio = Objects.requireNonNull(fechaHoraCambio);
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
                    "USUARIO requiere un actor; SISTEMA no debe tener actor humano."
            );
        }

        if (this.estadoAnterior == null
                && !"PENDIENTE".equals(this.estadoNuevo)) {
            throw new IllegalArgumentException(
                    "El registro inicial del reembolso debe indicar PENDIENTE."
            );
        }

        if (this.estadoAnterior != null
                && this.estadoAnterior.equals(this.estadoNuevo)) {
            throw new IllegalArgumentException(
                    "La transición debe cambiar realmente el estado."
            );
        }

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

    private static String normalizarCodigo(String texto) {
        String limpio = limpiar(texto);
        return limpio == null ? null : limpio.toUpperCase(Locale.ROOT);
    }

    public Integer getId() {
        return id;
    }

    public Reembolso getReembolso() {
        return reembolso;
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
