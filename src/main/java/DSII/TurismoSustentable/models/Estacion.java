package DSII.TurismoSustentable.models;

import javax.persistence.*;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.DecimalMax;
import javax.validation.constraints.Digits;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;
import com.fasterxml.jackson.annotation.JsonProperty;

@Entity
public class Estacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @NotBlank(message = "El nombre es obligatorio")
    @Column(unique = true) // No permitimos dos estaciones con el mismo nombre
    private String nombre; // Ej: "Plaza Cien Años"

    @NotBlank(message = "La dirección es obligatoria")
    private String direccion; // Ej: "Av. San Martín 123"

    @Min(value = 1, message = "La capacidad debe ser al menos 1")
    private int capacidad; // Ej: 20 (cuántas bicis entran)

    private boolean borradoEstacion = false;

    @Column(name = "es_publica", nullable = false)
    private boolean esPublica = true;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @Version
    @Column(name = "version", nullable = false)
    private Integer version;

    @NotNull(message = "La latitud es obligatoria")
    @DecimalMin(value = "-90", message = "La latitud debe estar entre -90 y 90.")
    @DecimalMax(value = "90", message = "La latitud debe estar entre -90 y 90.")
    @Digits(integer = 3, fraction = 6, message = "La latitud admite hasta 6 decimales.")
    @Column(name = "latitud", nullable = false, precision = 9, scale = 6)
    private BigDecimal latitud;

    @NotNull(message = "La longitud es obligatoria")
    @DecimalMin(value = "-180", message = "La longitud debe estar entre -180 y 180.")
    @DecimalMax(value = "180", message = "La longitud debe estar entre -180 y 180.")
    @Digits(integer = 3, fraction = 6, message = "La longitud admite hasta 6 decimales.")
    @Column(name = "longitud", nullable = false, precision = 9, scale = 6)
    private BigDecimal longitud;

    // Relación: Una Estación puede tener muchas bicicletas "estacionadas" aquí.
    @OneToMany(mappedBy = "ubicacionActual", fetch = FetchType.LAZY)
    private Set<Bicicleta> bicicletasEnEstacion = new HashSet<>();

    // CONSTRUCTORES
    public Estacion() {
    }

    // Constructor
    public Estacion(String nombre, String direccion, int capacidad, BigDecimal latitud, BigDecimal longitud) {
        this.nombre = nombre;
        this.direccion = direccion;
        this.capacidad = capacidad;
        this.latitud = latitud;
        this.longitud = longitud;
        this.borradoEstacion = false;
    }

    // GETTERS Y SETTERS (Básicos)
    public int getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }

    public int getCapacidad() {
        return capacidad;
    }

    public void setCapacidad(int capacidad) {
        this.capacidad = capacidad;
    }

    public BigDecimal getLatitud() {
        return latitud;
    }

    public void setLatitud(BigDecimal latitud) {
        this.latitud = latitud;
    }

    public BigDecimal getLongitud() {
        return longitud;
    }

    public void setLongitud(BigDecimal longitud) {
        this.longitud = longitud;
    }

    public Set<Bicicleta> getBicicletasEnEstacion() {
        return bicicletasEnEstacion;
    }

    public boolean isBorradoEstacion() {
        return borradoEstacion;
    }

    public void setBorradoEstacion(boolean borradoEstacion) {
        this.borradoEstacion = borradoEstacion;
    }

    public boolean isEsPublica() {
        return esPublica;
    }

    public void setEsPublica(boolean esPublica) {
        this.esPublica = esPublica;
    }

    public Integer getVersion() {
        return version;
    }

    // --- equals() y hashCode() ---
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Estacion estacion = (Estacion) o;
        return nombre != null ? nombre.equals(estacion.nombre) : estacion.nombre == null;
    }

    @Override
    public int hashCode() {
        return nombre != null ? nombre.hashCode() : 0;
    }
}
