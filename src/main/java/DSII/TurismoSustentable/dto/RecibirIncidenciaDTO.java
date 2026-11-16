package DSII.TurismoSustentable.dto;
import javax.validation.constraints.*;

public class RecibirIncidenciaDTO {
    @NotNull(message="Recargá la incidencia antes de operar.") @PositiveOrZero(message="La versión de incidencia no es válida.") public Integer version;
    @NotNull(message="Recargá la bicicleta antes de operar.") @PositiveOrZero(message="La versión de bicicleta no es válida.") public Integer versionBicicleta;
    @Positive(message="Seleccioná una estación válida.") public Integer estacionId;
    @Size(max=2000,message="El lugar de recepción admite hasta 2000 caracteres.") public String lugarRecepcion;
    @NotBlank(message="El motivo de la recepción es obligatorio.") @Size(max=2000,message="El motivo admite hasta 2000 caracteres.") public String motivo;
    @NotNull(message="Confirmá la recepción física real.") @AssertTrue(message="Confirmá la recepción física real.") public Boolean recepcionFisicaConfirmada;
    @com.fasterxml.jackson.annotation.JsonAnySetter public void rechazar(String campo,Object valor){throw new IllegalArgumentException("La solicitud contiene campos no permitidos.");}
}
