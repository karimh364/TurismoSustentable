package DSII.TurismoSustentable.models;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class AlquilerImportesTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    private static final String[] CAMPOS = {
            "costoEstimadoUsd",
            "costoEstimadoArs",
            "costoFinalUsd",
            "costoFinalArs"
    };

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
    void aceptaImportesNoNegativosDentroDeLaPrecisionDefinida() {
        for (String valor : new String[]{
                "0.00",
                "1530.50",
                "9999999999999999999.99"
        }) {
            BigDecimal importe = new BigDecimal(valor);
            Alquiler alquiler = conImportes(importe);

            comprobarValoresConservados(alquiler, importe);

            for (String campo : CAMPOS) {
                assertTrue(
                        validator.validateProperty(alquiler, campo).isEmpty(),
                        "Debería aceptar " + valor + " en " + campo
                );
            }
        }
    }

    @Test
    void rechazaImportesInvalidosSinRedondearlos() {
        for (String valor : new String[]{
                "-0.01",
                "1.001",
                "10000000000000000000.00"
        }) {
            BigDecimal importe = new BigDecimal(valor);
            Alquiler alquiler = conImportes(importe);

            comprobarValoresConservados(alquiler, importe);

            for (String campo : CAMPOS) {
                assertFalse(
                        validator.validateProperty(alquiler, campo).isEmpty(),
                        "Debería rechazar " + valor + " en " + campo
                );
            }
        }
    }

    @Test
    void mantieneImportesPendientesSinInventarCerosEnLaTransicion() {
        Alquiler alquiler = new Alquiler(
                new Usuario(),
                new Bicicleta(),
                new Estacion()
        );

        assertNull(alquiler.getCostoEstimadoUsd());
        assertNull(alquiler.getCostoEstimadoArs());
        assertNull(alquiler.getCostoFinalUsd());
        assertNull(alquiler.getCostoFinalArs());

        for (String campo : CAMPOS) {
            assertTrue(
                    validator.validateProperty(alquiler, campo).isEmpty(),
                    "Durante la transición se permite null en " + campo
            );
        }
    }

    private Alquiler conImportes(BigDecimal importe) {
        Alquiler alquiler = new Alquiler();

        alquiler.setCostoEstimadoUsd(importe);
        alquiler.setCostoEstimadoArs(importe);
        alquiler.setCostoFinalUsd(importe);
        alquiler.setCostoFinalArs(importe);

        return alquiler;
    }

    private void comprobarValoresConservados(
            Alquiler alquiler,
            BigDecimal importe
    ) {
        assertEquals(importe, alquiler.getCostoEstimadoUsd());
        assertEquals(importe, alquiler.getCostoEstimadoArs());
        assertEquals(importe, alquiler.getCostoFinalUsd());
        assertEquals(importe, alquiler.getCostoFinalArs());
    }
}