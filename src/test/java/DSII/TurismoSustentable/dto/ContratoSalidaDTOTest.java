package DSII.TurismoSustentable.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import DSII.TurismoSustentable.models.*;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class ContratoSalidaDTOTest {

    private final ObjectMapper mapper =
            new ObjectMapper().findAndRegisterModules();

    @Test
    void usuarioNoExponeColeccionDeAlquileresSinUso() {
        Persona persona = new Persona(
                "Ana",
                "Perez",
                LocalDate.of(1995, 5, 20),
                new Genero("FEMENINO"),
                new TipoDocumento("DNI", "Documento Nacional de Identidad"),
                "12345678",
                new Pais("AR", "Argentina")
        );

        Usuario usuario = new Usuario(
                persona,
                "ana.perez",
                "ana@example.com",
                "hash-de-prueba"
        );

        var json = mapper.valueToTree(new UsuarioDTO(usuario));

        assertFalse(json.has("alquileres"));
        assertEquals("ana.perez", json.get("nickname").asText());
    }

    @Test
    void bicicletaNoExponeColeccionDeAlquileresSinUso() {
        Bicicleta bicicleta = new Bicicleta(
                "QA-DTO-01",
                new TipoBicicleta("MONTANA", new BigDecimal("10.00"), 3),
                new EstadoBicicleta("DISPONIBLE"),
                null
        );

        var json = mapper.valueToTree(new BicicletaDTO(bicicleta));

        assertFalse(json.has("alquileres"));
        assertEquals("QA-DTO-01", json.get("codigoBicicleta").asText());
    }
}
