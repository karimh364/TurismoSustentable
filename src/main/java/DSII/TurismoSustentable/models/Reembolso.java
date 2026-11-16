package DSII.TurismoSustentable.models;

import javax.persistence.*;
import javax.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Objects;

@Entity
@Table(
        name = "reembolso",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_reembolso_idempotencia",
                        columnNames = "clave_idempotencia"
                ),
                @UniqueConstraint(
                        name = "uk_reembolso_referencia_externa",
                        columnNames = {
                                "pago_id",
                                "referencia_externa"
                        }
                )
        }
)
public class Reembolso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "pago_id",
            nullable = false,
            updatable = false
    )
    private Pago pago;

    @NotNull
    @DecimalMin("0.01")
    @Digits(integer = 19, fraction = 2)
    @Column(
            name = "monto_ars",
            nullable = false,
            precision = 21,
            scale = 2,
            updatable = false
    )
    private BigDecimal montoArs;

    @NotBlank
    @Pattern(regexp = "^ARS$")
    @Column(nullable = false, length = 3, updatable = false)
    private String moneda = "ARS";

    @NotBlank
    @Pattern(
            regexp = "^(PENDIENTE|EN_PROCESO|CONFIRMADO|FALLIDO)$"
    )
    @Column(nullable = false, length = 20)
    private String estado = "PENDIENTE";

    @NotNull
    @Column(
            name = "fecha_creacion",
            nullable = false,
            updatable = false
    )
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_confirmacion")
    private LocalDateTime fechaConfirmacion;

    @NotBlank
    @Column(
            nullable = false,
            columnDefinition = "TEXT",
            updatable = false
    )
    private String motivo;

    @Size(max = 100)
    @Column(name = "referencia_externa", length = 100)
    private String referenciaExterna;

    @NotBlank
    @Size(max = 100)
    @Column(
            name = "clave_idempotencia",
            nullable = false,
            length = 100,
            updatable = false
    )
    private String claveIdempotencia;

    @Version
    @Column(nullable = false)
    private Integer version;

    protected Reembolso() {
    }

    public Reembolso(
            Pago pago,
            BigDecimal montoArs,
            LocalDateTime fechaCreacion,
            String motivo,
            String claveIdempotencia
    ) {
        this.pago = Objects.requireNonNull(pago);

        if (pago.getEstadoPago() == null
                || !"APROBADO".equals(pago.getEstadoPago().getCodigo())
                || pago.getFechaConfirmacion() == null) {
            throw new IllegalArgumentException(
                    "Solo puede reembolsarse un pago aprobado y confirmado."
            );
        }

        this.montoArs = Objects.requireNonNull(montoArs);
        this.fechaCreacion = Objects.requireNonNull(fechaCreacion);
        this.motivo = motivo == null ? null : motivo.trim();
        this.claveIdempotencia = claveIdempotencia == null
                ? null
                : claveIdempotencia.trim();
    }

    public Integer getId() {
        return id;
    }

    public Pago getPago() {
        return pago;
    }

    public BigDecimal getMontoArs() {
        return montoArs;
    }

    public String getMoneda() {
        return moneda;
    }

    public String getEstado() {
        return estado;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public LocalDateTime getFechaConfirmacion() {
        return fechaConfirmacion;
    }

    public String getMotivo() {
        return motivo;
    }

    public String getReferenciaExterna() {
        return referenciaExterna;
    }

    public String getClaveIdempotencia() {
        return claveIdempotencia;
    }

    public Integer getVersion() {
        return version;
    }

    public void setEstado(String estado) {
        this.estado = estado == null
                ? null
                : estado.trim().toUpperCase(Locale.ROOT);
    }

    public void setFechaConfirmacion(LocalDateTime fechaConfirmacion) {
        this.fechaConfirmacion = fechaConfirmacion;
    }

    public void setReferenciaExterna(String referenciaExterna) {
        this.referenciaExterna =
                referenciaExterna == null || referenciaExterna.isBlank()
                        ? null
                        : referenciaExterna.trim();
    }
}
