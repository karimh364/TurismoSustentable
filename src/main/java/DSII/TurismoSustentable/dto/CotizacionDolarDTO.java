package DSII.TurismoSustentable.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public final class CotizacionDolarDTO {

    private final BigDecimal cotizacionArsPorUsd;
    private final String fuente;
    private final OffsetDateTime fechaActualizacionFuente;

    public CotizacionDolarDTO(
            BigDecimal cotizacionArsPorUsd,
            String fuente,
            OffsetDateTime fechaActualizacionFuente
    ) {
        if (cotizacionArsPorUsd == null
                || cotizacionArsPorUsd.signum() <= 0) {
            throw new IllegalArgumentException(
                    "La cotización debe ser mayor que cero."
            );
        }

        if (cotizacionArsPorUsd.scale() > 8
                || cotizacionArsPorUsd.precision()
                - cotizacionArsPorUsd.scale() > 12) {
            throw new IllegalArgumentException(
                    "La cotización supera la precisión admitida."
            );
        }

        if (fuente == null || fuente.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "La fuente de cotización es obligatoria."
            );
        }

        String fuenteNormalizada = fuente.trim();

        if (fuenteNormalizada.length() > 100) {
            throw new IllegalArgumentException(
                    "La fuente no puede superar los 100 caracteres."
            );
        }

        this.cotizacionArsPorUsd = cotizacionArsPorUsd;
        this.fuente = fuenteNormalizada;
        this.fechaActualizacionFuente = fechaActualizacionFuente;
    }

    public BigDecimal getCotizacionArsPorUsd() {
        return cotizacionArsPorUsd;
    }

    public String getFuente() {
        return fuente;
    }

    public OffsetDateTime getFechaActualizacionFuente() {
        return fechaActualizacionFuente;
    }
}