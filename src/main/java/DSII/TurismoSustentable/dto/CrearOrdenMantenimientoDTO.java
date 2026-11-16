package DSII.TurismoSustentable.dto;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import javax.validation.constraints.*;

public class CrearOrdenMantenimientoDTO {
    @NotNull @Positive private Integer bicicletaId;
    @NotBlank @Pattern(regexp="^(PREVENTIVO|CORRECTIVO)$") private String tipo;
    @Positive private Integer tecnicoId;
    @NotBlank @Size(max=2000) private String motivo;
    public Integer getBicicletaId() { return bicicletaId; }
    public void setBicicletaId(Integer v) { bicicletaId=v; }
    public String getTipo() { return tipo; }
    public void setTipo(String v) { tipo=v == null ? null : v.trim(); }
    public Integer getTecnicoId() { return tecnicoId; }
    public void setTecnicoId(Integer v) { tecnicoId=v; }
    public String getMotivo() { return motivo; }
    public void setMotivo(String v) { motivo=v == null ? null : v.trim(); }
    @JsonAnySetter public void rechazar(String campo,Object valor) {
        throw new IllegalArgumentException("La solicitud contiene campos no permitidos.");
    }
}
