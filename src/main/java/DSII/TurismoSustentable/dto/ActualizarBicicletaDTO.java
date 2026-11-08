package DSII.TurismoSustentable.dto;

import javax.validation.constraints.*;

public class ActualizarBicicletaDTO {

    @com.fasterxml.jackson.annotation.JsonAnySetter
    public void rechazarCampoDesconocido(String campo, Object valor) {
        throw new IllegalArgumentException("La solicitud contiene campos no permitidos.");
    }

    @NotBlank(message = "El código de la bicicleta es obligatorio.")
    @Size(
            max = 255,
            message = "El código no puede superar los 255 caracteres."
    )
    private String codigoBicicleta;

    @NotNull(message = "El tipo de bicicleta es obligatorio.")
    @Positive(message = "Debe seleccionar un tipo de bicicleta válido.")
    private Integer tipoBicicletaId;

    @Positive(message = "Debe seleccionar una estación válida.")
    private Integer estacionId;

    @NotNull(
            message = "Falta información de la bicicleta. Recargá los datos antes de editar."
    )
    @PositiveOrZero(
            message = "La versión no es válida. Recargá los datos."
    )
    private Integer version;

    @Size(
            max = 1000,
            message = "El motivo no puede superar los 1000 caracteres."
    )
    private String motivo;

    public ActualizarBicicletaDTO() {
    }

    public String getCodigoBicicleta() {
        return codigoBicicleta;
    }

    public void setCodigoBicicleta(String codigoBicicleta) {
        this.codigoBicicleta = codigoBicicleta == null
                ? null
                : codigoBicicleta.trim();
    }

    public Integer getTipoBicicletaId() {
        return tipoBicicletaId;
    }

    public void setTipoBicicletaId(Integer tipoBicicletaId) {
        this.tipoBicicletaId = tipoBicicletaId;
    }

    public Integer getEstacionId() {
        return estacionId;
    }

    public void setEstacionId(Integer estacionId) {
        this.estacionId = estacionId;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo == null ? null : motivo.trim();
    }
}
