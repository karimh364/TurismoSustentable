package DSII.TurismoSustentable.models;

import javax.persistence.*;
import javax.validation.constraints.*;
import java.util.Locale;

@Entity
@Table(name = "estado_pago")
public class EstadoPago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotBlank
    @Size(max = 20)
    @Pattern(
            regexp = "^(CREADO|PENDIENTE|APROBADO|RECHAZADO|CANCELADO)$",
            message = "El estado del pago no es válido."
    )
    @Column(nullable = false, unique = true, length = 20)
    private String codigo;

    public EstadoPago() {
    }

    public EstadoPago(String codigo) {
        setCodigo(codigo);
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
}