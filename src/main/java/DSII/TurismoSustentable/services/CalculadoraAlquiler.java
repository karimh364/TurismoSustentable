package DSII.TurismoSustentable.services;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;

@Component
public class CalculadoraAlquiler {

    private static final BigDecimal SEGUNDOS_POR_HORA =
            new BigDecimal("3600");

    private static final BigDecimal TARIFA_MINIMA =
            new BigDecimal("0.01");

    public long calcularSegundosCompletos(
            LocalDateTime retiro,
            LocalDateTime finUso
    ) {
        if (retiro == null || finUso == null) {
            throw new IllegalArgumentException(
                    "El retiro y el fin de uso son obligatorios."
            );
        }

        if (finUso.isBefore(retiro)) {
            throw new IllegalArgumentException(
                    "El fin de uso no puede ser anterior al retiro."
            );
        }

        return Duration.between(retiro, finUso).getSeconds();
    }

    public BigDecimal calcularSubtotalUsd(
            BigDecimal precioPorHoraUsd,
            long segundos
    ) {
        return calcular(
                precioPorHoraUsd,
                segundos,
                BigDecimal.ONE
        );
    }

    public BigDecimal calcularSubtotalArs(
            BigDecimal precioPorHoraUsd,
            long segundos,
            BigDecimal cotizacionArsPorUsd
    ) {
        return calcular(
                precioPorHoraUsd,
                segundos,
                cotizacionArsPorUsd
        );
    }

    private BigDecimal calcular(
            BigDecimal precioPorHoraUsd,
            long segundos,
            BigDecimal cotizacion
    ) {
        if (precioPorHoraUsd == null
                || precioPorHoraUsd.compareTo(TARIFA_MINIMA) < 0) {
            throw new IllegalArgumentException(
                    "La tarifa debe ser de al menos USD 0.01."
            );
        }

        if (segundos < 0) {
            throw new IllegalArgumentException(
                    "Los segundos de uso no pueden ser negativos."
            );
        }

        if (cotizacion == null || cotizacion.signum() <= 0) {
            throw new IllegalArgumentException(
                    "La cotización debe ser mayor que cero."
            );
        }

        return precioPorHoraUsd
                .multiply(BigDecimal.valueOf(segundos))
                .multiply(cotizacion)
                .divide(
                        SEGUNDOS_POR_HORA,
                        2,
                        RoundingMode.HALF_UP
                );
    }
}
