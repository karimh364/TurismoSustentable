package DSII.TurismoSustentable.dto;

import javax.validation.constraints.*;

public class CrearEstacionDTO {

    @NotBlank(message = "El nombre de la estación es obligatorio.")
    @Size(
            max = 255,
            message = "El nombre no puede superar los 255 caracteres."
    )
    private String nombre;

    @NotBlank(message = "La dirección es obligatoria.")
    @Size(
            max = 255,
            message = "La dirección no puede superar los 255 caracteres."
    )
    private String direccion;

    @NotNull(message = "La capacidad es obligatoria.")
    @Min(value = 1, message = "La capacidad debe ser al menos 1.")
    private Integer capacidad;

    @NotNull(message = "La latitud es obligatoria.")
    @DecimalMin(
            value = "-90.0",
            message = "La latitud debe estar entre -90 y 90."
    )
    @DecimalMax(
            value = "90.0",
            message = "La latitud debe estar entre -90 y 90."
    )
    private Double latitud;

    @NotNull(message = "La longitud es obligatoria.")
    @DecimalMin(
            value = "-180.0",
            message = "La longitud debe estar entre -180 y 180."
    )
    @DecimalMax(
            value = "180.0",
            message = "La longitud debe estar entre -180 y 180."
    )
    private Double longitud;

    @NotNull(
            message = "Indicá si la estación es pública o una estación de uso interno."
    )
    private Boolean esPublica;

    public CrearEstacionDTO() {
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre == null
                ? null
                : nombre.replaceAll("(?U)\\s+", " ").trim();
    }

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion == null
                ? null
                : direccion.replaceAll("(?U)\\s+", " ").trim();
    }

    public Integer getCapacidad() {
        return capacidad;
    }

    public void setCapacidad(Integer capacidad) {
        this.capacidad = capacidad;
    }

    public Double getLatitud() {
        return latitud;
    }

    public void setLatitud(Double latitud) {
        this.latitud = latitud;
    }

    public Double getLongitud() {
        return longitud;
    }

    public void setLongitud(Double longitud) {
        this.longitud = longitud;
    }

    public Boolean getEsPublica() {
        return esPublica;
    }

    public void setEsPublica(Boolean esPublica) {
        this.esPublica = esPublica;
    }
}