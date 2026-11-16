package DSII.TurismoSustentable.dto;
import javax.validation.constraints.*;
import java.time.OffsetDateTime;

public class ResolverIncidenciaDTO {
    @NotNull(message="Recargá la incidencia antes de operar.") @PositiveOrZero(message="La versión de incidencia no es válida.") public Integer version;
    @NotNull(message="Recargá la bicicleta antes de operar.") @PositiveOrZero(message="La versión de bicicleta no es válida.") public Integer versionBicicleta;
    @NotNull(message="Seleccioná la decisión.") @Pattern(regexp="VALIDADA|DESCARTADA",message="La decisión debe ser validar o descartar.") public String decision;
    @NotBlank(message="El fundamento de la decisión es obligatorio.") @Size(max=2000,message="El fundamento admite hasta 2000 caracteres.") public String motivo;
    public OffsetDateTime fechaHoraCorteValidado;
    @com.fasterxml.jackson.annotation.JsonAnySetter public void rechazar(String campo,Object valor){throw new IllegalArgumentException("La solicitud contiene campos no permitidos.");}
}
