package DSII.TurismoSustentable.models;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class AlquilerCotizacionTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void prepararValidaciones() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void cerrarValidaciones() {
        if (factory != null) {
            factory.close();
        }
    }

    @Test
    void aceptaCotizacionesPositivasDentroDeLaPrecisionDefinida() {
        for (String valor : new String[]{
                "0.00000001",
                "1530",
                "1530.12345678",
                "999999999999.99999999"
        }) {
            Alquiler alquiler = new Alquiler();
            alquiler.setCotizacionArsPorUsd(new BigDecimal(valor));

            assertTrue(
                    validator.validateProperty(
                            alquiler, "cotizacionArsPorUsd"
                    ).isEmpty(),
                    "Debería aceptar la cotización: " + valor
            );
        }
    }

    @Test
    void rechazaCotizacionInvalidaSinRedondearla() {
        for (String valor : new String[]{
                "0",
                "-1",
                "0.000000001",
                "1000000000000"
        }) {
            Alquiler alquiler = new Alquiler();
            BigDecimal cotizacion = new BigDecimal(valor);

            alquiler.setCotizacionArsPorUsd(cotizacion);

            assertEquals(cotizacion, alquiler.getCotizacionArsPorUsd());

            assertFalse(
                    validator.validateProperty(
                            alquiler, "cotizacionArsPorUsd"
                    ).isEmpty(),
                    "Debería rechazar la cotización: " + valor
            );
        }
    }

    @Test
    void aceptaFuenteInformadaHastaCienCaracteres() {
        for (String fuente : new String[]{
                "FUENTE_PRUEBA",
                "A".repeat(100)
        }) {
            Alquiler alquiler = new Alquiler();
            alquiler.setFuenteCotizacion(fuente);

            assertTrue(
                    validator.validateProperty(
                            alquiler, "fuenteCotizacion"
                    ).isEmpty()
            );
        }
    }

    @Test
    void rechazaFuenteVaciaOConLongitudExcesiva() {
        for (String fuente : new String[]{
                "",
                "   ",
                "\t\n",
                "A".repeat(101)
        }) {
            Alquiler alquiler = new Alquiler();
            alquiler.setFuenteCotizacion(fuente);

            assertFalse(
                    validator.validateProperty(
                            alquiler, "fuenteCotizacion"
                    ).isEmpty(),
                    "Debería rechazar una fuente vacía o demasiado larga."
            );
        }
    }

    @Test
    void conservaValorFuenteYAmbasFechasSinAlterarlos() {
        Alquiler alquiler = new Alquiler();

        BigDecimal cotizacion = new BigDecimal("1530.12345678");
        String fuente = "FUENTE_PRUEBA";

        LocalDateTime aplicacion =
                LocalDateTime.of(2026, 11, 8, 12, 0);

        LocalDateTime actualizacion =
                LocalDateTime.of(2026, 11, 8, 11, 55);

        alquiler.setCotizacionArsPorUsd(cotizacion);
        alquiler.setFuenteCotizacion(fuente);
        alquiler.setFechaHoraAplicacionCotizacion(aplicacion);
        alquiler.setFechaHoraActualizacionFuente(actualizacion);

        assertEquals(cotizacion, alquiler.getCotizacionArsPorUsd());
        assertEquals(fuente, alquiler.getFuenteCotizacion());
        assertEquals(
                aplicacion,
                alquiler.getFechaHoraAplicacionCotizacion()
        );
        assertEquals(
                actualizacion,
                alquiler.getFechaHoraActualizacionFuente()
        );
    }

    @Test
    void constructorAnteriorNoInventaDatosDeCotizacion() {
        Alquiler alquiler = new Alquiler(
                new Usuario(),
                new Bicicleta(),
                new Estacion()
        );

        assertNull(alquiler.getCotizacionArsPorUsd());
        assertNull(alquiler.getFuenteCotizacion());
        assertNull(alquiler.getFechaHoraAplicacionCotizacion());
        assertNull(alquiler.getFechaHoraActualizacionFuente());

        assertTrue(validator.validate(alquiler).isEmpty());
    }
}
