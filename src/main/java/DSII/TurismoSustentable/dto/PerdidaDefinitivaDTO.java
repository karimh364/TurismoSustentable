package DSII.TurismoSustentable.dto;
import javax.validation.constraints.*;

public class PerdidaDefinitivaDTO {
    @NotNull(message="Recargá la incidencia antes de operar.") @PositiveOrZero(message="La versión de incidencia no es válida.") public Integer version;
    @NotNull(message="Recargá la bicicleta antes de operar.") @PositiveOrZero(message="La versión de bicicleta no es válida.") public Integer versionBicicleta;
    @NotBlank(message="El motivo de pérdida definitiva es obligatorio.") @Size(max=2000,message="El motivo admite hasta 2000 caracteres.") public String motivo;
    @NotNull(message="Confirmá la pérdida definitiva.") @AssertTrue(message="Confirmá la pérdida definitiva.") public Boolean perdidaDefinitivaConfirmada;
    @com.fasterxml.jackson.annotation.JsonAnySetter public void rechazar(String campo,Object valor){throw new IllegalArgumentException("La solicitud contiene campos no permitidos.");}
}
