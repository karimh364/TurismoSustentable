package DSII.TurismoSustentable.dto;

import org.junit.jupiter.api.Test;
import javax.validation.Validation;
import java.util.Arrays;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class ActualizarRolesUsuarioDTOTest {
    @Test void validaContratoYNormalizaMotivoSinCambiarCodigos() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var v = factory.getValidator();
            var datos = new ActualizarRolesUsuarioDTO();
            datos.setVersion(0); datos.setRoles(List.of("ADMIN", "CLIENTE")); datos.setMotivo(" Prueba QA ");
            assertEquals("Prueba QA", datos.getMotivo()); assertTrue(v.validate(datos).isEmpty());
            for (List<String> roles : List.of(List.<String>of(), List.of("ROOT"),
                    List.of("CLIENTE", "CLIENTE"), Arrays.asList("CLIENTE", null))) {
                datos.setRoles(roles); assertFalse(v.validate(datos).isEmpty());
            }
            datos.setRoles(List.of("CLIENTE")); datos.setVersion(null); assertFalse(v.validate(datos).isEmpty());
            datos.setVersion(-1); assertFalse(v.validate(datos).isEmpty());
            datos.setVersion(0); datos.setMotivo(" "); assertFalse(v.validate(datos).isEmpty());
            datos.setMotivo("x".repeat(2001)); assertFalse(v.validate(datos).isEmpty());
        }
    }
}
