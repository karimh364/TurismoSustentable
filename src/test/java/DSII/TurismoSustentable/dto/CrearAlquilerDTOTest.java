package DSII.TurismoSustentable.dto;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class CrearAlquilerDTOTest {

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
    void aceptaSolicitudValida() {
        CrearAlquilerDTO dto = datosValidos();

        assertTrue(validator.validate(dto).isEmpty());
        assertEquals(Arrays.asList(1, 2), dto.getBicicletaIds());
        assertEquals(Integer.valueOf(7200), dto.getDuracionEstimadaSegundos());
    }

    @Test
    void rechazaEstacionAusenteONoPositiva() {
        for (Integer id : new Integer[]{null, 0, -1}) {
            CrearAlquilerDTO dto = datosValidos();
            dto.setEstacionRetiroId(id);

            comprobarError(dto, "estacionRetiroId");
        }
    }

    @Test
    void rechazaSeleccionAusenteOVacia() {
        CrearAlquilerDTO dto = datosValidos();

        dto.setBicicletaIds(null);
        comprobarError(dto, "bicicletaIds");

        dto.setBicicletaIds(Collections.emptyList());
        comprobarError(dto, "bicicletaIds");
    }

    @Test
    void rechazaIdentificadoresDeBicicletaInvalidos() {
        for (Integer id : new Integer[]{null, 0, -1}) {
            CrearAlquilerDTO dto = datosValidos();
            dto.setBicicletaIds(Arrays.asList(1, id));

            comprobarError(dto, "bicicletaIds");
        }
    }

    @Test
    void rechazaRepetidosSinEliminarlosSilenciosamente() {
        CrearAlquilerDTO dto = datosValidos();
        dto.setBicicletaIds(Arrays.asList(1, 2, 1));

        comprobarError(dto, "bicicletaIds");
        assertEquals(Arrays.asList(1, 2, 1), dto.getBicicletaIds());
    }

    @Test
    void rechazaDuracionAusenteONoPositiva() {
        for (Integer duracion : new Integer[]{null, 0, -1}) {
            CrearAlquilerDTO dto = datosValidos();
            dto.setDuracionEstimadaSegundos(duracion);

            comprobarError(dto, "duracionEstimadaSegundos");
        }
    }

    @Test
    void recibeJsonConLosTresCamposPermitidos() throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        String json = "{\"estacionRetiroId\":1,"
                + "\"bicicletaIds\":[1,2],"
                + "\"duracionEstimadaSegundos\":7200}";

        CrearAlquilerDTO dto = mapper.readValue(
                json,
                CrearAlquilerDTO.class
        );

        assertEquals(Integer.valueOf(1), dto.getEstacionRetiroId());
        assertEquals(Arrays.asList(1, 2), dto.getBicicletaIds());
        assertEquals(Integer.valueOf(7200), dto.getDuracionEstimadaSegundos());
        assertTrue(validator.validate(dto).isEmpty());
    }

    @Test
    void rechazaJsonConCamposAjenos() {
        ObjectMapper mapper = new ObjectMapper();

        String[] campos = {
                "usuarioId", "rol", "estado", "precio",
                "cotizacion", "fechaVencimiento", "borradoAlquiler"
        };

        for (String campo : campos) {
            String json = "{\"estacionRetiroId\":1,"
                    + "\"bicicletaIds\":[1,2],"
                    + "\"duracionEstimadaSegundos\":7200,"
                    + "\"" + campo + "\":1}";

            assertThrows(
                    JsonMappingException.class,
                    () -> mapper.readValue(json, CrearAlquilerDTO.class),
                    "Debería rechazar el campo: " + campo
            );
        }
    }

    private CrearAlquilerDTO datosValidos() {
        CrearAlquilerDTO dto = new CrearAlquilerDTO();
        dto.setEstacionRetiroId(1);
        dto.setBicicletaIds(Arrays.asList(1, 2));
        dto.setDuracionEstimadaSegundos(7200);
        return dto;
    }

    private void comprobarError(CrearAlquilerDTO dto, String campo) {
        assertTrue(
                validator.validate(dto).stream().anyMatch(
                        error -> error.getPropertyPath()
                                .toString().startsWith(campo)
                ),
                "Debería detectar un error en: " + campo
        );
    }
}
