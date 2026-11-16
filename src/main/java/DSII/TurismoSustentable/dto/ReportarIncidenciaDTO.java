package DSII.TurismoSustentable.dto;
import javax.validation.constraints.*;
import java.time.OffsetDateTime;

public class ReportarIncidenciaDTO {
    @NotNull(message="Seleccioná el alquiler.") @Positive(message="Seleccioná un alquiler válido.") public Integer alquilerId;
    @NotNull(message="Seleccioná la bicicleta del alquiler.") @Positive(message="Seleccioná un detalle válido.") public Integer detalleId;
    @NotNull(message="Recargá el detalle antes de reportar.") @PositiveOrZero(message="Recargá el detalle antes de reportar.") public Integer versionDetalle;
    @NotNull(message="Seleccioná el tipo de incidencia.") @Pattern(regexp="AVERIA|ACCIDENTE|PERDIDA|ROBO",message="El tipo de incidencia no es válido.") public String tipo;
    @NotBlank(message="La descripción es obligatoria.") @Size(max=10000,message="La descripción admite hasta 10000 caracteres.") public String descripcion;
    @Size(max=2000,message="El lugar informado admite hasta 2000 caracteres.") public String lugarReportado;
    @NotNull(message="La fecha y hora del hecho es obligatoria.") public OffsetDateTime fechaHoraHechoInformada;
    @Positive(message="El informante debe tener un identificador válido.") public Integer informanteId; // Solo ADMIN; CLIENTE se obtiene de la sesión.
    @com.fasterxml.jackson.annotation.JsonAnySetter public void rechazar(String campo,Object valor){throw new IllegalArgumentException("La solicitud contiene campos no permitidos.");}
}
