package DSII.TurismoSustentable.validation;

import DSII.TurismoSustentable.dto.ActualizarUsuarioDTO;
import DSII.TurismoSustentable.dto.RegistroUsuarioDTO;
import DSII.TurismoSustentable.models.*;
import org.junit.jupiter.api.Test;

import javax.validation.Validation;
import javax.validation.ValidatorFactory;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class NormalizacionNombreTest {

    @Test
    void unificaEspaciosUnicodeYTildesSinCambiarLaCapitalizacion() {
        assertEquals("José María", NormalizacionNombre.normalizar(" \u00a0Jose\u0301\t María  "));
        assertEquals("de la Cruz", NormalizacionNombre.normalizar(" de  la\nCruz "));
        assertEquals("Muñoz", NormalizacionNombre.normalizar("Muñoz"));
        assertEquals("", NormalizacionNombre.normalizar("\u00a0\u2003\t"));
        assertNull(NormalizacionNombre.normalizar(null));
    }

    @Test
    void reconoceApostrofesYGuionesTipograficos() {
        assertEquals("O'Connor", NormalizacionNombre.normalizar("O\u2019Connor"));
        assertEquals("D'Angelo", NormalizacionNombre.normalizar("D\u02BCAngelo"));
        assertEquals("Jean-Luc", NormalizacionNombre.normalizar("Jean\u2011Luc"));
        assertTrue(NormalizacionNombre.normalizar("O\u2019Connor").matches(NormalizacionNombre.PATRON));
    }

    @Test
    void aplicaLaMismaReglaEnRegistroEdicionYEntidad() {
        RegistroUsuarioDTO registro = new RegistroUsuarioDTO();
        ActualizarUsuarioDTO actualizacion = new ActualizarUsuarioDTO();
        Persona persona = new Persona(
                " Jose\u0301  María ", " O\u2019Connor ", LocalDate.of(1995, 5, 20),
                new Genero("OTRO"), new TipoDocumento("DNI", "DNI"),
                "00.123.456", new Pais("AR", "Argentina")
        );
        registro.setNombre(" Jose\u0301  María ");
        registro.setApellido(" O\u2019Connor ");
        actualizacion.setNombre(" Jose\u0301  María ");
        actualizacion.setApellido(" O\u2019Connor ");

        assertEquals("José María", registro.getNombre());
        assertEquals(registro.getNombre(), actualizacion.getNombre());
        assertEquals(registro.getNombre(), persona.getNombre());
        assertEquals("O'Connor", registro.getApellido());
        assertEquals(registro.getApellido(), actualizacion.getApellido());
        assertEquals(registro.getApellido(), persona.getApellido());
        assertEquals("00123456", persona.getNumeroDocumento());

        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            for (Object valor : new Object[]{registro, actualizacion, persona}) {
                assertTrue(factory.getValidator().validateProperty(valor, "nombre").isEmpty());
                assertTrue(factory.getValidator().validateProperty(valor, "apellido").isEmpty());
            }
        }
    }

    @Test
    void conservaElMaximoYRechazaNumerosOSimbolosAjenosAlNombre() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            RegistroUsuarioDTO registro = new RegistroUsuarioDTO();
            registro.setNombre("A".repeat(50));
            assertTrue(factory.getValidator().validateProperty(registro, "nombre").isEmpty());
            registro.setNombre("A".repeat(51));
            assertFalse(factory.getValidator().validateProperty(registro, "nombre").isEmpty());
            for (String valor : new String[]{"Ana12", "Ana@", "-Ana", "Ana-", "O''Connor"}) {
                registro.setNombre(valor);
                assertFalse(factory.getValidator().validateProperty(registro, "nombre").isEmpty());
            }
        }
    }
}
