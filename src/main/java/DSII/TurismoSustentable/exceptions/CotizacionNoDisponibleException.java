package DSII.TurismoSustentable.exceptions;

public class CotizacionNoDisponibleException extends RuntimeException {

    public CotizacionNoDisponibleException(Throwable causa) {
        super(
                "No pudimos obtener una cotización válida. "
                        + "Intentá nuevamente en unos momentos.",
                causa
        );
    }
}