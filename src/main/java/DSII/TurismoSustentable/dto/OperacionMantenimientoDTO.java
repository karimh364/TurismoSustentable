package DSII.TurismoSustentable.dto;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import javax.validation.constraints.*;
import java.math.BigDecimal;

// Comando común; cada acción del servicio admite solo su subconjunto de campos.
public class OperacionMantenimientoDTO {
    @NotNull(message="Recargá la orden antes de operar.") @Min(0) private Integer version;
    @Min(0) private Integer versionBicicleta;
    @Positive private Integer tecnicoId;
    @Size(max=2000) private String motivo;
    @Size(max=10000) private String diagnostico;
    @Size(max=10000) private String descripcionTrabajo;
    @Size(max=10000) private String observaciones;
    @DecimalMin("0.00") @Digits(integer=10,fraction=2) private BigDecimal costoRepuestos;
    @Pattern(regexp="^(APTA|NO_APTA)$") private String resultadoTecnico;
    public Integer getVersion(){return version;} public void setVersion(Integer v){version=v;}
    public Integer getVersionBicicleta(){return versionBicicleta;} public void setVersionBicicleta(Integer v){versionBicicleta=v;}
    public Integer getTecnicoId(){return tecnicoId;} public void setTecnicoId(Integer v){tecnicoId=v;}
    public String getMotivo(){return motivo;} public void setMotivo(String v){motivo=limpiar(v);}
    public String getDiagnostico(){return diagnostico;} public void setDiagnostico(String v){diagnostico=limpiar(v);}
    public String getDescripcionTrabajo(){return descripcionTrabajo;} public void setDescripcionTrabajo(String v){descripcionTrabajo=limpiar(v);}
    public String getObservaciones(){return observaciones;} public void setObservaciones(String v){observaciones=limpiar(v);}
    public BigDecimal getCostoRepuestos(){return costoRepuestos;} public void setCostoRepuestos(BigDecimal v){costoRepuestos=v;}
    public String getResultadoTecnico(){return resultadoTecnico;} public void setResultadoTecnico(String v){resultadoTecnico=limpiar(v);}
    private String limpiar(String v){return v == null ? null : v.trim();}
    @JsonAnySetter public void rechazar(String campo,Object valor){throw new IllegalArgumentException("La solicitud contiene campos no permitidos.");}
}
