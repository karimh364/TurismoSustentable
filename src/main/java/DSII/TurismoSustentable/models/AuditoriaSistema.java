package DSII.TurismoSustentable.models;
import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "auditoria_sistema")
public class AuditoriaSistema {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "administrador_id", nullable = false)
    private Usuario administrador;
    @Column(name = "entidad_afectada", nullable = false, length = 80)
    private String entidadAfectada;
    @Column(name = "entidad_id", nullable = false)
    private Integer entidadId;
    @Column(nullable = false, length = 50)
    private String accion;
    @Column(name = "valores_anteriores", columnDefinition = "TEXT")
    private String valoresAnteriores;
    @Column(name = "valores_nuevos", columnDefinition = "TEXT")
    private String valoresNuevos;
    @Column(columnDefinition = "TEXT")
    private String motivo;
    @Column(name = "fecha_operacion", nullable = false)
    private LocalDateTime fechaOperacion;

    protected AuditoriaSistema() {}
    public AuditoriaSistema(Usuario admin, String entidad, Integer entidadId,
                            String accion, String anteriores, String nuevos, String motivo) {
        this.administrador = java.util.Objects.requireNonNull(admin);
        this.entidadAfectada = entidad;
        this.entidadId = entidadId;
        this.accion = accion;
        this.valoresAnteriores = anteriores;
        this.valoresNuevos = nuevos;
        this.motivo = motivo;
        this.fechaOperacion = LocalDateTime.now();
    }
    public Integer getId() { return id; }
    public Usuario getAdministrador() { return administrador; }
    public String getEntidadAfectada() { return entidadAfectada; }
    public Integer getEntidadId() { return entidadId; }
    public String getAccion() { return accion; }
    public String getValoresAnteriores() { return valoresAnteriores; }
    public String getValoresNuevos() { return valoresNuevos; }
    public String getMotivo() { return motivo; }
    public LocalDateTime getFechaOperacion() { return fechaOperacion; }
}
