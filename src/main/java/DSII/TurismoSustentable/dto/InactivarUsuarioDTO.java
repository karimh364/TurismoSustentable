package DSII.TurismoSustentable.dto;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import javax.validation.constraints.Size;
public class InactivarUsuarioDTO {
    @Size(max=2000) private String motivo;
    public String getMotivo(){return motivo;} public void setMotivo(String v){motivo=v==null?null:v.trim();}
    @JsonAnySetter public void rechazar(String campo,Object valor){throw new IllegalArgumentException("La solicitud contiene campos no permitidos.");}
}
