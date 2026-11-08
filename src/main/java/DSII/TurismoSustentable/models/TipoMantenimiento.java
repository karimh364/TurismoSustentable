package DSII.TurismoSustentable.models;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.util.Locale;

@Entity
@Table(name = "tipo_mantenimiento")
public class TipoMantenimiento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotBlank(message = "El código del tipo de mantenimiento es obligatorio.")
    @Size(
            max = 20,
            message = "El código no puede superar los 20 caracteres."
    )
    @Pattern(
            regexp = "^$|^(PREVENTIVO|CORRECTIVO)$",
            message = "El tipo de mantenimiento debe ser PREVENTIVO o CORRECTIVO."
    )
    @Column(nullable = false, unique = true, length = 20)
    private String codigo;

    @Size(
            max = 255,
            message = "La descripción no puede superar los 255 caracteres."
    )
    @Column(length = 255)
    private String descripcion;

    public TipoMantenimiento() {
    }

    public TipoMantenimiento(String codigo, String descripcion) {
        setCodigo(codigo);
        setDescripcion(descripcion);
    }

    public Integer getId() {
        return id;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo == null
                ? null
                : codigo.trim().toUpperCase(Locale.ROOT);
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion == null
                ? null
                : descripcion.trim();
    }
}