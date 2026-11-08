package DSII.TurismoSustentable.models;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.util.Locale;

@Entity
@Table(name = "estado_orden_mantenimiento")
public class EstadoOrdenMantenimiento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotBlank(message = "El código del estado es obligatorio.")
    @Size(
            max = 20,
            message = "El código del estado no puede superar los 20 caracteres."
    )
    @Pattern(
            regexp = "^$|^(CREADA|EN_PROCESO|PAUSADA|FINALIZADA|CANCELADA)$",
            message = "El código no corresponde a un estado de orden permitido."
    )
    @Column(nullable = false, unique = true, length = 20)
    private String codigo;

    public EstadoOrdenMantenimiento() {
    }

    public EstadoOrdenMantenimiento(String codigo) {
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