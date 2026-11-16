package DSII.TurismoSustentable.models;

import javax.persistence.*;
import javax.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Objects;

@Entity
@Table(
        name = "pago",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_pago_idempotencia",
                        columnNames = "clave_idempotencia"
                ),
                @UniqueConstraint(
                        name = "uk_pago_referencia_externa",
                        columnNames = {
                                "plataforma",
                                "entorno",
                                "referencia_externa"
                        }
                )
        }
)
public class Pago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "alquiler_id",
            nullable = false,
            updatable = false
    )
    private Alquiler alquiler;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "estado_pago_id", nullable = false)
    private EstadoPago estadoPago;

    @NotBlank
    @Pattern(regexp = "^(ANTICIPO|ADICIONAL)$")
    @Column(nullable = false, length = 20, updatable = false)
    private String finalidad;

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
    @Pattern(regexp = "^(SIMULADA|MERCADOPAGO)$")
    @Column(nullable = false, length = 20, updatable = false)
    private String plataforma;

    @NotBlank
    @Pattern(regexp = "^(PRUEBA|PRODUCCION)$")
    @Column(nullable = false, length = 10, updatable = false)
    private String entorno;

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

    protected Pago() {
    }

    public Pago(
            Alquiler alquiler,
            EstadoPago estadoInicial,
            String finalidad,
            BigDecimal montoArs,
            LocalDateTime fechaCreacion,
            String plataforma,
            String entorno,
            String claveIdempotencia
    ) {
        this.alquiler = Objects.requireNonNull(alquiler);
        this.estadoPago = Objects.requireNonNull(estadoInicial);

        if (!"CREADO".equals(estadoInicial.getCodigo())) {
            throw new IllegalArgumentException(
                    "Un pago nuevo debe comenzar en CREADO."
            );
        }

        this.finalidad = normalizarCodigo(finalidad);
        this.montoArs = Objects.requireNonNull(montoArs);
        this.fechaCreacion = Objects.requireNonNull(fechaCreacion);
        this.plataforma = normalizarCodigo(plataforma);
        this.entorno = normalizarCodigo(entorno);
        this.claveIdempotencia = claveIdempotencia == null
                ? null
                : claveIdempotencia.trim();

        if ("SIMULADA".equals(this.plataforma)
                && !"PRUEBA".equals(this.entorno)) {
            throw new IllegalArgumentException(
                    "La plataforma simulada solo admite el entorno PRUEBA."
            );
        }
    }

    private static String normalizarCodigo(String valor) {
        return valor == null
                ? null
                : valor.trim().toUpperCase(Locale.ROOT);
    }

    public Integer getId() {
        return id;
    }

    public Alquiler getAlquiler() {
        return alquiler;
    }

    public EstadoPago getEstadoPago() {
        return estadoPago;
    }

    public String getFinalidad() {
        return finalidad;
    }

    public BigDecimal getMontoArs() {
        return montoArs;
    }

    public String getMoneda() {
        return moneda;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public LocalDateTime getFechaConfirmacion() {
        return fechaConfirmacion;
    }

    public String getPlataforma() {
        return plataforma;
    }

    public String getEntorno() {
        return entorno;
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

    public void setEstadoPago(EstadoPago estadoPago) {
        this.estadoPago = Objects.requireNonNull(estadoPago);
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
