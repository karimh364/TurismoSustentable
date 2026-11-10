package DSII.TurismoSustentable.services;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class CalculadoraAlquilerTest {

    private final CalculadoraAlquiler calculadora =
            new CalculadoraAlquiler();

    @Test
    void calculaCuarentaYCincoMinutosDeUso() {
        LocalDateTime retiro =
                LocalDateTime.of(2026, 11, 9, 10, 0);

        long segundos = calculadora.calcularSegundosCompletos(
                retiro,
                retiro.plusMinutes(45)
        );

        assertEquals(2700L, segundos);

        assertEquals(
                decimal("7.50"),
                calculadora.calcularSubtotalUsd(decimal("10.00"), segundos)
        );

        assertEquals(
                decimal("11475.00"),
                calculadora.calcularSubtotalArs(
                        decimal("10.00"), segundos, decimal("1530")
                )
        );
    }

    @Test
    void calculaTresHorasSinAplicarMultaOBloquesExtra() {
        long segundos = 3L * 3600;

        assertEquals(
                decimal("30.00"),
                calculadora.calcularSubtotalUsd(decimal("10.00"), segundos)
        );

        assertEquals(
                decimal("45900.00"),
                calculadora.calcularSubtotalArs(
                        decimal("10.00"), segundos, decimal("1530")
                )
        );
    }

    @Test
    void descartaSoloLaFraccionFinalDelIntervalo() {
        LocalDateTime retiro =
                LocalDateTime.of(2026, 11, 9, 10, 0, 0, 800_000_000);

        LocalDateTime fin = retiro
                .plusSeconds(2723)
                .plusNanos(999_000_000);

        long segundos =
                calculadora.calcularSegundosCompletos(retiro, fin);

        assertEquals(2723L, segundos);

        assertEquals(
                decimal("6.81"),
                calculadora.calcularSubtotalUsd(decimal("9.00"), segundos)
        );

        assertEquals(
                decimal("6807.50"),
                calculadora.calcularSubtotalArs(
                        decimal("9.00"), segundos, decimal("1000")
                )
        );
    }

    @Test
    void calculaPesosSinConvertirDesdeElUsdYaRedondeado() {
        assertEquals(
                decimal("0.00"),
                calculadora.calcularSubtotalUsd(decimal("1.00"), 1)
        );

        assertEquals(
                decimal("0.28"),
                calculadora.calcularSubtotalArs(
                        decimal("1.00"), 1, decimal("1000")
                )
        );
    }

    @Test
    void redondeaHalfUpCadaLineaAntesDeSumarlas() {
        BigDecimal primera = calculadora.calcularSubtotalArs(
                decimal("2.01"), 180, decimal("10")
        );

        BigDecimal segunda = calculadora.calcularSubtotalArs(
                decimal("4.01"), 180, decimal("10")
        );

        assertEquals(decimal("1.01"), primera);
        assertEquals(decimal("2.01"), segunda);
        assertEquals(decimal("3.02"), primera.add(segunda));
    }

    @Test
    void ceroSegundosCompletosProduceCostoCero() {
        LocalDateTime retiro =
                LocalDateTime.of(2026, 11, 9, 10, 0);

        assertEquals(
                0L,
                calculadora.calcularSegundosCompletos(retiro, retiro)
        );

        long segundos = calculadora.calcularSegundosCompletos(
                retiro,
                retiro.plusNanos(999_999_999)
        );

        assertEquals(0L, segundos);

        assertEquals(
                decimal("0.00"),
                calculadora.calcularSubtotalUsd(decimal("10.00"), segundos)
        );

        assertEquals(
                decimal("0.00"),
                calculadora.calcularSubtotalArs(
                        decimal("10.00"), segundos, decimal("1530")
                )
        );
    }

    @Test
    void rechazaFechasAusentesOFinAnteriorAlRetiro() {
        LocalDateTime retiro =
                LocalDateTime.of(2026, 11, 9, 10, 0);

        assertThrows(
                IllegalArgumentException.class,
                () -> calculadora.calcularSegundosCompletos(null, retiro)
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> calculadora.calcularSegundosCompletos(retiro, null)
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> calculadora.calcularSegundosCompletos(
                        retiro, retiro.minusNanos(1)
                )
        );
    }

    @Test
    void rechazaSegundosNegativosEnAmbasMonedas() {
        assertThrows(
                IllegalArgumentException.class,
                () -> calculadora.calcularSubtotalUsd(decimal("10.00"), -1)
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> calculadora.calcularSubtotalArs(
                        decimal("10.00"), -1, decimal("1530")
                )
        );
    }

    @Test
    void rechazaTarifaAusenteOInferiorAlMinimo() {
        for (BigDecimal tarifa : new BigDecimal[]{
                null,
                decimal("0"),
                decimal("-1"),
                decimal("0.009")
        }) {
            assertThrows(
                    IllegalArgumentException.class,
                    () -> calculadora.calcularSubtotalUsd(tarifa, 3600)
            );

            assertThrows(
                    IllegalArgumentException.class,
                    () -> calculadora.calcularSubtotalArs(
                            tarifa, 3600, decimal("1530")
                    )
            );
        }
    }

    @Test
    void rechazaCotizacionAusenteCeroONegativa() {
        for (BigDecimal cotizacion : new BigDecimal[]{
                null,
                decimal("0"),
                decimal("-1")
        }) {
            assertThrows(
                    IllegalArgumentException.class,
                    () -> calculadora.calcularSubtotalArs(
                            decimal("10.00"), 3600, cotizacion
                    )
            );
        }
    }

    private BigDecimal decimal(String valor) {
        return new BigDecimal(valor);
    }
}