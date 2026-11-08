package DSII.TurismoSustentable.dto;
import javax.validation.constraints.*;

public class OperacionBicicletaDTO {
    @NotNull(message = "Recargá los datos antes de realizar la operación.")
    @PositiveOrZero
    private Integer version;
    @NotBlank(message = "Debe indicar el motivo de la operación.")
    @Size(max = 1000, message = "El motivo no puede superar los 1000 caracteres.")
    private String motivo;

    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo == null ? null : motivo.trim(); }
}
