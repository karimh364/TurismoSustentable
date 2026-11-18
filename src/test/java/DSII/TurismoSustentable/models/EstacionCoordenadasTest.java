package DSII.TurismoSustentable.models;

import DSII.TurismoSustentable.dto.ActualizarEstacionDTO;
import DSII.TurismoSustentable.dto.CrearEstacionDTO;
import DSII.TurismoSustentable.dto.EstacionDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;

import javax.persistence.Column;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class EstacionCoordenadasTest {
    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void preparar() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void cerrar() {
        factory.close();
    }

    @ParameterizedTest
    @CsvSource({
            "latitud, 0, true",
            "longitud, 0, true",
            "latitud, -90, true",
            "latitud, 90, true",
            "longitud, -180, true",
            "longitud, 180, true",
            "latitud, -53.123456, true",
            "longitud, -67.123456, true",
            "latitud, 90.000001, false",
            "latitud, -90.000001, false",
            "longitud, 180.000001, false",
            "longitud, -180.000001, false",
            "latitud, -53.1234567, false",
            "longitud, -67.1234567, false",
            "latitud, 0.0000001, false",
            "longitud, 1E-7, false"
    })
    void entidadYEntradasCompartenRangosYPrecision(
            String campo, String texto, boolean valido
    ) {
        BigDecimal numero = new BigDecimal(texto);
        for (Object destino : new Object[]{
                new Estacion(), new CrearEstacionDTO(), new ActualizarEstacionDTO()
        }) {
            ReflectionTestUtils.setField(destino, campo, numero);
            assertEquals(valido, validator.validateProperty(destino, campo).isEmpty(),
                    destino.getClass().getSimpleName() + ": " + campo + "=" + texto);
            // Validar no debe redondear ni sustituir el dato recibido.
            assertSame(numero, ReflectionTestUtils.getField(destino, campo));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"latitud", "longitud"})
    void noConfundeCoordenadaAusenteConCero(String campo) {
        for (Object destino : new Object[]{
                new Estacion(), new CrearEstacionDTO(), new ActualizarEstacionDTO()
        }) {
            assertFalse(validator.validateProperty(destino, campo).isEmpty());
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"latitud", "longitud"})
    void mapeaDecimalNueveSeisObligatorio(String campo) throws Exception {
        var atributo = Estacion.class.getDeclaredField(campo);
        assertEquals(BigDecimal.class, atributo.getType());
        Column columna = atributo.getAnnotation(Column.class);
        assertEquals(campo, columna.name());
        assertEquals(9, columna.precision());
        assertEquals(6, columna.scale());
        assertFalse(columna.nullable());
    }

    @Test
    void dtoDeSalidaConservaDecimalesYLosSerializaComoNumeros() throws Exception {
        BigDecimal latitud = new BigDecimal("-53.123456");
        BigDecimal longitud = BigDecimal.ZERO;
        Estacion estacion = new Estacion("QA", "Calle QA", 2, latitud, longitud);
        EstacionDTO dto = new EstacionDTO(estacion);
        assertSame(latitud, dto.getLatitud());
        assertSame(longitud, dto.getLongitud());
        var mapper = new ObjectMapper();
        var json = mapper.readTree(mapper.writeValueAsString(dto));
        assertTrue(json.get("latitud").isNumber());
        assertTrue(json.get("longitud").isNumber());
        assertEquals(0, latitud.compareTo(json.get("latitud").decimalValue()));
        assertEquals(0, longitud.compareTo(json.get("longitud").decimalValue()));
    }
}
