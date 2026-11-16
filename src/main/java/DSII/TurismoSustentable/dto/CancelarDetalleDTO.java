package DSII.TurismoSustentable.dto;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import javax.validation.constraints.*;

public class CancelarDetalleDTO {
    @NotBlank(message = "Debe indicar el motivo operativo o de seguridad.")
    @Size(max = 1000, message = "El motivo no puede superar los 1000 caracteres.")
    private String motivo;
    @NotBlank(message = "Debe indicar el estado operativo de la bicicleta.")
    @Pattern(regexp = "^(INACTIVA|MANTENIMIENTO)$", message = "Debe elegir INACTIVA o MANTENIMIENTO.")
    private String estadoBicicleta;
    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo == null ? null : motivo.trim(); }
    public String getEstadoBicicleta() { return estadoBicicleta; }
    public void setEstadoBicicleta(String estado) { this.estadoBicicleta = estado; }
    @JsonAnySetter public void rechazarCampo(String campo, Object valor) {
        throw new IllegalArgumentException("La solicitud contiene campos no permitidos.");
    }
}
