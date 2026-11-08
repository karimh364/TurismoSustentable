package DSII.TurismoSustentable.models;
import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "historial_estado_bicicleta")
public class HistorialEstadoBicicleta {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "bicicleta_id", nullable = false)
    private Bicicleta bicicleta;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "estado_anterior_id")
    private EstadoBicicleta estadoAnterior;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "estado_nuevo_id", nullable = false)
    private EstadoBicicleta estadoNuevo;
    @Column(name = "fecha_hora_cambio", nullable = false)
    private LocalDateTime fechaHoraCambio;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_usuario_id")
    private Usuario actorUsuario;
    @Column(nullable = false, length = 20)
    private String origen;
    @Column(columnDefinition = "TEXT")
    private String observaciones;

    protected HistorialEstadoBicicleta() {}
    public HistorialEstadoBicicleta(Bicicleta bicicleta, EstadoBicicleta anterior,
                                    EstadoBicicleta nuevo, Usuario actor, String observaciones) {
        this.bicicleta = bicicleta;
        this.estadoAnterior = anterior;
        this.estadoNuevo = nuevo;
        this.actorUsuario = java.util.Objects.requireNonNull(actor);
        this.origen = "USUARIO";
        this.fechaHoraCambio = LocalDateTime.now();
        this.observaciones = observaciones;
    }
    public Integer getId() { return id; }
    public Bicicleta getBicicleta() { return bicicleta; }
    public EstadoBicicleta getEstadoAnterior() { return estadoAnterior; }
    public EstadoBicicleta getEstadoNuevo() { return estadoNuevo; }
    public LocalDateTime getFechaHoraCambio() { return fechaHoraCambio; }
    public Usuario getActorUsuario() { return actorUsuario; }
    public String getOrigen() { return origen; }
    public String getObservaciones() { return observaciones; }
}
