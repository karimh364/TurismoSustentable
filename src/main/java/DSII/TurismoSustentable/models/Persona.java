package DSII.TurismoSustentable.models;

import DSII.TurismoSustentable.validation.DocumentoIdentidad;
import DSII.TurismoSustentable.validation.NormalizacionNombre;

import javax.persistence.*;
import javax.validation.constraints.*;
import java.time.LocalDate;

@Entity
@Table(
        name = "persona",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_persona_documento",
                        columnNames = {
                                "tipo_documento_id",
                                "pais_emisor_id",
                                "numero_documento"
                        }
                )
        }
)
public class Persona {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotBlank(message = "El nombre no puede estar vacío.")
    @Size(max = 50, message = "El nombre no puede superar los 50 caracteres.")
    @Pattern(
            regexp = NormalizacionNombre.PATRON,
            message = "El nombre debe contener letras y separaciones simples por espacios, apóstrofes o guiones, sin separadores al inicio o al final."
    )
    @Column(nullable = false, length = 50)
    private String nombre;

    @NotBlank(message = "El apellido no puede estar vacío.")
    @Size(max = 50, message = "El apellido no puede superar los 50 caracteres.")
    @Pattern(
            regexp = NormalizacionNombre.PATRON,
            message = "El apellido debe contener letras y separaciones simples por espacios, apóstrofes o guiones, sin separadores al inicio o al final."
    )
    @Column(nullable = false, length = 50)
    private String apellido;

    @NotNull(message = "La fecha de nacimiento es obligatoria.")
    @Past(message = "La fecha de nacimiento debe estar en el pasado.")
    @Column(name = "fecha_nacimiento", nullable = false)
    private LocalDate fechaNacimiento;

    @NotNull(message = "El género es obligatorio.")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "genero_id", nullable = false)
    private Genero genero;

    @NotNull(message = "El tipo de documento es obligatorio.")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tipo_documento_id", nullable = false)
    private TipoDocumento tipoDocumento;

    @NotBlank(message = "El número de documento es obligatorio.")
    @Size(
            max = DocumentoIdentidad.LONGITUD_MAXIMA,
            message = "El número de documento no puede superar los 50 caracteres."
    )
    @Pattern(
            regexp = "^$|^[A-Z0-9]+$",
            message = "El documento debe guardarse sin espacios o símbolos y con las letras en mayúsculas."
    )
    @Column(name = "numero_documento", nullable = false, length = DocumentoIdentidad.LONGITUD_MAXIMA)
    private String numeroDocumento;

    @NotNull(message = "El país emisor del documento es obligatorio.")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pais_emisor_id", nullable = false)
    private Pais paisEmisor;

    public Persona() {
    }

    public Persona(
            String nombre,
            String apellido,
            LocalDate fechaNacimiento,
            Genero genero,
            TipoDocumento tipoDocumento,
            String numeroDocumento,
            Pais paisEmisor
    ) {
        setNombre(nombre);
        setApellido(apellido);
        this.fechaNacimiento = fechaNacimiento;
        this.genero = genero;
        this.tipoDocumento = tipoDocumento;
        setNumeroDocumento(numeroDocumento);
        this.paisEmisor = paisEmisor;
    }

    public Integer getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = NormalizacionNombre.normalizar(nombre);
    }

    public String getApellido() {
        return apellido;
    }

    public void setApellido(String apellido) {
        this.apellido = NormalizacionNombre.normalizar(apellido);
    }

    public LocalDate getFechaNacimiento() {
        return fechaNacimiento;
    }

    public void setFechaNacimiento(LocalDate fechaNacimiento) {
        this.fechaNacimiento = fechaNacimiento;
    }

    public Genero getGenero() {
        return genero;
    }

    public void setGenero(Genero genero) {
        this.genero = genero;
    }

    public TipoDocumento getTipoDocumento() {
        return tipoDocumento;
    }

    public void setTipoDocumento(TipoDocumento tipoDocumento) {
        this.tipoDocumento = tipoDocumento;
        setNumeroDocumento(this.numeroDocumento);
    }

    public String getNumeroDocumento() {
        return numeroDocumento;
    }

    public void setNumeroDocumento(String numeroDocumento) {
        this.numeroDocumento = DocumentoIdentidad.normalizar(
                numeroDocumento,
                tipoDocumento == null ? null : tipoDocumento.getCodigo()
        );
    }

    public Pais getPaisEmisor() {
        return paisEmisor;
    }

    public void setPaisEmisor(Pais paisEmisor) {
        this.paisEmisor = paisEmisor;
    }
}
