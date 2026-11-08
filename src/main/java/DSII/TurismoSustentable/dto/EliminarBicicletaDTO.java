package DSII.TurismoSustentable.dto;

import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.NotNull;

/**
 * Reutiliza versión/motivo y exige una confirmación propia de la eliminación.
 * No agrega campos a las peticiones de inactivar, habilitar o dar de baja.
 */
public class EliminarBicicletaDTO extends OperacionBicicletaDTO {

    @NotNull(message = "Debe confirmar la salida física definitiva de la bicicleta.")
    @AssertTrue(message = "Debe confirmar la salida física definitiva de la bicicleta.")
    private Boolean salidaFisicaConfirmada;

    public Boolean getSalidaFisicaConfirmada() { return salidaFisicaConfirmada; }

    public void setSalidaFisicaConfirmada(Boolean salidaFisicaConfirmada) {
        this.salidaFisicaConfirmada = salidaFisicaConfirmada;
    }
}
