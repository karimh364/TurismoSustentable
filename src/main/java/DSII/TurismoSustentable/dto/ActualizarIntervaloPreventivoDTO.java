package DSII.TurismoSustentable.dto;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import javax.validation.constraints.*;
public class ActualizarIntervaloPreventivoDTO {
    @NotNull @Positive private Integer mesesAnteriores;
    @NotNull @Positive private Integer meses;
    @NotBlank @Size(max=2000) private String motivo;
    public Integer getMesesAnteriores(){return mesesAnteriores;} public void setMesesAnteriores(Integer v){mesesAnteriores=v;}
    public Integer getMeses(){return meses;} public void setMeses(Integer v){meses=v;}
    public String getMotivo(){return motivo;} public void setMotivo(String v){motivo=v==null?null:v.trim();}
    @JsonAnySetter public void rechazar(String campo,Object valor){throw new IllegalArgumentException("La solicitud contiene campos no permitidos.");}
}
