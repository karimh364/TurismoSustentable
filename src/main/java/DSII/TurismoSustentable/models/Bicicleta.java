package DSII.TurismoSustentable.models;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.HashSet;
import javax.validation.constraints.Size;
import java.util.Set;

@Entity
public class Bicicleta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @NotBlank(message = "El código es obligatorio.")
    @Size(max = 255)
    @Column(nullable = false, unique = true, length = 255)
    private String codigoBicicleta;

    @NotNull(message = "El tipo de bicicleta es obligatorio.")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tipo_bicicleta_id", nullable = false)
    private TipoBicicleta tipo;

    @NotNull(message = "El estado es obligatorio.")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "estado_bicicleta_id", nullable = false)
    private EstadoBicicleta estado;

    @NotNull
    @Column(name = "fecha_alta", nullable = false, updatable = false)
    private LocalDate fechaAlta;

    @Version
    @Column(nullable = false)
    private Integer version;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "estacion_id", nullable = true)
    private Estacion ubicacionActual;

    @Column(nullable = false)
    private boolean borradoBici = false; // Independiente del estado y de la presencia física
    // ------------------------------------

    @OneToMany(mappedBy = "bicicleta", fetch = FetchType.LAZY)
    private Set<Alquiler> alquileres = new HashSet<>();

    // CONSTRUCTORES
    public Bicicleta() {
    }

    public Bicicleta(String codigoBicicleta, TipoBicicleta tipo, EstadoBicicleta estado, Estacion ubicacionActual) {
        setCodigoBicicleta(codigoBicicleta);
        this.tipo = tipo;
        this.estado = estado;
        this.ubicacionActual = ubicacionActual;
        this.borradoBici = false;
    }

    // GETTERS Y SETTERS

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getCodigoBicicleta() {
        return codigoBicicleta;
    }

    public void setCodigoBicicleta(String codigoBicicleta) {
        this.codigoBicicleta = codigoBicicleta == null ? null : codigoBicicleta.trim();
    }

    public TipoBicicleta getTipo() {
        return tipo;
    }

    public void setTipo(TipoBicicleta tipo) {
        this.tipo = tipo;
    }

    public EstadoBicicleta getEstado() {
        return estado;
    }

    public void setEstado(EstadoBicicleta estado) {
        this.estado = estado;
    }

    // Getters y Setters de los campos adicionales

    public LocalDate getFechaAlta() { return fechaAlta; }
    public void setFechaAlta(LocalDate fechaAlta) { this.fechaAlta = fechaAlta; }
    public Integer getVersion() { return version; }

    public Estacion getUbicacionActual() {
        return ubicacionActual;
    }

    public void setUbicacionActual(Estacion ubicacionActual) {
        this.ubicacionActual = ubicacionActual;
    }

    public boolean isBorradoBici() {
        return borradoBici;
    }

    public void setBorradoBici(boolean borradoBici) {
        this.borradoBici = borradoBici;
    }

    // Getters y Setters de la relación

    public Set<Alquiler> getAlquileres() {
        return alquileres;
    }

    public void setAlquileres(Set<Alquiler> alquileres) {
        this.alquileres = alquileres;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Bicicleta)) return false;

        Bicicleta bicicleta = (Bicicleta) o;

        return getId() != 0 && getId() == bicicleta.getId();
    }

    @Override
    public int hashCode() {
        // El código es editable: no debe cambiar el hash de un elemento del Set.
        return Bicicleta.class.hashCode();
    }

}
