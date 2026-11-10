package DSII.TurismoSustentable.dto;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import org.hibernate.validator.constraints.UniqueElements;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import java.util.List;

public class CrearAlquilerDTO {

    @NotNull(message = "La estación de retiro es obligatoria.")
    @Positive(message = "Debe seleccionar una estación de retiro válida.")
    private Integer estacionRetiroId;

    @NotEmpty(message = "Debe seleccionar al menos una bicicleta.")
    @UniqueElements(
            message = "No puede seleccionar la misma bicicleta más de una vez."
    )
    private List<
            @NotNull(message = "La selección contiene una bicicleta sin identificar.")
            @Positive(message = "Debe seleccionar bicicletas válidas.")
                    Integer> bicicletaIds;

    @NotNull(message = "La duración estimada es obligatoria.")
    @Positive(message = "La duración estimada debe ser mayor que cero.")
    private Integer duracionEstimadaSegundos;

    public CrearAlquilerDTO() {
    }

    public Integer getEstacionRetiroId() {
        return estacionRetiroId;
    }

    public void setEstacionRetiroId(Integer estacionRetiroId) {
        this.estacionRetiroId = estacionRetiroId;
    }

    public List<Integer> getBicicletaIds() {
        return bicicletaIds;
    }

    public void setBicicletaIds(List<Integer> bicicletaIds) {
        this.bicicletaIds = bicicletaIds;
    }

    public Integer getDuracionEstimadaSegundos() {
        return duracionEstimadaSegundos;
    }

    public void setDuracionEstimadaSegundos(Integer duracionEstimadaSegundos) {
        this.duracionEstimadaSegundos = duracionEstimadaSegundos;
    }

    @JsonAnySetter
    public void rechazarCampoDesconocido(String campo, Object valor) {
        throw new IllegalArgumentException(
                "La solicitud contiene campos no permitidos."
        );
    }
}