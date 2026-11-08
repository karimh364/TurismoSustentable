package DSII.TurismoSustentable.models;

import javax.persistence.*;
import javax.validation.constraints.*;
import java.math.BigDecimal;
import java.util.Locale;

@Entity
@Table(name = "tipo_bicicleta")
public class TipoBicicleta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotBlank(message = "El código del tipo de bicicleta es obligatorio.")
    @Size(
            max = 20,
            message = "El código no puede superar los 20 caracteres."
    )
    @Column(nullable = false, unique = true, length = 20)
    private String codigo;

    @NotNull(message = "El precio por hora es obligatorio.")
    @DecimalMin(
            value = "0.01",
            message = "El precio por hora debe ser de al menos USD 0.01."
    )
    @Digits(
            integer = 8,
            fraction = 2,
            message = "El precio admite hasta 8 dígitos enteros y 2 decimales."
    )
    @Column(
            name = "precio_por_hora_usd",
            nullable = false,
            precision = 10,
            scale = 2
    )
    private BigDecimal precioPorHoraUsd;

    @NotNull(message = "El intervalo de mantenimiento preventivo es obligatorio.")
    @Positive(message = "El intervalo preventivo debe ser mayor que cero.")
    @Column(name = "intervalo_preventivo_meses", nullable = false)
    private Integer intervaloPreventivoMeses;

    public TipoBicicleta() {
    }

    public TipoBicicleta(
            String codigo,
            BigDecimal precioPorHoraUsd,
            Integer intervaloPreventivoMeses
    ) {
        setCodigo(codigo);
        setPrecioPorHoraUsd(precioPorHoraUsd);
        setIntervaloPreventivoMeses(intervaloPreventivoMeses);
    }

    public Integer getId() {
        return id;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo == null
                ? null
                : codigo.trim().toUpperCase(Locale.ROOT);
    }

    public BigDecimal getPrecioPorHoraUsd() {
        return precioPorHoraUsd;
    }

    public void setPrecioPorHoraUsd(BigDecimal precioPorHoraUsd) {
        this.precioPorHoraUsd = precioPorHoraUsd;
    }

    public Integer getIntervaloPreventivoMeses() {
        return intervaloPreventivoMeses;
    }

    public void setIntervaloPreventivoMeses(
            Integer intervaloPreventivoMeses
    ) {
        this.intervaloPreventivoMeses = intervaloPreventivoMeses;
    }
}