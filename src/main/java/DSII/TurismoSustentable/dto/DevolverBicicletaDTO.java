package DSII.TurismoSustentable.dto;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;

import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import javax.validation.constraints.Size;

public class DevolverBicicletaDTO {

    @NotNull(message = "La estación de devolución es obligatoria.")
    @Positive(message = "Debe seleccionar una estación de devolución válida.")
    private Integer estacionDevolucionId;

    @NotNull(message = "Debe indicar si reporta una falla.")
    private Boolean fallaReportada = false;

    @Size(max = 2000, message = "La descripción de la falla admite hasta 2000 caracteres.")
    private String descripcionFalla;

    public Integer getEstacionDevolucionId() {
        return estacionDevolucionId;
    }

    public void setEstacionDevolucionId(Integer estacionDevolucionId) {
        this.estacionDevolucionId = estacionDevolucionId;
    }

    public Boolean getFallaReportada() {
        return fallaReportada;
    }

    public void setFallaReportada(Boolean fallaReportada) {
        this.fallaReportada = fallaReportada;
    }

    public String getDescripcionFalla() {
        return descripcionFalla;
    }

    public void setDescripcionFalla(String descripcionFalla) {
        this.descripcionFalla = descripcionFalla == null ? null : descripcionFalla.trim();
    }

    @JsonIgnore
    @AssertTrue(message = "Si reporta una falla, describa qué ocurrió; si no, deje la descripción vacía.")
    public boolean isReporteFallaCoherente() {
        boolean tieneDescripcion = descripcionFalla != null && !descripcionFalla.isBlank();
        return fallaReportada == null || fallaReportada == tieneDescripcion;
    }

    @JsonAnySetter
    public void rechazarCampoDesconocido(String campo, Object valor) {
        throw new IllegalArgumentException("La solicitud contiene campos no permitidos.");
    }
}
