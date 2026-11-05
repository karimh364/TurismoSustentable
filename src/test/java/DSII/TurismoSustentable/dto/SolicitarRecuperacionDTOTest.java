package DSII.TurismoSustentable.dto;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;

import static org.junit.jupiter.api.Assertions.*;

class SolicitarRecuperacionDTOTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void prepararValidacion() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void cerrarValidacion() {
        factory.close();
    }

    @Test
    void aceptaCorreoValidoYNormalizaEspaciosYMayusculas() {
        SolicitarRecuperacionDTO datos = new SolicitarRecuperacionDTO();
        datos.setEmail(" ANA@EXAMPLE.COM ");

        assertEquals("ana@example.com", datos.getEmail());
        assertTrue(validator.validate(datos).isEmpty());
    }

    @Test
    void rechazaCorreoAusenteOVacio() {
        for (String email : new String[]{null, "", "   "}) {
            SolicitarRecuperacionDTO datos = new SolicitarRecuperacionDTO();
            datos.setEmail(email);

            assertTrue(validator.validate(datos).stream().anyMatch(error ->
                    error.getMessage().equals("El email no puede estar vacío.")));
        }
    }

    @Test
    void rechazaFormatoIncorrecto() {
        for (String email : new String[]{"hola", "ana@ejemplo", "@mail@gmail.com", "a..b@example.com"}) {
            SolicitarRecuperacionDTO datos = new SolicitarRecuperacionDTO();
            datos.setEmail(email);

            assertFalse(validator.validate(datos).isEmpty(), email);
        }
    }

    @Test
    void acepta254CaracteresYRechaza255SinRecortarElCorreo() {
        String email = "a@" + "b".repeat(63) + "." + "c".repeat(63)
                + "." + "d".repeat(63) + "." + "e".repeat(56) + ".com";
        assertEquals(254, email.length());
        SolicitarRecuperacionDTO datos = new SolicitarRecuperacionDTO();
        datos.setEmail(email);
        assertTrue(validator.validate(datos).isEmpty());

        datos.setEmail("a" + email);
        assertEquals(255, datos.getEmail().length());
        assertTrue(validator.validate(datos).stream().anyMatch(error ->
                error.getMessage().equals("El email no puede superar los 254 caracteres.")));
    }
}
