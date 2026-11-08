package DSII.TurismoSustentable.models;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.util.Locale;

@Entity
@Table(name = "estado_alquiler_detalle")
public class EstadoAlquilerDetalle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotBlank(message = "El código del estado es obligatorio.")
    @Size(
            max = 30,
            message = "El código del estado no puede superar los 30 caracteres."
    )
    @Pattern(
            regexp = "^$|^(RESERVADO|PENDIENTE_RETIRO|EN_CURSO|DEVUELTO|"
                    + "CANCELADO|VENCIDO|CERRADO_POR_INCIDENCIA)$",
            message = "El código no corresponde a un estado de detalle de alquiler permitido."
    )
    @Column(nullable = false, unique = true, length = 30)
    private String codigo;

    public EstadoAlquilerDetalle() {
    }

    public EstadoAlquilerDetalle(String codigo) {
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