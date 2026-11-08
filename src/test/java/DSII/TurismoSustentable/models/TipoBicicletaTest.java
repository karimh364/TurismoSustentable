package DSII.TurismoSustentable.models;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class TipoBicicletaTest {

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
    void aceptaDatosValidosYNormalizaCodigo() {
        TipoBicicleta tipo = new TipoBicicleta(
                " montana ",
                new BigDecimal("9.50"),
                3
        );

        assertEquals("MONTANA", tipo.getCodigo());
        assertTrue(validator.validate(tipo).isEmpty());
    }

    @Test
    void rechazaCodigosInvalidos() {
        String[] codigos = {
                null, "", "   ", "X".repeat(21)
        };

        for (String codigo : codigos) {
            TipoBicicleta tipo = datosValidos();
            tipo.setCodigo(codigo);

            comprobarError(tipo, "codigo");
        }
    }

    @Test
    void aceptaPreciosDentroDeLosLimites() {
        String[] precios = {
                "0.01", "9.50", "99999999.99"
        };

        for (String precio : precios) {
            TipoBicicleta tipo = datosValidos();
            tipo.setPrecioPorHoraUsd(new BigDecimal(precio));

            assertTrue(
                    validator.validate(tipo).isEmpty(),
                    "Debería aceptar: " + precio
            );
        }
    }

    @Test
    void rechazaPreciosInvalidos() {
        String[] precios = {
                null, "0", "-1", "9.999", "100000000.00"
        };

        for (String precio : precios) {
            TipoBicicleta tipo = datosValidos();
            tipo.setPrecioPorHoraUsd(
                    precio == null ? null : new BigDecimal(precio)
            );

            comprobarError(tipo, "precioPorHoraUsd");
        }
    }

    @Test
    void noRedondeaSilenciosamenteElPrecio() {
        TipoBicicleta tipo = datosValidos();
        BigDecimal precio = new BigDecimal("9.999");

        tipo.setPrecioPorHoraUsd(precio);

        assertEquals(precio, tipo.getPrecioPorHoraUsd());
        comprobarError(tipo, "precioPorHoraUsd");
    }

    @Test
    void rechazaIntervalosInvalidos() {
        Integer[] intervalos = {null, 0, -1};

        for (Integer intervalo : intervalos) {
            TipoBicicleta tipo = datosValidos();
            tipo.setIntervaloPreventivoMeses(intervalo);

            comprobarError(tipo, "intervaloPreventivoMeses");
        }
    }

    private TipoBicicleta datosValidos() {
        return new TipoBicicleta(
                "MONTANA",
                new BigDecimal("9.50"),
                3
        );
    }

    private void comprobarError(TipoBicicleta tipo, String campo) {
        assertTrue(
                validator.validate(tipo).stream().anyMatch(
                        error -> error.getPropertyPath()
                                .toString().equals(campo)
                ),
                "Debería detectar un error en: " + campo
        );
    }
}