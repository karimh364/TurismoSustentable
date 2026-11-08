package DSII.TurismoSustentable.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import javax.validation.constraints.Size;

public class CrearBicicletaDTO {

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

    @NotNull(message = "La estación es obligatoria.")
    @Positive(message = "Debe seleccionar una estación válida.")
    private Integer estacionId;

    public CrearBicicletaDTO() {
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
}
