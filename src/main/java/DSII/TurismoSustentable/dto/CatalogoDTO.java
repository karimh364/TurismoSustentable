package DSII.TurismoSustentable.dto;

public class CatalogoDTO {

    private final Integer id;
    private final String codigo;
    private final String nombre;

    public CatalogoDTO(
            Integer id,
            String codigo,
            String nombre
    ) {
        this.id = id;
        this.codigo = codigo;
        this.nombre = nombre;
    }

    public Integer getId() {
        return id;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getNombre() {
        return nombre;
    }
}